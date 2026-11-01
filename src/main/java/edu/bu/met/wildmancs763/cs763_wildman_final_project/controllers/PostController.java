package edu.bu.met.wildmancs763.cs763_wildman_final_project.controllers;

import org.springframework.web.bind.annotation.RestController;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Post;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.PostService;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.UserService;

@RestController
@RequestMapping("/courses/{courseId}/projects/{projectId}/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping
    public List<Post> getPosts(@PathVariable int projectId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return postService.getPosts(projectId, user);
    }

    @GetMapping("/{postId}")
    public Post getPost(@PathVariable int postId, @RequestParam int userId) {

        User user = userService.getUser(userId);

        return postService.getPost(postId, user);
    }

    @PostMapping
    public Post createPost(@PathVariable int projectId, @RequestBody Post post, @RequestParam int userId) {

        User user = userService.getUser(userId);

        return postService.createPost(projectId, post, user);
    }

    @PutMapping("/{postId}")
    public Post updatePost(@PathVariable int postId, @RequestBody Post post, @RequestParam int userId) {

        User user = userService.getUser(userId);

        return postService.updatePost(postId, user, post);
    }

    @DeleteMapping("/{postId}")
    public void deletePost(@PathVariable int postId, @RequestParam int userId) {

        User user = userService.getUser(userId);

        postService.deletePost(postId, user);
    }
}