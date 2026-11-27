/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%

*/

package edu.bu.met.wildmancs763.discussions;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class TestJwtIssuer implements AutoCloseable {

    static final String ISSUER = "http://test-issuer.local";
    static final String AUDIENCE = "cs763-api";

    private final KeyPair rsaKeyPair;
    private final KeyPair ecKeyPair;
    private final KeyPair rogueKeyPair;
    private final HttpServer server;

    TestJwtIssuer() throws IOException, GeneralSecurityException {
        KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
        rsaGenerator.initialize(2048);
        rsaKeyPair = rsaGenerator.generateKeyPair();
        rogueKeyPair = rsaGenerator.generateKeyPair();
        KeyPairGenerator ecGenerator = KeyPairGenerator.getInstance("EC");
        ecGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecKeyPair = ecGenerator.generateKeyPair();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = jwks().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    String jwksUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
    }

    String issue(String subject, List<String> roles) {
        return issueWith(subject, roles, "RS256", Map.of(), Map.of(), rsaKeyPair, "rsa-test");
    }

    String issueEc(String subject, List<String> roles) {
        return issueWith(subject, roles, "ES256", Map.of(), Map.of(), ecKeyPair, "ec-test");
    }

    String issueWith(
            String subject,
            List<String> roles,
            String algorithm,
            Map<String, Object> claimOverrides,
            Map<String, Object> headerOverrides,
            KeyPair signingKey,
            String keyId) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", algorithm);
        header.put("typ", "JWT");
        header.put("kid", keyId);
        header.putAll(headerOverrides);

        Map<String, Object> claims = defaultClaims(subject, roles);
        for (Map.Entry<String, Object> override : claimOverrides.entrySet()) {
            if (override.getValue() == null) {
                claims.remove(override.getKey());
            } else {
                claims.put(override.getKey(), override.getValue());
            }
        }
        return encodeAndSign(header, claims, algorithm, signingKey);
    }

    String issueWith(
            String subject,
            List<String> roles,
            String algorithm,
            Map<String, Object> claimOverrides,
            Map<String, Object> headerOverrides) {
        return issueWith(subject, roles, algorithm, claimOverrides, headerOverrides, rsaKeyPair, "rsa-test");
    }

    String issueWithBadSignature(String subject, List<String> roles) {
        return issueWith(subject, roles, "RS256", Map.of(), Map.of(), rogueKeyPair, "rsa-test");
    }

    String issueWithUnknownKey(String subject, List<String> roles) {
        return issueWith(subject, roles, "RS256", Map.of(), Map.of(), rsaKeyPair, "unknown-key-id");
    }

    String issueWithAlgorithm(String algorithm, String subject, List<String> roles) {
        return issueWith(subject, roles, algorithm, Map.of(), Map.of(), rsaKeyPair, "rsa-test");
    }

    String issueWithJkuInjection() {
        return issueWith("alice", List.of("student"), "RS256", Map.of(),
                Map.of("jku", "https://evil.example.com/jwks.json"), rogueKeyPair, "rsa-test");
    }

    String issueNone() {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of("alg", "none", "typ", "JWT");
        Map<String, Object> claims = Map.of(
                "iss", ISSUER,
                "sub", "alice",
                "aud", AUDIENCE,
                "iat", now,
                "nbf", now,
                "exp", now + 3600,
                "roles", List.of("student"));
        return encodeAndSign(header, claims, "none", null);
    }

    String tamperPayload(String token) {
        String[] parts = token.split("\\.");
        Map<String, Object> claims = defaultClaims("alice", List.of("student"));
        claims.put("sub", "bob");
        claims.put("roles", List.of("admin"));
        String changedPayload = base64Url(writeJson(claims));
        return parts[0] + "." + changedPayload + "." + parts[2];
    }

    String malformedToken() {
        return "not.a.jwt";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private String jwks() {
        Map<String, Object> result = Map.of("keys", List.of(rsaJwk(), ecJwk()));
        return new String(writeJson(result), StandardCharsets.UTF_8);
    }

    private Map<String, Object> rsaJwk() {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        return Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", "rsa-test",
                "n", base64Url(unsigned(publicKey.getModulus())),
                "e", base64Url(unsigned(publicKey.getPublicExponent())));
    }

    private Map<String, Object> ecJwk() {
        ECPublicKey publicKey = (ECPublicKey) ecKeyPair.getPublic();
        return Map.of(
                "kty", "EC",
                "use", "sig",
                "alg", "ES256",
                "kid", "ec-test",
                "crv", "P-256",
                "x", base64Url(fixed32(publicKey.getW().getAffineX())),
                "y", base64Url(fixed32(publicKey.getW().getAffineY())));
    }

    private String encodeAndSign(
            Map<String, Object> header,
            Map<String, Object> claims,
            String algorithm,
            KeyPair signingKey) {
        String signingInput = base64Url(writeJson(header)) + "." + base64Url(writeJson(claims));
        byte[] signature;
        try {
            signature = switch (algorithm) {
                case "RS256" -> signRsa(signingInput, signingKey);
                case "ES256" -> derToJose(signEc(signingInput, signingKey));
                case "HS256" -> signHmac(signingInput);
                case "none" -> new byte[0];
                default -> new byte[]{1};
            };
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to create test token.", exception);
        }
        return signingInput + "." + base64Url(signature);
    }

    private byte[] signRsa(String signingInput, KeyPair keyPair) throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signer.sign();
    }

    private byte[] signEc(String signingInput, KeyPair keyPair) throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signer.sign();
    }

    private byte[] signHmac(String signingInput) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(rsaKeyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        mac.init(new SecretKeySpec(pem.getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] derToJose(byte[] der) {
        int offset = 2;
        if ((der[1] & 0x80) != 0) {
            offset += (der[1] & 0x7f);
        }
        int rLength = der[offset + 1] & 0xff;
        byte[] r = new byte[rLength];
        System.arraycopy(der, offset + 2, r, 0, rLength);
        offset += rLength + 2;
        int sLength = der[offset + 1] & 0xff;
        byte[] s = new byte[sLength];
        System.arraycopy(der, offset + 2, s, 0, sLength);
        byte[] raw = new byte[64];
        copyInteger(r, raw, 0);
        copyInteger(s, raw, 32);
        return raw;
    }

    private void copyInteger(byte[] source, byte[] target, int targetOffset) {
        int sourceOffset = source.length > 32 ? source.length - 32 : 0;
        int length = source.length - sourceOffset;
        System.arraycopy(source, sourceOffset, target, targetOffset + 32 - length, length);
    }

    private byte[] unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        return bytes.length > 1 && bytes[0] == 0
                ? java.util.Arrays.copyOfRange(bytes, 1, bytes.length)
                : bytes;
    }

    private byte[] fixed32(BigInteger value) {
        byte[] bytes = unsigned(value);
        byte[] result = new byte[32];
        System.arraycopy(bytes, Math.max(0, bytes.length - 32), result,
                Math.max(0, 32 - bytes.length), Math.min(32, bytes.length));
        return result;
    }

    private byte[] writeJson(Object value) {
        return jsonValue(value).getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Object> defaultClaims(String subject, List<String> roles) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", subject);
        claims.put("aud", AUDIENCE);
        claims.put("iat", now);
        claims.put("nbf", now);
        claims.put("exp", now + 3600);
        claims.put("jti", "test-token");
        claims.put("roles", new ArrayList<>(roles));
        return claims;
    }

    private String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return quote(text);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            List<String> fields = new ArrayList<>();
            map.forEach((key, fieldValue) ->
                    fields.add(quote(key.toString()) + ":" + jsonValue(fieldValue)));
            return "{" + String.join(",", fields) + "}";
        }
        if (value instanceof Iterable<?> values) {
            List<String> fields = new ArrayList<>();
            values.forEach(item -> fields.add(jsonValue(item)));
            return "[" + String.join(",", fields) + "]";
        }
        throw new IllegalArgumentException("Unsupported JSON test value.");
    }

    private String quote(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
