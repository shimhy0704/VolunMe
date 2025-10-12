package com.example.logindemo.volunteer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor      // ← VolunteerService에서 new ApplicantResponse(...) 생성 시 충돌 방지용은 아니지만 관례로 추가
@AllArgsConstructor
@Builder// ← (id, userId, name, status, attended, rewarded) 생성자 필요
public class ApplicantResponse {
    private Long id;          // application id
    private Long userId;      // 지원자 회원 id
    private String name;      // 지원자 표시명
    private String status;    // approved / canceled ...
    private boolean attended; // 출석 체크 여부
    private boolean rewarded; // 보상 지급 여부
}
