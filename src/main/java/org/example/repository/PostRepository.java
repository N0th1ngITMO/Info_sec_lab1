package org.example.repository;

import org.example.entity.Post;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.author.username = :username AND p.published = true ORDER BY p.createdAt DESC")
    List<Post> findByAuthorUsername(@Param("username") String username);

    List<Post> findByAuthorOrderByCreatedAtDesc(User author);

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.author.username = :username")
    Optional<Post> findByIdAndAuthorUsername(@Param("id") Long id, @Param("username") String username);

    @Query("SELECT p FROM Post p WHERE p.published = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY p.createdAt DESC")
    List<Post> searchPublishedPosts(@Param("query") String query);

    List<Post> findTop10ByPublishedTrueOrderByCreatedAtDesc();
}
