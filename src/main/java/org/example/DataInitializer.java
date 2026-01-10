package org.example;

import org.example.entity.Post;
import org.example.entity.User;
import org.example.repository.PostRepository;
import org.example.repository.UserRepository;
import org.example.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final SecurityService securityService;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           PostRepository postRepository,
                           SecurityService securityService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.securityService = securityService;
    }

    @Override
    public void run(String... args) {
        createUserIfNotExists("testuser", "test@example.com", "TestPassword123!");
        createUserIfNotExists("alice", "alice@example.com", "Password123!");
        createUserIfNotExists("bob", "bob@example.com", "Password123!");
        createSamplePosts();
    }

    private void createUserIfNotExists(String username, String email, String password) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(securityService.hashPassword(password));
            user.setEnabled(true);

            userRepository.save(user);
            System.out.println("Test user created: " + username + " / " + password);
        }
    }

    private void createSamplePosts() {
        if (postRepository.count() == 0) {
            System.out.println("Creating sample posts...");

            List<String> sampleTitles = Arrays.asList(
                    "Мой первый пост о Spring Security",
                    "Как защитить REST API от OWASP Top 10",
                    "JWT аутентификация в Spring Boot",
                    "Лучшие практики разработки безопасных приложений",
                    "Интеграция SQLite с Spring Data JPA"
            );

            List<String> sampleContents = Arrays.asList(
                    "В этом посте я расскажу о своем опыте настройки Spring Security...",
                    "Защита от SQL инъекций, XSS атак и Broken Authentication...",
                    "JWT (JSON Web Tokens) - отличный способ для stateless аутентификации...",
                    "Использование параметризованных запросов, хэширование паролей...",
                    "SQLite - легкая база данных, отлично подходит для демонстрационных проектов..."
            );

            List<User> users = userRepository.findAll();

            for (int i = 0; i < 15; i++) {
                Post post = new Post();
                post.setTitle(sampleTitles.get(i % sampleTitles.size()) + " #" + (i + 1));
                post.setContent(sampleContents.get(i % sampleContents.size()) +
                        " Это пост номер " + (i + 1) + " в нашей демонстрации.");
                post.setAuthor(users.get(i % users.size()));
                post.setPublished(true);
                post.setCreatedAt(LocalDateTime.now().minusDays(i));
                post.setUpdatedAt(LocalDateTime.now().minusDays(i));

                postRepository.save(post);
            }

            System.out.println("Created 15 sample posts");
        }
    }
}