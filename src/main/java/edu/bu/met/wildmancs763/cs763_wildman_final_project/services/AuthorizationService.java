package edu.bu.met.wildmancs763.cs763_wildman_final_project.services;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Post;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Comment;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.UserRole;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.CourseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
    private final CourseRepository courseRepository;

    public AuthorizationService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // Deferring to one service to check course belonging
    public void requireCourseAccess(int userId, int courseId) {
        boolean hasAccess = courseRepository.userHasCourseAccess(userId, courseId);
        if (!hasAccess) {
            throw new AccessDeniedException("User does not have access to this course");
        }
    }

    // Deferring to one service to check post ownership when a student attempts to
    // modify it
    public void requirePostOwnership(User user, Post post) {
        if (user.getUserRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getUserRole() == UserRole.PROFESSOR) {
            return;
        }
        if (user.getUserRole() == UserRole.STUDENT
                && post.getAuthor().getUserId() == user.getUserId()) {
            return;
        }

        throw new AccessDeniedException("User cannot modify this post. Students may only modify their own posts.");
    }

    // Deferring to one service to check post/comment ownership when a student
    // attempts to modify it
    public void requireCommentOwnership(User user, Comment comment) {
        if (user.getUserRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getUserRole() == UserRole.PROFESSOR) {
            return;
        }
        if (user.getUserRole() == UserRole.STUDENT
                && comment.getAuthor().getUserId() == user.getUserId()) {
            return;
        }

        throw new AccessDeniedException(
                "User cannot modify this comment. Students may only modify their own comments.");
    }

    public void requireCourseManagementAccess(User user, int courseId) {

        if (user.getUserRole() != UserRole.PROFESSOR && user.getUserRole() != UserRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only professors or admins can modify courses");
        }

        requireCourseAccess(
                user.getUserId(),
                courseId);
    }

}
