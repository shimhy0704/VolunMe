package com.example.logindemo.volunteer.dto;

import com.example.logindemo.volunteer.entity.VolunteerPost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkerBriefResponse {
    private Long id;
    private String title;
    private Double lat;
    private Double lng;
    private String status;   // ✅ Service에서 .status(...) 호출용
    private String category; // ✅ Service에서 .category(...) 호출용
    // 필요하면 주소도 추가 가능: private String address;

    public static MarkerBriefResponse from(VolunteerPost v) {
        if (v == null) return null;
        return MarkerBriefResponse.builder()
                .id(v.getId())
                .title(v.getTitle())
                .lat(v.getLat())
                .lng(v.getLng())
                .status(v.getStatus() != null ? v.getStatus().name() : null)
                .category(v.getCategory())
                // .address(v.getAddress())
                .build();
    }
}
