package edu.bu.met.wildmancs763.cs763_wildman_final_project.entities;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Comment {
    @Id
    private int commentId;
    private String commentBody;
    private LocalDateTime commentCreationTimestamp;

    public Comment(int commentId, String commentBody, LocalDateTime commentCreationTimestamp, User author, Post post) {
        this.commentId = commentId;
        this.commentBody = commentBody;
        this.commentCreationTimestamp = commentCreationTimestamp;
        this.author = author;
        this.post = post;
    }

    @ManyToOne 
    private User author;

    public User getAuthor() {
        return author;
    }

    @ManyToOne 
    private Post post;

    public Post getPost() {
        return post;
    }

    public void setCommentBody(String commentBody) {
        this.commentBody = commentBody;
    }

    public String getCommentBody() {
        return commentBody;
    }

    public LocalDateTime getCommentCreationTimestamp() {
        return commentCreationTimestamp;
    }   
    public void setAuthor(User author) {
        this.author = author;
    }

    public void setPost(Post post) {
        this.post = post;
    }

}
