package com.example.logindemo.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerCreateRequest {
    private String title;        // 필수
    private String description;  // 필수
    private String category;     // 필수(환경/돌봄/교육/기타 등)
    private String address;      // 선택(좌표만 있어도 됨)
    private Double lat;          // 선택 (지도 선택 시)
    private Double lng;          // 선택
    private Integer capacity;    // 선택(null이면 무제한)

    // ✅ 후기 글 여부 (기본 false)
    private Boolean review = false;

    // ✅ null-safe getter
    public boolean isReview() {
        return Boolean.TRUE.equals(review);
    }

    // 예: 2025-10-06T20:00
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;
}
