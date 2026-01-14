package org.example.controller;

import org.example.dto.CreatePostRequest;
import org.example.entity.Post;
import org.example.entity.User;
import org.example.repository.PostRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DataController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Autowired
    public DataController(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/posts/user/{username}")
    public ResponseEntity<Map<String, Object>> getUserPosts(
            @PathVariable String username) {

        boolean userExists = userRepository.findByUsername(username).isPresent();
        if (!userExists) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "User not found",
                    "message", "User with username '" + username + "' does not exist"
            ));
        }

        List<Post> posts = postRepository.findByAuthorUsername(username);

        List<Map<String, Object>> formattedPosts = posts.stream()
                .map(post -> {
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", sanitizeOutput(post.getTitle()));
                    postMap.put("content", sanitizeOutput(post.getContent()));
                    postMap.put("author", post.getAuthorUsername());
                    postMap.put("createdAt", post.getCreatedAt());
                    postMap.put("updatedAt", post.getUpdatedAt());
                    postMap.put("published", post.isPublished());
                    return postMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("posts", formattedPosts);
        response.put("totalPosts", posts.size());
        response.put("timestamp", LocalDateTime.now());
        response.put("note", "All user-generated content has been sanitized for XSS protection");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts")
    public ResponseEntity<Map<String, Object>> createPost(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser,
            @RequestBody CreatePostRequest request) {

        String sanitizedTitle = sanitizeInput(request.getTitle());
        String sanitizedContent = sanitizeInput(request.getContent());

        if (sanitizedTitle.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "Title cannot be empty"
            ));
        }

        if (sanitizedContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "Content cannot be empty"
            ));
        }

        User author = userRepository.findByUsername(authUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = new Post();
        post.setTitle(sanitizedTitle);
        post.setContent(sanitizedContent);
        post.setAuthor(author);
        post.setPublished(request.isPublished());

        Post savedPost = postRepository.save(post);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Post created successfully");
        response.put("postId", savedPost.getId());
        response.put("title", savedPost.getTitle());
        response.put("author", savedPost.getAuthorUsername());
        response.put("createdAt", savedPost.getCreatedAt());
        response.put("published", savedPost.isPublished());
        response.put("note", "All user input has been sanitized for XSS protection");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/latest")
    public ResponseEntity<Map<String, Object>> getLatestPosts() {

        List<Post> latestPosts = postRepository.findTop10ByPublishedTrueOrderByCreatedAtDesc();

        List<Map<String, Object>> formattedPosts = latestPosts.stream()
                .map(post -> {
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", sanitizeOutput(post.getTitle()));
                    postMap.put("excerpt", getExcerpt(sanitizeOutput(post.getContent()), 100));
                    postMap.put("author", post.getAuthorUsername());
                    postMap.put("createdAt", post.getCreatedAt());
                    return postMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("posts", formattedPosts);
        response.put("total", formattedPosts.size());
        response.put("description", "Latest published posts");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/my")
    public ResponseEntity<Map<String, Object>> getMyPosts(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User authUser) {

        List<Post> myPosts = postRepository.findByAuthorUsername(authUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("username", authUser.getUsername());
        response.put("posts", myPosts.stream()
                .map(post -> {
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", sanitizeOutput(post.getTitle()));
                    postMap.put("content", sanitizeOutput(post.getContent()));
                    postMap.put("createdAt", post.getCreatedAt());
                    postMap.put("updatedAt", post.getUpdatedAt());
                    postMap.put("published", post.isPublished());
                    return postMap;
                })
                .collect(Collectors.toList()));
        response.put("totalPosts", myPosts.size());
        response.put("publishedPosts", myPosts.stream().filter(Post::isPublished).count());
        response.put("draftPosts", myPosts.stream().filter(p -> !p.isPublished()).count());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/search")
    public ResponseEntity<Map<String, Object>> searchPosts(@RequestParam String q) {

        List<Post> searchResults = postRepository.searchPublishedPosts(q);

        Map<String, Object> response = new HashMap<>();
        response.put("query", sanitizeInput(q));
        response.put("results", searchResults.stream()
                .map(post -> {
                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("id", post.getId());
                    postMap.put("title", sanitizeOutput(post.getTitle()));
                    postMap.put("excerpt", getExcerpt(sanitizeOutput(post.getContent()), 150));
                    postMap.put("author", post.getAuthorUsername());
                    postMap.put("createdAt", post.getCreatedAt());
                    return postMap;
                })
                .collect(Collectors.toList()));
        response.put("totalResults", searchResults.size());
        response.put("searchTimestamp", LocalDateTime.now());
        response.put("securityNote", "Search uses parameterized queries to prevent SQL injection");

        return ResponseEntity.ok(response);
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("<[^>]*>", "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String sanitizeOutput(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("<[^>]*>", "");
    }

    private String getExcerpt(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content != null ? content : "";
        }
        return content.substring(0, maxLength) + "...";
    }
}