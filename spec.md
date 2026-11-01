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
      "type": "string"
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
    "projectCreationTimestamp": {
      "type": "string"
    }
  },
  "required": [
    "projectId",
    "projectTitle",
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
    "postTimestamp": {
      "type": "string"
    },
    "postBody": {
      "type": "string"
    }
  },
  "required": [
    "postId",
    "postTitle",
    "authorId",
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
    "commentTimestamp": {
      "type": "string"
    },
    "commentBody": {
      "type": "string"
    }
  },
  "required": [
    "commentId",
    "authorId",
    "commentTimestamp",
    "commentBody"
  ]
}

# Database Models
Primary tables would be
- User
- Course
- Project
- Post
- Comment

An association table would be
- Assignments

Where...
- User (Student) -- Course (many-to-many)
- User (Professor) -- Course (one-to-many)
- User -- Post (one-to-many)
- User -- Comment (one-to-many)
- Course -- Project (one-to-many)
- Project -- Post (one-to-many)
- Post -- Comment (one-to-many)

# Mock Data Structures

## User

| userId | username | firstName | lastName | role |
|--------|----------|-----------|----------|------|
| 5 | lgilmore | Lorelai | Gilmore | STUDENT |
| 6 | ldanes | Luke | Danes | STUDENT |
| 7 | sstjames | Sookie | St. James | STUDENT |
| 8 | jdoe | John | Doe | PROFESSOR |
| 9 | jadoe | Jane | Doe | PROFESSOR |

Primary key = userId

## Course
| courseId | courseName | courseTitle | courseCreationTimestamp |
|----------|------------|-------------|--------------------|
| 22 | BUS101 | Business 101 | 2000-08-05 00:00:00 |
| 23 | WOOD100 | Intro to Carving | 2002-08-01 00:00:00 | 

Primary key = courseId

## Project
| projectId | projectTitle | projectCreationTimestamp | courseId |
|-----------|--------------|---------------------|----------|
| 43 | Grant Proposal Project | 2000-09-01 13:12:00 | 22 |
| 44 | Chuppah Project | 2003-02-13 09:00:00 | 23 |

Primary key = projectId

Foreign key = courseId

## Post
| postId | postTitle | authorId | postTimestamp | postBody | projectId |
|--------|-----------|-----------|----------|----------|-----------|
| 1 | Reschedule meeting? | 5 | 2000-09-05 08:57:00 | I'm running late! Can we move the meeting to 9:30? | 43 |
| 2 | Reminder to contribute | 6 | 2003-02-28 23:46:04 | Just a reminder that the chuppah needs to be done by next week. I can't make it alone! | 44 |

Primary key = postId

Foreign key = authorId -> User.userId, projectId

## Comment
| commentId | authorId | commentTimestamp | commentBody | postId |
|-----------|----------|-------------|-------------|--------|
| 1 | 7 | 2000-09-05 09:01:00 | Oh gosh that is so like you! (: Happy to reschedule! | 1 |
| 2 | 5 | 2000-09-05 09:05:32 | What can I say! | 1 |
| 3 | 6 | 2003-03-05 08:02:12 | Nevermind, I did it myself. | 2 |

Primary key = commentId

Foreign key = authordId -> User.userId, postId

## Assignments
| userId | courseId | role |
|--------|----------|------|
| 5 | BUS101 | STUDENT |
| 6 | WOOD100 | STUDENT |
| 7 | BUS101 | STUDENT |
| 8 | WOOD100 | PROFESSOR |
| 9 | BUS101 | PROFESSOR |

Foreign key = userId, courseId