package edu.bu.met.wildmancs763.cs763_wildman_final_project.entities;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class User {
    @Id
    private int userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    public User(int userId, String username, String firstName, String lastName, String email, UserRole userRole) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userRole = userRole;
    }

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @ManyToMany 
    private List<Course> courses;

    public int getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public List<Course> getCourses() {
        return courses;
    }
}
