// src/main/java/com/example/logindemo/post/PostCommentRepository.java
package com.example.logindemo.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// PostCommentRepository.java
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    @EntityGraph(attributePaths = {"member"})
    Page<PostComment> findByPostIdAndParentIsNullOrderByIdDesc(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"member"})
    List<PostComment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    @EntityGraph(attributePaths = {"member"})
    List<PostComment> findByPostIdAndParentIsNullOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);
}

