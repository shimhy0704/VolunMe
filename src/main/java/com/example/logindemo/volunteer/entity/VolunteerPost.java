package com.example.logindemo.volunteer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "volunteer_posts", indexes = {
        @Index(name = "idx_vpost_lat_lng", columnList = "lat,lng")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false, length = 30)
    private String category;

    private String address;

    private Double lat;
    private Double lng;
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VolunteerStatus status = VolunteerStatus.open;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ✅ 시작 시간: 단일 @Column만 사용 (중복 금지)
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // 후기 글 여부 (기본 false). null도 false로 취급.
    @Column(name = "is_review")
    @Builder.Default
    private Boolean review = false;

    // 편의 게터 (null-safety)
    public boolean isReview() { return Boolean.TRUE.equals(review); }
}
