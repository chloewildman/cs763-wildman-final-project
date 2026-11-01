# Class notes, to be deleted
Something small to show the app will work through the pipeline correctly
Include tests that should run and be successful before the rest of the pipeline runs
Use secrete scanning

# Service definition
The Research Blog & Project Discussion Board (Domain 4): This service coordinates collaborative project updates, announcements, and interactive comment feeds to streamline communication within active academic project teams and course groups.

# Endpoints
GET /users

GET /users/{userId}

GET /courses

GET /courses/{courseId}

GET /courses/{courseId}/projects

POST /courses/{courseId}/projects

GET /courses/{courseId}/projects/{projectId}

GET /courses/{courseId}/projects/{projectId}/posts

POST /courses/{courseId}/projects/{projectId}/posts

GET /courses/{courseId}/projects/{projectId}/posts/{postId}

GET /courses/{courseId}/projects/{projectId}/posts/{postId}/comments

POST /courses/{courseId}/projects/{projectId}/posts/{postId}/comments

GET /courses/{courseId}/projects/{projectId}/posts/{postId}/comments/{commentId}

# API Behavior and Acceptance Criteria
- All successful responses use `application/json`.
- A collection `GET` returns a JSON array, including an empty array when there are no matching resources. A single-resource `GET` returns one JSON object.
- A successful `GET` returns HTTP `200`. A successful `POST` returns HTTP `201` and the created resource representation.
- A request with an invalid body or invalid field value returns HTTP `400`. A request for a missing resource, or a child resource that does not belong to the parent in the URL, returns HTTP `404`.
- `POST /courses/{courseId}/projects` accepts `projectTitle`; the course association comes from `{courseId}`.
- `POST /courses/{courseId}/projects/{projectId}/posts` accepts `postTitle` and `postBody`; the project association comes from `{projectId}`.
- `POST /courses/{courseId}/projects/{projectId}/posts/{postId}/comments` accepts `commentBody`; the post association comes from `{postId}`.
- The service generates resource IDs and creation timestamps.
- Acceptance tests must verify that collection endpoints return arrays, empty collections return `[]`, created resources retain the parent ID from the URL, and a child requested beneath the wrong parent returns `404`.
- The smoke test in the delivery requirements must run after the service starts and before later pipeline stages.

# Open Decisions
- [NEEDS CLARIFICATION] Authentication and authorization: specify how the caller is identified, which users may read or create each resource, and whether `authorId` is derived from the caller or supplied in the request.
- [NEEDS CLARIFICATION] Collection behavior: specify supported filters, default sort order, pagination, and maximum page size (or state that collections are unpaginated).
- [NEEDS CLARIFICATION] Field constraints: specify length limits for names, titles, and bodies, plus any uniqueness rules for usernames, emails, and course names.

# JSON Schemas

## User
{
  "type": "object",
  "properties": {
    "userId": {
      "type": "integer"
    },
    "username": {
      "type": "string"
    },
    "firstName": {
      "type": "string"
    },
    "lastName": {
      "type": "string"
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "role": {
      "type": "string",
      "enum": ["PROFESSOR", "STUDENT"]
    }
  },
  "required": [
    "userId",
    "username",
    "firstName",
    "lastName",
    "email",
    "role"
  ]
}

## Course
{
  "type": "object",
  "properties": {
    "courseId": {
      "type": "integer"
    },
    "courseName": {
      "type": "string"
    },
    "courseTitle": {
      "type": "string"
    },
    "courseCreationTimestamp": {
      "type": "string",
      "format": "date-time"
    }
  },
  "required": [
    "courseId",
    "courseName",
    "courseTitle",
    "courseCreationTimestamp"
  ]
}


## Project
{
  "type": "object",
  "properties": {
    "projectId": {
      "type": "integer"
    },
    "projectTitle": {
      "type": "string"
    },
    "courseId": {
      "type": "integer"
    },
    "projectCreationTimestamp": {
      "type": "string",
      "format": "date-time"
    }
  },
  "required": [
    "projectId",
    "projectTitle",
    "courseId",
    "projectCreationTimestamp"
  ]
}

## Post
{
  "type": "object",
  "properties": {
    "postId": {
      "type": "integer"
    },
    "postTitle": {
      "type": "string"
    },
    "authorId": {
      "type": "integer"
    },
    "projectId": {
      "type": "integer"
    },
    "postTimestamp": {
      "type": "string",
      "format": "date-time"
    },
    "postBody": {
      "type": "string"
    }
  },
  "required": [
    "postId",
    "postTitle",
    "authorId",
    "projectId",
    "postTimestamp",
    "postBody"
  ]
}

## Comment
{
  "type": "object",
  "properties": {
    "commentId": {
      "type": "integer"
    },
    "authorId": {
      "type": "integer"
    },
    "postId": {
      "type": "integer"
    },
    "commentTimestamp": {
      "type": "string",
      "format": "date-time"
    },
    "commentBody": {
      "type": "string"
    }
  },
  "required": [
    "commentId",
    "authorId",
    "postId",
    "commentTimestamp",
    "commentBody"
  ]
}

# Database Models
Primary tables:
- User
- Course
- Project
- Post
- Comment

Association table:
- Assignments

- `User.role` identifies the user's account role. Each Assignments row records that user's role in a course; it must match `User.role`.
- A student may be assigned to multiple courses, and a course may have multiple student assignments.
- A professor may be assigned to multiple courses, and each course has exactly one professor assignment.
- User -- Post (one-to-many)
- User -- Comment (one-to-many)
- Course -- Project (one-to-many)
- Project -- Post (one-to-many)
- Post -- Comment (one-to-many)
- `Assignments` has a composite primary key of (`userId`, `courseId`). Both columns are integer foreign keys to the corresponding primary keys.
- Every project belongs to one course; every post belongs to one project and has one author; every comment belongs to one post and has one author.

# Mock Data Structures

## User

| userId | username | firstName | lastName | email | role |
|--------|----------|-----------|----------|-------|------|
| 5 | lgilmore | Lorelai | Gilmore | lorelai@example.edu | STUDENT |
| 6 | ldanes | Luke | Danes | luke@example.edu | STUDENT |
| 7 | sstjames | Sookie | St. James | sookie@example.edu | STUDENT |
| 8 | jdoe | John | Doe | john.doe@example.edu | PROFESSOR |
| 9 | jadoe | Jane | Doe | jane.doe@example.edu | PROFESSOR |

Primary key = userId

## Course
| courseId | courseName | courseTitle | courseCreationTimestamp |
|----------|------------|-------------|-------------------------|
| 22 | BUS101 | Business 101 | 2000-08-05T00:00:00Z |
| 23 | WOOD100 | Intro to Carving | 2002-08-01T00:00:00Z |

Primary key = courseId

## Project
| projectId | projectTitle | projectCreationTimestamp | courseId |
|-----------|--------------|---------------------------|----------|
| 43 | Grant Proposal Project | 2000-09-01T13:12:00Z | 22 |
| 44 | Chuppah Project | 2003-02-13T09:00:00Z | 23 |

Primary key = projectId

Foreign key = courseId -> Course.courseId

## Post
| postId | postTitle | authorId | postTimestamp | postBody | projectId |
|--------|-----------|----------|--------------|----------|-----------|
| 1 | Reschedule meeting? | 5 | 2000-09-05T08:57:00Z | I'm running late! Can we move the meeting to 9:30? | 43 |
| 2 | Reminder to contribute | 6 | 2003-02-28T23:46:04Z | Just a reminder that the chuppah needs to be done by next week. I can't make it alone! | 44 |

Primary key = postId

Foreign keys = authorId -> User.userId, projectId -> Project.projectId

## Comment
| commentId | authorId | commentTimestamp | commentBody | postId |
|-----------|----------|------------------|-------------|--------|
| 1 | 7 | 2000-09-05T09:01:00Z | Oh gosh that is so like you! (: Happy to reschedule! | 1 |
| 2 | 5 | 2000-09-05T09:05:32Z | What can I say! | 1 |
| 3 | 6 | 2003-03-05T08:02:12Z | Nevermind, I did it myself. | 2 |

Primary key = commentId

Foreign keys = authorId -> User.userId, postId -> Post.postId

## Assignments
| userId | courseId | role |
|--------|----------|------|
| 5 | 22 | STUDENT |
| 6 | 23 | STUDENT |
| 7 | 22 | STUDENT |
| 8 | 23 | PROFESSOR |
| 9 | 22 | PROFESSOR |

Primary key = (`userId`, `courseId`)

Foreign keys = userId -> User.userId, courseId -> Course.courseId