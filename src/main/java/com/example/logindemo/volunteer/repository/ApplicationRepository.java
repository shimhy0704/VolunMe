package com.example.logindemo.volunteer.repository;

import com.example.logindemo.volunteer.entity.Application;
import com.example.logindemo.volunteer.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // 활성(= 승인) 인원 수
    @Query("""
           SELECT COUNT(a)
             FROM Application a
            WHERE a.postId = :postId
              AND a.status = com.example.logindemo.volunteer.entity.ApplicationStatus.approved
           """)
    long countActiveByPostId(@Param("postId") Long postId);

    // 중복 지원 체크
    Optional<Application> findByPostIdAndApplicantUserId(Long postId, Long applicantUserId);

    // 한 모집글의 전체 지원 내역
    List<Application> findByPostId(Long postId);

    int countByPostId(Long postId);
}
