package com.example.logindemo.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    long countByPostId(Long postId);
    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);
}

