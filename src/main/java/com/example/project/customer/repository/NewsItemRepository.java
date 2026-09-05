package com.example.project.customer.repository;

import com.example.project.customer.entity.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, Integer> {
    List<NewsItem> findAllByOrderByPublishedAtDesc();
    Page<NewsItem> findAllByOrderByPublishedAtDesc(Pageable pageable);
    List<NewsItem> findByCategoryOrderByPublishedAtDesc(String category);
}
