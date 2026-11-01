# Research Blog Student Demo

This Spring Boot prototype demonstrates the student interactions in `spec.md`. The demo stores sample data and changes in memory; they reset when the application restarts.

## Run

Set a local password for the demo student accounts and start the service in PowerShell:

```powershell
$env:DEMO_STUDENT_PASSWORD = "<choose-a-local-password>"
.\mvnw.cmd spring-boot:run
```

The demo accounts are `lgilmore`, `ldanes`, and `sstjames`; all use the password set in `DEMO_STUDENT_PASSWORD`.

## Student capabilities

Students can:

- List and view projects assigned to them.
- Read posts and comments in those projects.
- Create posts and edit their own posts.
- Add comments to posts in their assigned projects.

All other routes and HTTP methods are denied. A student cannot access another student's assigned project or edit another student's post.

## Try it

In another PowerShell window, list Lorelai's assigned projects:

```powershell
curl.exe --user "lgilmore:$env:DEMO_STUDENT_PASSWORD" http://localhost:8080/courses/22/projects
```

Create a post in project `43`:

```powershell
curl.exe --user "lgilmore:$env:DEMO_STUDENT_PASSWORD" `
  -H "Content-Type: application/json" `
  -d '{"postTitle":"Meeting update","postBody":"The meeting is at 9:30."}' `
  http://localhost:8080/courses/22/projects/43/posts
```

Run the integration tests with `.\mvnw.cmd test`.
