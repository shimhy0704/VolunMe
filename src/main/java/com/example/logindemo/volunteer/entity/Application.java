package com.example.logindemo.volunteer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "recruit_application",
        indexes = {
                @Index(name = "ix_app_recruit", columnList = "recruit_id"),
                @Index(name = "ux_app_recruit_user", columnList = "recruit_id, applicant_user_id", unique = true)
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 모집글에 대한 지원인지
    @Column(name = "recruit_id", nullable = false)
    private Long postId;

    // 지원자 사용자 ID
    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    @Column(name = "post_id", nullable = false)
    private Long legacyPostId;

    @Column(name = "user_id", nullable = false)
    private Long legacyUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApplicationStatus status; // approved / canceled

    @Column(nullable = false)
    private boolean attended; // 출석 여부

    @Column(nullable = false)
    private boolean rewarded; // 보상 지급 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
