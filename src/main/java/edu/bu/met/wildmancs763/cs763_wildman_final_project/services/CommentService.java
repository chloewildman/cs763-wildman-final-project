package edu.bu.met.wildmancs763.cs763_wildman_final_project.services;

import java.util.List;
import org.springframework.stereotype.Service;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Comment;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Post;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Project;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.CommentRepository;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.ProjectRepository;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.PostRepository;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final AuthorizationService authorizationService;
    private final ProjectRepository projectRepository;
    private final PostRepository postRepository;

    CommentService(CommentRepository commentRepository, AuthorizationService authorizationService,
            ProjectRepository projectRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.authorizationService = authorizationService;
        this.projectRepository = projectRepository;
        this.postRepository = postRepository;
    }

    public Comment getComment(int commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        int courseId = comment.getPost()
                .getProject()
                .getCourse()
                .getCourseId();

        authorizationService.requireCourseAccess(
                user.getUserId(),
                courseId);

        return comment;
    }

    public List<Comment> getComments(int postId, User user) {
        return commentRepository.findByPostIdAndUser(postId, user);
    }

    public Comment createComment(int projectId, int postId, Comment comment, User user) {

        Project project = projectRepository.findById(projectId).orElseThrow();

        authorizationService.requireCourseAccess(user.getUserId(), project.getCourse().getCourseId());

        Post post = postRepository.findById(postId).orElseThrow();

        comment.setAuthor(user);
        comment.setPost(post);

        return commentRepository.save(comment);
    }

    public Comment updateComment(int commentId, User user, Comment updatedComment) {

        Comment comment = commentRepository.findById(commentId).orElseThrow();

        int courseId = comment.getPost().getProject().getCourse().getCourseId();

        authorizationService.requireCourseAccess(user.getUserId(), courseId);

        authorizationService.requireCommentOwnership(user, comment);

        comment.setCommentBody(updatedComment.getCommentBody());

        return commentRepository.save(comment);
    }

    public void deleteComment(int commentId, User user) {

        Comment comment = commentRepository.findById(commentId).orElseThrow();

        int courseId = comment.getPost().getProject().getCourse().getCourseId();

        authorizationService.requireCourseAccess(user.getUserId(), courseId);

        authorizationService.requireCommentOwnership(user, comment);

        commentRepository.delete(comment);
    }
}
