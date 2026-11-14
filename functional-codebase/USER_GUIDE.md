# User accounts and discussion API

The API supports local accounts and session-based login. Public registration creates a `STUDENT` account; only an administrator can provision `FACULTY` or `ADMIN` accounts. Posts and comments use the authenticated username as their author, so client-supplied author handles are ignored.

## Getting a session

State-changing requests require both the `JSESSIONID` session cookie and a CSRF token. Start by requesting `GET /api/v1/auth/csrf`, retaining the `XSRF-TOKEN` cookie and the `token` in the response. Send the cookie and token in the `X-XSRF-TOKEN` header when calling `POST /api/v1/auth/register` with a username and password (at least 12 characters). Then request a fresh CSRF token and call `POST /api/v1/auth/login` with the same credentials. Retain the session cookie returned by login; fetch a fresh CSRF token after login and include it on subsequent `POST`, `PUT`, `PATCH`, and `DELETE` requests.

`GET /api/v1/auth/me` returns the signed-in account, and `POST /api/v1/auth/logout` ends the session. Discussion reads remain public. Authenticated users can create content and edit or delete their own content; administrators can manage any content. Faculty and administrators can use the existing moderation endpoints. User provisioning is available at `POST /api/v1/admin/users` to administrators.

## Initial administrator and persistence

Configure both `APP_BOOTSTRAP_ADMIN_USERNAME` and `APP_BOOTSTRAP_ADMIN_PASSWORD` before starting the service to create the initial administrator. Use a unique, strong password and keep it in a deployment secret store; do not commit it. The bootstrap account is created only when it does not already exist. If the configured name belongs to a non-admin account, startup fails rather than silently elevating it.

By default, the service persists H2 data under `./data/discussions`. In Docker, mount a durable volume at `/app/data`. `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` can configure an alternate datasource when its JDBC driver is included in the application. Deploy behind HTTPS and set `SESSION_COOKIE_SECURE=true` in production.
