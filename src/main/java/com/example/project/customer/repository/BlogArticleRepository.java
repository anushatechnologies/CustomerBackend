package com.example.project.customer.repository;

import com.example.project.customer.entity.BlogArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogArticleRepository extends JpaRepository<BlogArticle, Integer> {
    Optional<BlogArticle> findBySlugIgnoreCase(String slug);

    @Query("SELECT b FROM BlogArticle b WHERE b.published = true " +
           "AND (:category IS NULL OR LOWER(b.category) = LOWER(:category)) " +
           "AND (:tag IS NULL OR LOWER(b.tags) LIKE LOWER(CONCAT('%', :tag, '%'))) " +
           "ORDER BY b.publishedAt DESC")
    Page<BlogArticle> findPublishedArticles(
            @Param("category") String category,
            @Param("tag") String tag,
            Pageable pageable
    );
}
