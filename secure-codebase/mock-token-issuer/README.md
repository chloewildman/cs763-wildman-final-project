# CS763 Mock Token Issuer

A key-generation and mock token issuer tool for Project Assignment 3. It stands in for a real identity provider, so you can spend your time on validating tokens and authorizing requests in your service rather than building login flows.

**For local development and testing only.** Do not deploy it or reuse its keys anywhere else.

**AI generated.** This tool was generated with an AI coding assistant (Claude Code). Read the code and run the tests before you rely on it for graded work. It is a test aid, not a reference implementation of JWT validation.

## Start here

You need Python 3.9 or newer. These five steps take about five minutes.

**1. Install the one library the tool needs.**

```bash
pip install cryptography
```

**2. Create a signing key (you only do this once).**

```bash
python mock_issuer.py keygen
```

This writes three files to a `keys/` folder: `private.pem` (secret), `public.pem`, and `jwks.json` (the public part your service reads). Never commit `private.pem`. The `keys/` folder is already in `.gitignore`.

**3. Get a token for a test user.**

```bash
python mock_issuer.py issue --sub alice
```

The tool prints one long string. That's a signed token for "alice".

**4. Look inside a token (optional).**

```bash
python mock_issuer.py decode <paste-your-token-here>
```

This shows the header and claims. It doesn't check the signature, so don't trust it for security decisions.

**5. Make the test set for your service.**

```bash
python mock_issuer.py negative --out negative-tokens.json
```

Your service should accept the tokens marked `accept` and reject the rest. The [Negative tests](#negative-tests) table says what each one checks.

### Running it as a server

If your service needs to fetch keys over HTTP, start the server:

```bash
python mock_issuer.py serve
```

It listens on `http://localhost:9000`. In another terminal, check that it's up:

```bash
curl http://localhost:9000/health
```

By default the server only accepts connections from this computer. Stop it with Ctrl+C.

If there are no keys yet, `serve` creates them and prints `No key found in keys. Generated a new RS256 key pair.` If you already ran `keygen`, it uses those keys. The Docker image relies on this to start with no setup.

### Something went wrong?

| What you see | What to do |
|---|---|
| `No key found at keys/private.pem` | Run step 2 (`keygen`) first. |
| `keys/private.pem already exists` | Your keys are already there. Use them, or add `--force` to replace them. Tokens from the old key stop verifying. |
| `Missing dependency. Run: pip install cryptography` | Run step 1. |
| `Connection refused` on port 9000 | The server isn't running. Start it with `serve`. |
| Your API rejects the `valid` token | Check that your API's issuer and audience match `ISSUER` and `AUDIENCE`, and that its JWKS URL points at this server. |
| `405` with "use POST /token" | Tokens are issued with POST. A GET request with `?sub=...` is not accepted. |
| `400` with "body is not valid JSON" | Check the request body. JSON must be an object, such as `{"sub": "alice"}`. |
| `401` with "missing or wrong X-Issuer-Secret header" | `ISSUE_SECRET` is set on the server. Send the same value in the `X-Issuer-Secret` header. |

## What it does

- Generates an RS256 (RSA 2048) or ES256 (P-256) signing key pair and a JWKS file
- Issues signed JWTs with `iss`, `sub`, `aud`, `iat`, `nbf`, `exp`, `jti`, and `roles`
- Produces a set of labeled bad tokens for your negative tests (expired, wrong issuer, wrong audience, `alg: none`, HS256 key confusion, bad signature, tampered payload, and more)
- Runs a small HTTP server with a JWKS endpoint and a token endpoint, so it can run as a service in Docker Compose

The tool needs Python 3.9+ and the `cryptography` package. The test script in `tests/` also needs `pyjwt`.

## Command reference

```bash
pip install cryptography

python mock_issuer.py keygen                  # RS256 by default; use --alg ES256 for EC
python mock_issuer.py issue --sub alice       # prints a token for user "alice"
python mock_issuer.py issue --sub bob --roles user,admin --ttl 600
python mock_issuer.py negative --out negative-tokens.json
python mock_issuer.py decode <token>          # shows header and payload, no verification
```

Keys are written to `keys/` (`private.pem`, `public.pem`, `jwks.json`). **Never commit `private.pem`.** Add `keys/` to your `.gitignore`.

Call your API with:

```bash
TOKEN=$(python mock_issuer.py issue --sub alice)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/...
```

## Running as a service (Docker Compose)

To run only the issuer, use the example file in this repo:

```bash
docker compose -f docker-compose.example.yml up --build
curl http://localhost:9000/health
```

The image runs as a non-root user (uid 10001) and listens on all interfaces inside the container. If you bind-mount a folder from your computer at `/app/keys` instead of the named volume, that folder must be writable by uid 10001, or the container fails at startup. The example publishes port 9000 as `127.0.0.1:9000:9000`, so only this computer can reach it. A bare `"9000:9000"` would publish it on every network interface.

To run it next to your API, copy `mock_issuer.py` and the `Dockerfile` into a folder in your repository, for example `token-issuer/`, then add a service:

```yaml
services:
  token-issuer:
    build: ./token-issuer
    ports:
      - "127.0.0.1:9000:9000"        # this computer only
    environment:
      ISSUER: http://localhost:9000
      AUDIENCE: cs763-api
      ALG: RS256
    volumes:
      - issuer-keys:/app/keys        # keeps the same keys across restarts

  api:
    build: ./secure-codebase
    depends_on:
      - token-issuer
    environment:
      JWT_JWK_SET_URI: http://token-issuer:9000/.well-known/jwks.json
      JWT_ISSUER: http://localhost:9000
      JWT_AUDIENCE: cs763-api

volumes:
  issuer-keys:
```

The `iss` value is just a string your service compares against. Use the same `ISSUER` in the issuer and in your API configuration. Inside Compose, your API fetches keys from `http://token-issuer:9000`, while you request tokens from your machine at `http://localhost:9000`.

### Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/.well-known/jwks.json` | Public keys your service uses to verify signatures |
| GET | `/.well-known/openid-configuration` | Discovery document |
| POST | `/token` | Issue a token. Form or JSON body: `sub=alice&roles=user,admin&ttl=3600`. `ttl` must be a positive integer. `aud` overrides the audience for that one token. |
| GET | `/negative?sub=alice&other_sub=bob` | Labeled good and bad tokens as JSON |
| GET | `/health` | Status, issuer, audience, alg, kid |

```bash
curl -s -X POST -d "sub=alice&roles=user" http://localhost:9000/token
```

If `ISSUE_SECRET` is set, `/token` and `/negative` also need the header `X-Issuer-Secret: <your secret>`. The JWKS and health endpoints stay open.

## Negative tests

`negative` (or `GET /negative`) returns JSON like:

```json
{
  "valid":            { "expect": "accept", "why": "...", "token": "..." },
  "expired":          { "expect": "reject", "why": "...", "token": "..." },
  ...
}
```

| Token | Your service should | What it tests |
|---|---|---|
| `valid` | accept | Baseline |
| `valid_other_user` | accept the token, but deny access to alice's records (403 or 404) | Row-level authorization |
| `expired` | reject (401) | `exp` validation |
| `not_yet_valid` | reject | `nbf` validation |
| `missing_exp` | reject | Requiring `exp` |
| `wrong_issuer` | reject | `iss` validation |
| `wrong_audience` | reject | `aud` validation |
| `valid_aud_array` | accept | `aud` as an array that includes your audience (RFC 7519) |
| `expired_2min` | reject | `exp` is 2 minutes in the past. That is beyond the 60 s clock skew that Spring and many libraries allow by default |
| `alg_none` | reject | Algorithm allowlist blocks `none` |
| `hs256_key_confusion` | reject | Algorithm allowlist blocks HS256 (attacker signs with your public key as an HMAC secret) |
| `alg_not_allowed` | reject | Header names an algorithm that is not on your allowlist (PS256 here) |
| `bad_signature` | reject | Signature verification |
| `tampered_payload` | reject | Payload changed after signing |
| `unknown_kid` | reject | Key not in the JWKS |
| `jku_injection` | reject | Header points at an attacker's key URL. Only use the JWKS URL you configured |
| `malformed` | reject | Input handling |

Tokens are time-sensitive (`exp` defaults to one hour), so generate them at the start of a test run rather than committing them.

## Configuration

| Setting | CLI flag | Environment variable | Default |
|---|---|---|---|
| Keys directory | `--keys-dir` | `KEYS_DIR` | `keys` |
| Algorithm | `--alg` | `ALG` | `RS256` |
| Issuer | `--iss` | `ISSUER` | `http://localhost:9000` |
| Audience | `--aud` | `AUDIENCE` | `cs763-api` |
| Token lifetime (seconds) | `--ttl` | `TOKEN_TTL` | `3600` |
| Port (serve) | `--port` | `PORT` | `9000` |
| Bind address (serve) | `--host` | `HOST` | `127.0.0.1` (the Docker image sets `0.0.0.0`) |
| Require a secret to issue | `--issue-secret` | `ISSUE_SECRET` | off (an empty value also means off) |

Extra claims: `issue --sub alice --claim tenant=7 --claim email=alice@example.com`.
The claims `iss`, `sub`, `aud`, `iat`, `nbf`, `exp`, `jti`, `roles`, and `scope` have their own flags, so `--claim` refuses them.

## Tests

The tool has two kinds of tests. They need `pyjwt` and `pytest` on top of `cryptography`. Install everything from `requirements-dev.txt`:

```bash
pip install -r requirements-dev.txt

python -m pytest tests/                    # unit and HTTP tests, a few seconds
python tests/verify_with_pyjwt.py          # checks every negative token against PyJWT
```

`verify_with_pyjwt.py` looks up each token's key by its `kid`, the way a real service should. It writes keys to a temporary folder and deletes them afterwards. It exits with an error if any token gets the wrong result.

## Spring Boot

See `examples/spring-boot/SecurityConfigExample.java` for a resource server configuration with an RS256/ES256 allowlist and `iss`, `aud`, and `exp` validation, plus a row-level authorization example. Other stacks can use any JWT library that supports a JWKS URL and an algorithm allowlist (for example, `jose` in Node or `PyJWT` in Python).

## Notes

- `roles` is a JSON array, and `scope` repeats the roles as a space-separated string, so either style of authority mapping works.
- `kid` is the RFC 7638 thumbprint of the public key, so it changes when you regenerate keys.
- If you regenerate keys, restart your API or let it refetch the JWKS.
- The source files start with an AI contribution header, as the CS763 AI logging policy asks for AI-generated code. Your own project files need their own headers and AI usage log.
