package com.example.logindemo.volunteer.dto;

import java.time.LocalDateTime;

public record VolunteerDetailResponse(
        Long id,
        String title,
        String description,
        String address,
        double lat,
        double lng,
        String category,
        String status,
        Integer capacity,
        Long appliedCount,
        LocalDateTime startTime,
        boolean applied,           //(현재 로그인 사용자가 지원했는지)
        Host host
) {
    public record Host(Long id, String nickname, String profileImageUrl) {}
}
