# Security Architecture and Verification

## AI Contribution Header
AI contribution: 99%

Human contribution: 1%

This file was written by the AI agent after making the changes requested in my secure prompt. The only change I made was moving information it placed in the README into this file to keep AI generated information in one place.

## Identity and roles

The service is a stateless Spring Boot resource server. It validates bearer JWTs from the configured mock issuer or a compatible identity provider. `POST /api/v1/auth/login` accepts a validated bearer token and returns its subject and effective roles; it does not issue tokens or accept username/password credentials. Every other API route, including actuator endpoints, requires authentication.

The JWT `sub` is the caller identity and is stored as the post/comment owner. Membership is never accepted from token claims or request bodies. Non-admin callers must have an administrator-provisioned local account and set a password before accessing protected APIs. The authenticated password-setup route is available to a provisioned account that has not set its password yet. Administrators are trusted from the signed JWT so that a first administrator can provision accounts.

| JWT role | Service authority | Access |
|---|---|---|
| `admin` | `ROLE_ADMIN` | All scopes, account provisioning, actuator, content management and moderation |
| `faculty_instructor` (legacy `faculty` is also recognized) | `ROLE_FACULTY_INSTRUCTOR` | Assigned scopes, announcements and moderation |
| `student` | `ROLE_STUDENT` | Assigned scopes, ordinary posts/comments, and own content |
| `student_peer` | `ROLE_STUDENT_PEER` | Student-level access; no faculty or admin privileges |
| Any other non-empty role | `ROLE_STUDENT` | Student-level access; no faculty or admin privileges |

Only the JWT `roles` array grants authority. A role value stored on a provisioned account is descriptive and cannot override the signed token.

## JWT validation

`JWT_JWK_SET_URI`, `JWT_ISSUER`, and `JWT_AUDIENCE` configure the trusted issuer. The decoder allows only RS256 and ES256, uses only the configured JWKS URL, rejects `jku`/`x5u` headers, and validates `exp`, `iss`, `aud`, `nbf`, `sub`, and the `roles` array. The implementation uses consistent generic authentication errors. Since the service has no username/password login path, it does not disclose whether a local username or password is correct.

## Scope membership and row authorization

Local mock memberships are maintained in `LocalMembershipRegistry`:

| Scope | Members |
|---|---|
| `project-42` | `alice`, `bob` |
| `project-1` | `alice`, `bob` |
| `project-99` | `alice` (single-member isolation fixture) |
| `course-9` | `alice`, `bob`, `professor1` |
| `lab-1` | `alice`, `bob`, `professor1` |

Active posts and comments are visible only to assigned users and administrators. Hidden content is visible only to its author, faculty instructors, or administrators. An author or administrator may update/delete a visible resource; updates by a non-author return 403. Requests for an inaccessible row consistently return 404. Lists, timelines, comment searches, and text manifests apply the same visibility checks.

## Sensitive fields and input handling

Moderation notes are encrypted before persistence with AES-256-GCM and a fresh 96-bit nonce for each value. The key must be provided as Base64 through `FIELD_ENCRYPTION_KEY` and decode to 32 bytes. Notes are decrypted only for faculty instructors and administrators. `.env.example` contains a placeholder, `.env` is ignored, and Compose passes the environment variable to the service.

Post titles/bodies/tags, comment messages, and moderation notes are reduced to plain text using Jsoup before storage. Passwords must be 16–72 characters and include uppercase, lowercase, a digit, and an allowed special character; angle brackets, ampersand, slash, and quote characters are excluded. Password hashes use PBKDF2.

## Availability and audit

The service permits 50 requests per subject in a rolling 60-second window. The next request starts a 10-second lockout and returns 429 with `Retry-After`. Keyword searches, timelines, and text-manifest exports also require a 250 ms minimum interval. Results are capped at 500 entries.

The audit filter records username/subject, action, resource ID, outcome, and UTC timestamp. It logs neither authorization headers/tokens nor request bodies.

The API is stateless and accepts bearer credentials only through the `Authorization` header. It does not issue session or CSRF cookies; any servlet session cookie, if introduced by a future integration, is configured Secure and HttpOnly.

## Local setup

From `secure-codebase/mock-token-issuer`, generate a local signing key and issue test tokens with the included mock issuer. Its `keys/` directory is ignored by Git. Generate an independent encryption key and place it in an ignored root `.env` file:

```powershell
python mock_issuer.py keygen
python mock_issuer.py issue --sub alice --roles student
python -c "import base64,secrets; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

Set the generated Base64 value as `FIELD_ENCRYPTION_KEY` in `.env`, then run `docker compose up --build`. Do not reuse the mock issuer keys or this local configuration in production. In production, configure a trusted identity provider and supply the encryption key through a secret manager.

## Test/production generation order

**Production-first.** The security model and production implementation were established first because moving from sessions to issuer-validated JWTs changed the authentication boundary, role mapping, and account lifecycle. The existing functional tests were then adapted to the bearer-token identity model, and focused security tests were added for the new authorization, validation, encryption, rate-limit, and audit behavior.

## Secure service runtime

The discussion service accepts RS256 or ES256 bearer tokens from the mock issuer configured by Docker Compose. Only `POST /api/v1/auth/login` is public; send the token in the `Authorization: Bearer ...` header. Other API and actuator routes require authentication, and actuator routes require the `admin` role.

Set a Base64-encoded 32-byte `FIELD_ENCRYPTION_KEY` in the root `.env` file before starting the service. The root `.env.example` is a placeholder, `.env` is ignored by Git, and the same environment variable is passed to the container. Account records are provisioned by administrators; a provisioned user must set a 16–72 character compliant password before using other protected APIs.

See the repository-root [prompt-architecture.md](../prompt-architecture.md) for role mappings, mock memberships, authorization semantics, encryption, rate limiting, and the production/test generation order.
