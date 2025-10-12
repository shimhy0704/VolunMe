package com.example.logindemo.volunteer.controller;

import com.example.logindemo.member.Member;
import com.example.logindemo.volunteer.service.VolunteerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController // ✅ JSON 반환
@RequiredArgsConstructor
@RequestMapping("/api/recruits") // ✅ API 전용 prefix
public class RecruitApplicantsApiController {

    private final VolunteerService service;

    // 지원자 목록 JSON
    @GetMapping(value = "/{id}/applicants", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> applicants(@PathVariable("id") Long recruitId,
                                                                HttpSession session) {
        Long viewerId = null;
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me != null) viewerId = me.getId();

        List<Map<String, Object>> list = service.getApplicants(recruitId, viewerId);
        return ResponseEntity.ok(list);
    }
}
