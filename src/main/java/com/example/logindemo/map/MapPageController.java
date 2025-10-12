package com.example.logindemo.map;

import com.example.logindemo.member.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MapPageController {

    /**
     * 카카오 지도 페이지
     * 예: /map
     * 예: /map?pick=1   (핀 좌표 선택 모드)
     * 예: /map?focus=45 (특정 모집글 상세 포커스)
     */
    @GetMapping("/map")
    public String mapPage(
            @RequestParam(name = "pick", required = false) Integer pickMode,
            @RequestParam(name = "focus", required = false) Long focusId,
            HttpSession session,
            Model model
    ) {
        // 로그인 사용자(있으면) 전달
        Member me = (Member) session.getAttribute("loggedInMember");
        model.addAttribute("meId", (me != null) ? me.getId() : null);

        // 프론트 스크립트에서 읽을 파라미터 전달
        model.addAttribute("pickMode", pickMode != null && pickMode == 1);
        model.addAttribute("focusId", focusId);

        // Thymeleaf 템플릿 이름 (classpath:/templates/map.html)
        return "map";
    }

    @GetMapping("/recruits/new")
    public String recruitNew(HttpSession session, Model model) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) return "redirect:/login";   // 세션 로그인 없으면 로그인으로
        model.addAttribute("meId", me.getId());      // 템플릿에서 쓰려면 전달
        return "volunteer_form";                       // templates/recruits/new.html 렌더
    }
}
