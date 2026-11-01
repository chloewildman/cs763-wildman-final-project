package edu.bu.met.wildmancs763.cs763_wildman_final_project.controllers;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Comment;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import java.util.List;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.CommentService;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.UserService;

@RestController
@RequestMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping
    public List<Comment> getComments(@PathVariable int postId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return commentService.getComments(postId, user);
    }

    @GetMapping("/{commentId}")
    public Comment getComment(@PathVariable int commentId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return commentService.getComment(commentId, user);
    }

    @PostMapping
    public Comment createComment(@PathVariable int projectId, @PathVariable int postId, @RequestBody Comment comment,
            @RequestParam int userId) {
        User user = userService.getUser(userId);
        return commentService.createComment(projectId, postId, comment, user);
    }

    @PutMapping("/{commentId}")
    public Comment updateComment(@PathVariable int commentId, @RequestBody Comment comment, @RequestParam int userId) {

        User user = userService.getUser(userId);

        return commentService.updateComment(commentId, user, comment);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable int commentId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        commentService.deleteComment(commentId, user);
    }
}