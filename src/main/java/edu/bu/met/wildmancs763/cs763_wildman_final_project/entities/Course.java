package edu.bu.met.wildmancs763.cs763_wildman_final_project.entities;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Course {
    @Id
    private int courseId;
    private String courseName;
    private String courseTitle;
    private LocalDateTime courseCreationTimestamp;

    public Course(int courseId, String courseName, String courseTitle, LocalDateTime courseCreationTimestamp, User professor) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseTitle = courseTitle;
        this.courseCreationTimestamp = courseCreationTimestamp;
        this.professor = professor;
    }

    @ManyToOne 
    private User professor;

    @ManyToMany(mappedBy = "courses")
    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public LocalDateTime getCourseCreationTimestamp() {
        return courseCreationTimestamp;
    }

    public User getProfessor() {
        return professor;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public void setCourseCreationTimestamp(LocalDateTime courseCreationTimestamp) {
        this.courseCreationTimestamp = courseCreationTimestamp;
    }

    public void setProfessor(User professor) {
        this.professor = professor;
    }
}
