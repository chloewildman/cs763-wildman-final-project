package edu.bu.met.wildmancs763.cs763_wildman_final_project.entities;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Post {
    @Id 
    private int postId;
    private String postTitle;
    private String postBody;
    private LocalDateTime postCreationTimestamp;

    public Post(int postId, String postTitle, String postBody, LocalDateTime postCreationTimestamp, User author, Project project) {
        this.postId = postId;
        this.postTitle = postTitle;
        this.postBody = postBody;
        this.postCreationTimestamp = postCreationTimestamp;
        this.author = author;
        this.project = project;
    }

    @ManyToOne 
    private User author;

    @ManyToOne 
    private Project project;

    public User getAuthor() {
        return author;
    }

    public Project getProject() {
        return project;
    }   

    public int getPostId() {
        return postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getPostBody() {
        return postBody;
    }

    public LocalDateTime getPostCreationTimestamp() {
        return postCreationTimestamp;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public void setPostBody(String postBody) {
        this.postBody = postBody;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setPostCreationTimestamp(LocalDateTime postCreationTimestamp) {
        this.postCreationTimestamp = postCreationTimestamp;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }
}
