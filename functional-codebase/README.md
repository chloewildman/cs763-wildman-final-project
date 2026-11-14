# Research Blog & Project Discussion Board

Spring Boot REST service for scoped academic announcements, project update posts, nested comments, moderation, search, and timeline reads.

## Run and test

From the repository root:

```powershell
.\mvnw.cmd -f functional-codebase\pom.xml spring-boot:run
.\mvnw.cmd -f functional-codebase\pom.xml test
```

The service listens on port `8080` and uses an in-memory H2 database. To build and run the container with `functional-codebase` as the build context:

```powershell
docker build -t research-discussion-board functional-codebase
docker run --rm -p 8080:8080 research-discussion-board
```

## API

All routes are under `/api/v1/discussions`.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST`, `GET` | `/posts` | Create/list posts; list supports `keyword`, `projectId`, `courseId`, `labId`, `category`, `authorHandle`, `from`, and `to`. |
| `GET`, `PUT`, `DELETE` | `/posts/{postId}` | Read, replace, or delete a post. |
| `GET` | `/scopes/{projects\|courses\|labs}/{scopeId}/timeline` | Read a chronological scoped timeline; accepts `keyword`, `category`, `from`, and `to`. |
| `POST`, `GET` | `/posts/{postId}/comments` | Create/list nested comments; listing supports `keyword`, `authorHandle`, `from`, and `to`. Use `?format=text` to download a text manifest. |
| `GET`, `PUT`, `DELETE` | `/posts/{postId}/comments/{commentId}` | Read, edit, or delete a comment. |
| `PATCH` | `/posts/{postId}/moderation` | Set a post status to `active`, `reported`, or `hidden_by_moderator`. |
| `PATCH` | `/posts/{postId}/comments/{commentId}/moderation` | Set a comment moderation status. |

Post categories are `ACADEMIC_ANNOUNCEMENT` and `PROJECT_UPDATE_BLOG`. A post request must provide exactly one of `projectId`, `courseId`, or `labId`. Comment requests use `message`, `authorHandle`, and an optional `parentCommentId`. Hiding a post hides its comment stream; hiding a comment also hides its recursive replies.

The specification does not define an authentication or faculty identity provider. Moderation routes currently perform status management without role authorization and must be protected by the institution's authentication layer before public deployment.
