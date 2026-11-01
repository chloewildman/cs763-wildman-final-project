package edu.bu.met.wildmancs763.cs763_wildman_final_project.entities;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
public class Project {
    @Id
    private int projectId;
    private String projectTitle;
    private LocalDateTime projectCreationTimestamp;

    public Project(int projectId, String projectTitle, LocalDateTime projectCreationTimestamp, Course course) {
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.projectCreationTimestamp = projectCreationTimestamp;
        this.course = course;
    }

    public Course getCourse() {
        return course;
    }

    @ManyToOne 
    private Course course;

    public int getProjectId() {
        return projectId;
    }   
    public String getProjectTitle() {
        return projectTitle;
    }   
    public LocalDateTime getProjectCreationTimestamp() {
        return projectCreationTimestamp;
    }   

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public void setCourse(Course course) {
        this.course = course;
    }   

    public void setProjectCreationTimestamp(LocalDateTime projectCreationTimestamp) {
        this.projectCreationTimestamp = projectCreationTimestamp;
    }
}
