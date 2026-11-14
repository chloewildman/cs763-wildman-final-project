# The Research Blog & Project Discussion Board (Domain 4)

This service coordinates collaborative project updates, announcements, and interactive comment feeds to streamline communication within active academic project teams and course groups.

## Functional Requirements
Root Post Lifecycle Engine: Uniquely identify (UUID), initialize, and maintain the primary lifecycle of top-level communication feeds, categorized explicitly as either an official Academic Announcement or a Project Update Blog (storing title, rich text body content, categorization tags, and creation timestamps). To support multifaceted campus collaboration, root threads must accommodate hierarchical scoping rules: a discussion board or announcement feed can be bound directly to a single specific Project ID, a broad Course ID (encompassing multiple student projects), or an entire overarching Research Lab ID.

Differentiated Nested Comment Threading: Process separate child comment response entries that attach to a specific root post ID. Comments are lightweight entities that do not contain title headers or categorization tags; they focus strictly on message strings, timestamp tracking, and author handles. To support multi-tiered discussions, comments must utilize a self-referential adjacency list model, where a comment can serve as a direct reply to a root post or a recursive reply to an existing comment.

Moderation Pipeline & Status Management: Provide an administrative moderation workflow to enforce community standards across text inputs. The service must support state transitions (active, reported, hidden_by_moderator) applied independently to either root posts or child comments, allowing faculty roles to flag inputs and trigger cascading visibility adjustments across the board.

Complex Multi-Field Query: Support robust text-search and filtering capabilities across the communication canvas. Users must be able to query and filter topics, blogs, and comments simultaneously using string keyword scans, timestamp ranges, specific author handles, target Project IDs, Course IDs, or Lab-specific project channels. The system must actively support targeted keyword searching and author filtering within individual comment streams.

## REST APIs

Endpoint: /api/v1/discussions/posts
Verbs: POST, GET
Description: Create a primary announcement or blog post. Query parameters let users execute full-text keyword searches across post bodies or filter the entire portal communication feed by a specific Project ID, Course ID, Lab ID, or post category.

Endpoint: /api/v1/discussions/posts/{post_id}
Verbs: GET, PUT, DELETE
Description: Read, update, and delete a primary announcement thread or project blog post.

Endpoint: /api/v1/discussions/scopes/{scope_type}/{scope_id}/timeline
Verbs: GET
Description: Read a unified chronological workspace timeline aggregating all root announcements and team update blogs matching a specific structural context. (Note: {scope_type} accepts values restricted strictly to projects, courses, or labs to load the appropriate multi-project feed).

Endpoint: /api/v1/discussions/posts/{post_id}/comments
Verbs: POST, GET
Description: Post an interactive comment response under a targeted primary announcement/blog thread. Read the full chronological JSON array of comments linked to that specific post. Downloading the full comment manifest in a single text file format for team archival audits must also be supported. Query parameters let users execute full-text keyword searches across comment bodies or filter by a specific author.

Endpoint: /api/v1/discussions/posts/{post_id}/comments/{comment_id}
Verbs: PUT, DELETE, GET
Description: Edit, delete, or fetch a specific nested comment text record. 