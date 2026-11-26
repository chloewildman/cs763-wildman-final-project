# Chloe Wildman CS763 Project 3 Assignment

## Contribution Header

### For README.md:
AI Contribution Weight: 0%

Human Contribution: 100%

## Deliverables
- prompt-architecture.md
- secure-codebase/
- JWT/authorization reference:
    -  (recommended: note which one, 403 or 404, in your README).
- cross_service_remediation_analysis.md
- AI usage (.ai_logs/)

## Secure service runtime

The discussion service accepts RS256 or ES256 bearer tokens from the mock issuer configured by Docker Compose. Only `POST /api/v1/auth/login` is public; send the token in the `Authorization: Bearer ...` header. Other API and actuator routes require authentication, and actuator routes require the `admin` role.

Set a Base64-encoded 32-byte `FIELD_ENCRYPTION_KEY` in the root `.env` file before starting the service. The root `.env.example` is a placeholder, `.env` is ignored by Git, and the same environment variable is passed to the container. Account records are provisioned by administrators; a provisioned user must set a 16–72 character compliant password before using other protected APIs.

See the repository-root [prompt-architecture.md](../prompt-architecture.md) for role mappings, mock memberships, authorization semantics, encryption, rate limiting, and the production/test generation order.
