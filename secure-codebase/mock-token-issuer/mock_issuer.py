#!/usr/bin/env python3
# Tool: Claude (Anthropic), via Claude Code
# Prompt Summary: "Build a local mock JWT issuer for CS763 Assignment 3 with key
#   generation, signed tokens, a JWKS endpoint, and labeled negative tokens."
# AI Contribution: Initial draft (~90%)
# Human Modifications:
# - Reviewed the design and the negative test set against the course PDF
# - Chose the README wording and the course-fit notes
# - Accepted the fixes from a second AI review (input limits, kid lookup, clock skew)
# Verification:
# - pytest suite (tests/test_mock_issuer.py)
# - PyJWT check (tests/verify_with_pyjwt.py)
# - Docker build and healthcheck
# Confidence: Medium
"""
CS763 Mock Token Issuer

A small key-generation and JWT issuing tool for Project Assignment 3.
It stands in for a real identity provider so you can focus on validating
tokens in your service, not on building login flows.

FOR LOCAL DEVELOPMENT AND TESTING ONLY. Do not use in production.

Commands
  keygen    Generate a signing key pair (RS256 or ES256) and a JWKS file
  issue     Issue a signed JWT for a user
  negative  Print a set of labeled bad tokens for your tests
  decode    Print a token's header and payload (no verification)
  serve     Run an HTTP server with a JWKS endpoint and a token endpoint

Only dependency: the "cryptography" package (pip install cryptography).
"""

import argparse
import base64
import binascii
import functools
import hashlib
import hmac
import json
import os
import socketserver
import sys
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

try:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import ec, padding, rsa
    from cryptography.hazmat.primitives.asymmetric.utils import decode_dss_signature
except ImportError:  # pragma: no cover
    sys.exit("Missing dependency. Run: pip install cryptography")

DEFAULT_ISSUER = os.environ.get("ISSUER", "http://localhost:9000")
DEFAULT_AUDIENCE = os.environ.get("AUDIENCE", "cs763-api")
DEFAULT_KEYS_DIR = os.environ.get("KEYS_DIR", "keys")
DEFAULT_TTL = int(os.environ.get("TOKEN_TTL", "3600"))
DEFAULT_ALG = os.environ.get("ALG", "RS256")
SUPPORTED_ALGS = ("RS256", "ES256")


# ---------------------------------------------------------------- helpers

def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def b64url_json(obj) -> str:
    return b64url(json.dumps(obj, separators=(",", ":")).encode("utf-8"))


def int_to_b64url(n: int, length: int = None) -> str:
    if length is None:
        length = (n.bit_length() + 7) // 8
    return b64url(n.to_bytes(length, "big"))


def b64url_decode(s: str) -> bytes:
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


# ---------------------------------------------------------------- keys

def public_jwk(public_key, alg: str) -> dict:
    """Build a public JWK. kid is the RFC 7638 thumbprint."""
    if alg == "RS256":
        nums = public_key.public_numbers()
        core = {"e": int_to_b64url(nums.e), "kty": "RSA", "n": int_to_b64url(nums.n)}
    else:
        nums = public_key.public_numbers()
        core = {"crv": "P-256", "kty": "EC",
                "x": int_to_b64url(nums.x, 32), "y": int_to_b64url(nums.y, 32)}
    thumb = hashlib.sha256(json.dumps(core, separators=(",", ":"), sort_keys=True).encode()).digest()
    jwk = dict(core)
    jwk.update({"kid": b64url(thumb), "use": "sig", "alg": alg})
    return jwk


def generate_private_key(alg: str):
    if alg == "RS256":
        return rsa.generate_private_key(public_exponent=65537, key_size=2048)
    if alg == "ES256":
        return ec.generate_private_key(ec.SECP256R1())
    raise ValueError(f"Unsupported alg {alg}. Use one of {SUPPORTED_ALGS}")


def alg_for_key(private_key) -> str:
    return "RS256" if isinstance(private_key, rsa.RSAPrivateKey) else "ES256"


def keygen(keys_dir: str, alg: str, force: bool = False) -> dict:
    os.makedirs(keys_dir, exist_ok=True)
    priv_path = os.path.join(keys_dir, "private.pem")
    if os.path.exists(priv_path) and not force:
        raise FileExistsError(f"{priv_path} already exists. Use --force to replace it.")
    key = generate_private_key(alg)
    pem = key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                            serialization.NoEncryption())
    pub_pem = key.public_key().public_bytes(serialization.Encoding.PEM,
                                            serialization.PublicFormat.SubjectPublicKeyInfo)
    with open(priv_path, "wb") as f:
        f.write(pem)
    try:
        os.chmod(priv_path, 0o600)
    except OSError:
        pass
    with open(os.path.join(keys_dir, "public.pem"), "wb") as f:
        f.write(pub_pem)
    jwks = {"keys": [public_jwk(key.public_key(), alg)]}
    with open(os.path.join(keys_dir, "jwks.json"), "w") as f:
        json.dump(jwks, f, indent=2)
    return jwks


def load_keys(keys_dir: str, auto_alg: str = None):
    """Load the private key. If auto_alg is set and no key exists, generate one."""
    priv_path = os.path.join(keys_dir, "private.pem")
    if not os.path.exists(priv_path):
        if auto_alg:
            keygen(keys_dir, auto_alg)
            print(f"No key found in {keys_dir}. Generated a new {auto_alg} key pair.",
                  file=sys.stderr)
        else:
            sys.exit(f"No key found at {priv_path}. Run: python mock_issuer.py keygen")
    with open(priv_path, "rb") as f:
        key = serialization.load_pem_private_key(f.read(), password=None)
    alg = alg_for_key(key)
    return key, alg, public_jwk(key.public_key(), alg)


# ---------------------------------------------------------------- signing

def sign(signing_input: bytes, key, alg: str) -> bytes:
    if alg == "RS256":
        return key.sign(signing_input, padding.PKCS1v15(), hashes.SHA256())
    if alg == "ES256":
        der = key.sign(signing_input, ec.ECDSA(hashes.SHA256()))
        r, s = decode_dss_signature(der)
        return r.to_bytes(32, "big") + s.to_bytes(32, "big")  # JWS uses raw R||S
    raise ValueError(alg)


def encode_jwt(header: dict, payload: dict, key, alg: str) -> str:
    signing_input = f"{b64url_json(header)}.{b64url_json(payload)}"
    sig = sign(signing_input.encode("ascii"), key, alg)
    return f"{signing_input}.{b64url(sig)}"


def build_claims(sub, roles=None, iss=DEFAULT_ISSUER, aud=DEFAULT_AUDIENCE,
                 ttl=DEFAULT_TTL, extra=None, now=None) -> dict:
    now = int(now if now is not None else time.time())
    claims = {
        "iss": iss,
        "sub": sub,
        "aud": aud,
        "iat": now,
        "nbf": now,
        "exp": now + int(ttl),
        "jti": str(uuid.uuid4()),
    }
    if roles:
        claims["roles"] = roles
        claims["scope"] = " ".join(roles)
    if extra:
        claims.update(extra)
    return claims


def issue_token(key, alg, jwk, **kwargs) -> str:
    header = {"alg": alg, "typ": "JWT", "kid": jwk["kid"]}
    return encode_jwt(header, build_claims(**kwargs), key, alg)


# ---------------------------------------------------------------- negative tests

@functools.lru_cache(maxsize=None)
def _attacker_key(alg: str):
    """A key the issuer does not trust. Made once per process, since RSA keygen is slow."""
    return generate_private_key(alg)


def negative_tokens(key, alg, jwk, sub="alice", roles=None, iss=DEFAULT_ISSUER,
                    aud=DEFAULT_AUDIENCE, other_sub="bob") -> dict:
    """Return labeled tokens. Your service should accept only the ones marked 'accept'."""
    roles = roles or ["user"]
    now = int(time.time())
    hdr = {"alg": alg, "typ": "JWT", "kid": jwk["kid"]}

    def good(**over):
        claims = build_claims(sub, roles, iss, aud, DEFAULT_TTL, now=now)
        claims.update(over)
        return claims

    out = {}

    def add(name, token, expect, why):
        out[name] = {"expect": expect, "why": why, "token": token}

    add("valid", encode_jwt(hdr, good(), key, alg), "accept",
        f"Correctly signed {alg} token for {sub}")
    add("valid_other_user",
        encode_jwt(hdr, good(sub=other_sub), key, alg), "accept",
        f"Valid token for {other_sub}. Use it to test row-level authorization: "
        f"{other_sub} must not read or change {sub}'s records (expect 403 or 404).")

    add("expired", encode_jwt(hdr, good(iat=now - 7200, nbf=now - 7200, exp=now - 3600), key, alg),
        "reject", "exp is one hour in the past")
    add("not_yet_valid", encode_jwt(hdr, good(nbf=now + 3600), key, alg),
        "reject", "nbf is one hour in the future")
    add("missing_exp", encode_jwt(hdr, {k: v for k, v in good().items() if k != "exp"}, key, alg),
        "reject", "No exp claim; tokens without an expiry must be rejected")
    add("wrong_issuer", encode_jwt(hdr, good(iss="https://evil.example.com"), key, alg),
        "reject", "iss does not match the expected issuer")
    add("wrong_audience", encode_jwt(hdr, good(aud="some-other-api"), key, alg),
        "reject", "aud does not match the expected audience")
    add("valid_aud_array", encode_jwt(hdr, good(aud=[aud, "some-other-api"]), key, alg),
        "accept", "aud is an array that includes the expected audience (RFC 7519 allows this)")
    add("expired_2min", encode_jwt(hdr, good(iat=now - 300, nbf=now - 300, exp=now - 120), key, alg),
        "reject", "exp was 2 minutes ago. Past the 60 s clock skew that Spring and many libraries allow by default.")

    # alg=none: unsigned token
    none_hdr = {"alg": "none", "typ": "JWT", "kid": jwk["kid"]}
    add("alg_none", f"{b64url_json(none_hdr)}.{b64url_json(good())}.",
        "reject", "alg 'none' with no signature; must be blocked by the algorithm allowlist")

    # HS256 key confusion: HMAC signed with the public key bytes as the secret
    pub_pem = key.public_key().public_bytes(serialization.Encoding.PEM,
                                            serialization.PublicFormat.SubjectPublicKeyInfo)
    hs_hdr = {"alg": "HS256", "typ": "JWT", "kid": jwk["kid"]}
    hs_input = f"{b64url_json(hs_hdr)}.{b64url_json(good())}"
    hs_sig = hmac.new(pub_pem, hs_input.encode("ascii"), hashlib.sha256).digest()
    add("hs256_key_confusion", f"{hs_input}.{b64url(hs_sig)}", "reject",
        "HS256 signed with the public key as the HMAC secret (algorithm confusion attack)")

    # Signed by a different key but claims our kid
    attacker = _attacker_key(alg)
    add("bad_signature", encode_jwt(hdr, good(), attacker, alg), "reject",
        "Signed by an attacker's key while claiming the issuer's kid")

    # Valid signature, then payload swapped (privilege escalation attempt)
    valid = encode_jwt(hdr, good(), key, alg)
    h, _, s = valid.split(".")
    tampered_payload = b64url_json(good(sub=other_sub, roles=["admin"], scope="admin"))
    add("tampered_payload", f"{h}.{tampered_payload}.{s}", "reject",
        "Payload edited after signing (sub and roles changed); signature no longer matches")

    unk_hdr = dict(hdr, kid="unknown-key-id")
    # Signed with the issuer's real key, so only a kid lookup can reject it.
    # A verifier that ignores kid and uses its one known key would accept this token.
    add("unknown_kid", encode_jwt(unk_hdr, good(), key, alg), "reject",
        "kid not present in the JWKS. Signed with the real key, so only a kid lookup catches it.")

    # Header claims an algorithm that is not on the allowlist
    ps_hdr = dict(hdr, alg="PS256")
    add("alg_not_allowed", encode_jwt(ps_hdr, good(), key, alg), "reject",
        "Header says PS256, which is not on the allowlist. The signature is also wrong for PS256.")

    # Header asks the verifier to fetch keys from an attacker's URL
    jku_hdr = dict(hdr, jku="https://evil.example.com/jwks.json")
    add("jku_injection", encode_jwt(jku_hdr, good(), attacker, alg), "reject",
        "jku header points at an attacker's key set. Verifiers must use their own trusted JWKS URL only.")

    add("malformed", "not.a.jwt", "reject", "Not a valid JWT")
    return out


# ---------------------------------------------------------------- server

MAX_BODY_BYTES = 64 * 1024


class BadRequest(Exception):
    def __init__(self, message, status=400):
        super().__init__(message)
        self.status = status


class Handler(BaseHTTPRequestHandler):
    key = alg = jwk = None
    issuer = DEFAULT_ISSUER
    audience = DEFAULT_AUDIENCE
    issue_secret = None  # when set, /token and /negative need this in X-Issuer-Secret
    timeout = 10  # seconds. A client that sends a short body and waits gets disconnected.

    def _send(self, code, body, ctype="application/json"):
        data = body if isinstance(body, bytes) else json.dumps(body, indent=2).encode()
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def _params(self):
        url = urlparse(self.path)
        params = {k: v[-1] for k, v in parse_qs(url.query).items()}
        if self.command == "POST":
            try:
                length = int(self.headers.get("Content-Length") or 0)
            except ValueError:
                raise BadRequest("Content-Length must be a number")
            if length < 0:
                raise BadRequest("Content-Length must not be negative")
            if length > MAX_BODY_BYTES:
                raise BadRequest(f"body is larger than {MAX_BODY_BYTES} bytes", status=413)
            raw = self.rfile.read(length).decode("utf-8", "replace") if length else ""
            ctype = self.headers.get("Content-Type", "")
            if raw and "json" in ctype:
                try:
                    body = json.loads(raw)
                except json.JSONDecodeError:
                    raise BadRequest("body is not valid JSON")
                if not isinstance(body, dict):
                    raise BadRequest("JSON body must be an object")
                for k, v in body.items():
                    if isinstance(v, list):
                        if not all(isinstance(x, (str, int)) for x in v):
                            raise BadRequest(f"{k} must be a list of strings or numbers")
                        params[k] = ",".join(map(str, v))
                    elif isinstance(v, (str, int)) and not isinstance(v, bool):
                        params[k] = str(v)
                    else:
                        raise BadRequest(f"{k} must be a string or a number")
            elif raw:
                params.update({k: v[-1] for k, v in parse_qs(raw).items()})
        return url.path, params

    def _route(self):
        try:
            path, p = self._params()
        except BadRequest as e:
            return self._send(e.status, {"error": "invalid_request", "error_description": str(e)})
        if path == "/token" and self.command != "POST":
            return self._send(405, {"error": "method_not_allowed", "error_description": "use POST /token"})
        if path in ("/token", "/negative") and self.issue_secret:
            given = self.headers.get("X-Issuer-Secret", "")
            if not hmac.compare_digest(given.encode(), self.issue_secret.encode()):
                return self._send(401, {"error": "unauthorized",
                                        "error_description": "missing or wrong X-Issuer-Secret header"})
        if path in ("/", "/health"):
            return self._send(200, {"status": "ok", "issuer": self.issuer, "audience": self.audience,
                                    "alg": self.alg, "kid": self.jwk["kid"]})
        if path in ("/.well-known/jwks.json", "/jwks"):
            return self._send(200, {"keys": [self.jwk]})
        if path == "/.well-known/openid-configuration":
            base = self.issuer.rstrip("/")
            return self._send(200, {
                "issuer": self.issuer,
                "jwks_uri": f"{base}/.well-known/jwks.json",
                "token_endpoint": f"{base}/token",
                "id_token_signing_alg_values_supported": [self.alg],
                "response_types_supported": ["token"],
                "subject_types_supported": ["public"],
            })
        if path == "/token":
            sub = p.get("sub") or p.get("username")
            if not sub:
                return self._send(400, {"error": "invalid_request", "error_description": "sub is required"})
            roles = [r for r in p.get("roles", "user").split(",") if r]
            try:
                ttl = int(p.get("ttl", DEFAULT_TTL))
            except ValueError:
                return self._send(400, {"error": "invalid_request", "error_description": "ttl must be an integer"})
            if ttl <= 0:
                return self._send(400, {"error": "invalid_request", "error_description": "ttl must be greater than 0"})
            token = issue_token(self.key, self.alg, self.jwk, sub=sub, roles=roles,
                                iss=self.issuer, aud=p.get("aud", self.audience), ttl=ttl)
            return self._send(200, {"access_token": token, "token_type": "Bearer", "expires_in": ttl})
        if path == "/negative":
            return self._send(200, negative_tokens(self.key, self.alg, self.jwk,
                                                   sub=p.get("sub", "alice"),
                                                   other_sub=p.get("other_sub", "bob"),
                                                   iss=self.issuer, aud=self.audience))
        return self._send(404, {"error": "not_found"})

    do_GET = _route
    do_POST = _route

    def log_message(self, fmt, *args):
        sys.stderr.write("[mock-issuer] " + (fmt % args) + "\n")


class IssuerServer(ThreadingHTTPServer):
    """Same as ThreadingHTTPServer, but skips a reverse DNS lookup on bind.

    HTTPServer.server_bind calls socket.getfqdn(), which can stall for 30+ seconds
    on some machines before the server starts. The server name is not used here.
    """

    def server_bind(self):
        socketserver.TCPServer.server_bind(self)
        self.server_name, self.server_port = self.server_address[:2]


def serve(host, port, keys_dir, alg, issuer, audience, issue_secret=None):
    key, alg, jwk = load_keys(keys_dir, auto_alg=alg)
    Handler.key, Handler.alg, Handler.jwk = key, alg, jwk
    Handler.issuer, Handler.audience = issuer, audience
    Handler.issue_secret = issue_secret or None
    srv = IssuerServer((host, port), Handler)
    print(f"Mock token issuer on http://{host}:{port}  (alg={alg}, kid={jwk['kid']})", flush=True)
    print(f"  issuer:   {issuer}\n  audience: {audience}", flush=True)
    print("  GET  /.well-known/jwks.json\n  GET  /.well-known/openid-configuration", flush=True)
    print("  POST /token  (sub=alice&roles=user)\n  GET  /negative?sub=alice", flush=True)
    if issue_secret:
        print("  /token and /negative need the X-Issuer-Secret header", flush=True)
    print("FOR LOCAL DEVELOPMENT AND TESTING ONLY", flush=True)
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass


# ---------------------------------------------------------------- CLI

RESERVED_CLAIMS = {"iss", "sub", "aud", "iat", "nbf", "exp", "jti", "roles", "scope"}


def parse_extra(pairs):
    extra = {}
    for item in pairs or []:
        if "=" not in item:
            sys.exit(f"--claim expects key=value, got {item!r}")
        k, v = item.split("=", 1)
        if k in RESERVED_CLAIMS:
            sys.exit(f"--claim {k} is reserved. Use --sub, --roles, --iss, --aud, or --ttl instead.")
        try:
            extra[k] = json.loads(v)
        except json.JSONDecodeError:
            extra[k] = v
    return extra


def main(argv=None):
    ap = argparse.ArgumentParser(description="CS763 mock token issuer (local testing only)")
    ap.add_argument("--keys-dir", default=DEFAULT_KEYS_DIR, help="where keys live (default: keys)")
    sub = ap.add_subparsers(dest="cmd", required=True)

    k = sub.add_parser("keygen", help="generate a signing key pair and jwks.json")
    k.add_argument("--alg", choices=SUPPORTED_ALGS, default=DEFAULT_ALG)
    k.add_argument("--force", action="store_true", help="overwrite existing keys")

    i = sub.add_parser("issue", help="issue a signed JWT")
    i.add_argument("--sub", required=True, help="user id, for example alice")
    i.add_argument("--roles", default="user", help="comma separated, for example user,admin")
    i.add_argument("--iss", default=DEFAULT_ISSUER)
    i.add_argument("--aud", default=DEFAULT_AUDIENCE)
    i.add_argument("--ttl", type=int, default=DEFAULT_TTL, help="seconds until exp")
    i.add_argument("--claim", action="append", help="extra claim key=value (repeatable)")

    n = sub.add_parser("negative", help="print labeled good and bad tokens as JSON")
    n.add_argument("--sub", default="alice")
    n.add_argument("--other-sub", default="bob")
    n.add_argument("--iss", default=DEFAULT_ISSUER)
    n.add_argument("--aud", default=DEFAULT_AUDIENCE)
    n.add_argument("--out", help="write JSON to this file instead of stdout")

    d = sub.add_parser("decode", help="show header and payload without verifying")
    d.add_argument("token")

    s = sub.add_parser("serve", help="run JWKS and token endpoints")
    s.add_argument("--host", default=os.environ.get("HOST", "127.0.0.1"),
                   help="default 127.0.0.1 (this machine only). Use 0.0.0.0 inside Docker.")
    s.add_argument("--port", type=int, default=int(os.environ.get("PORT", "9000")))
    s.add_argument("--alg", choices=SUPPORTED_ALGS, default=DEFAULT_ALG,
                   help="used only if keys do not exist yet")
    s.add_argument("--iss", default=DEFAULT_ISSUER)
    s.add_argument("--aud", default=DEFAULT_AUDIENCE)
    s.add_argument("--issue-secret", default=os.environ.get("ISSUE_SECRET"),
                   help="if set, /token and /negative require this in the X-Issuer-Secret header")

    a = ap.parse_args(argv)

    if a.cmd == "keygen":
        try:
            jwks = keygen(a.keys_dir, a.alg, a.force)
        except FileExistsError as e:
            sys.exit(str(e))
        print(f"Wrote {a.keys_dir}/private.pem, public.pem, jwks.json (alg={a.alg}, kid={jwks['keys'][0]['kid']})")
        print("Do NOT commit private.pem. Add the keys directory to .gitignore.")
    elif a.cmd == "issue":
        key, alg, jwk = load_keys(a.keys_dir)
        roles = [r for r in a.roles.split(",") if r]
        print(issue_token(key, alg, jwk, sub=a.sub, roles=roles, iss=a.iss, aud=a.aud,
                          ttl=a.ttl, extra=parse_extra(a.claim)))
    elif a.cmd == "negative":
        key, alg, jwk = load_keys(a.keys_dir)
        data = json.dumps(negative_tokens(key, alg, jwk, sub=a.sub, other_sub=a.other_sub,
                                          iss=a.iss, aud=a.aud), indent=2)
        if a.out:
            with open(a.out, "w") as f:
                f.write(data + "\n")
            print(f"Wrote {a.out}")
        else:
            print(data)
    elif a.cmd == "decode":
        parts = a.token.split(".")
        if len(parts) != 3:
            sys.exit("Not a JWT (expected 3 parts)")
        try:
            header = json.loads(b64url_decode(parts[0]))
            payload = json.loads(b64url_decode(parts[1]))
        except (binascii.Error, ValueError, UnicodeDecodeError):
            sys.exit("Not a JWT (header or payload is not valid base64url JSON)")
        print(json.dumps({"header": header, "payload": payload}, indent=2))
    elif a.cmd == "serve":
        serve(a.host, a.port, a.keys_dir, a.alg, a.iss, a.aud, a.issue_secret)


if __name__ == "__main__":
    main()
