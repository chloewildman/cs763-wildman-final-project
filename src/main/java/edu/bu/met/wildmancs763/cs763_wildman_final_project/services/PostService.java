package edu.bu.met.wildmancs763.cs763_wildman_final_project.services;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Post;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Project;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.PostRepository;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.ProjectRepository;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final AuthorizationService authorizationService;
    private final ProjectRepository projectRepository;

    public PostService(PostRepository postRepository, AuthorizationService authorizationService, ProjectRepository projectRepository) {
        this.postRepository = postRepository;
        this.authorizationService = authorizationService;
        this.projectRepository = projectRepository;
    }

    public Post getPost(int postId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();

        int courseId = post.getProject().getCourse().getCourseId();

        authorizationService.requireCourseAccess(user.getUserId(), courseId);

        return post;
    }
    public List<Post> getPosts(int projectId, User user) {
        int courseId = projectRepository.findById(projectId).orElseThrow().getCourse().getCourseId();

        authorizationService.requireCourseAccess(user.getUserId(), courseId);

        return postRepository.findByProjectIdAndUserId(projectId, user.getUserId());
    }

    public Post createPost(int projectId, Post post, User user) {

        Project project = projectRepository.findById(projectId).orElseThrow();

        authorizationService.requireCourseAccess(user.getUserId(), project.getCourse().getCourseId());

        post.setAuthor(user);
        post.setProject(project);
        post.setPostCreationTimestamp(LocalDateTime.now());

        return postRepository.save(post);
    }

    public Post updatePost(int postId, User user, Post updatedPost) {

        Post post = postRepository.findById(postId).orElseThrow();

        int courseId = post.getProject().getCourse().getCourseId();

        authorizationService.requireCourseAccess(user.getUserId(), courseId);

        authorizationService.requirePostOwnership(user, post);

        post.setPostTitle(updatedPost.getPostTitle());
        post.setPostBody(updatedPost.getPostBody());

        return postRepository.save(post);
    }

    public void deletePost(int postId, User user) {

        Post post = postRepository.findById(postId).orElseThrow();

        int courseId = post.getProject().getCourse().getCourseId();

        authorizationService.requireCourseAccess(user.getUserId(), courseId);

        authorizationService.requirePostOwnership(user, post);

        postRepository.delete(post);
    }
}
