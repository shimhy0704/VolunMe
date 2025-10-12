package com.example.logindemo.volunteer.controller;

import com.example.logindemo.member.Member;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recruits") // 🔹 뷰(HTML) 전용 prefix
public class RecruitAppsViewController {

    // 지원자 목록 페이지 (templates/recruit_apps.html 렌더)
    @GetMapping("/{id}/applicants")
    public String appsView(@PathVariable("id") Long id,
                           HttpSession session,
                           Model model) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) return "redirect:/login";

        model.addAttribute("recruitId", id);  // 🔸 템플릿 JS가 읽는 키
        return "recruit_apps";                // templates/recruit_apps.html
    }
}
