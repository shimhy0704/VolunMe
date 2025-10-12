package com.example.logindemo.member;

import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberRepository memberRepository;
    //추가
    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }
    @PostMapping("/member")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String displayName,
                           Model model) {
        if (memberRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "register";
        }

        Member member = new Member();
        member.setUsername(username);
        member.setPassword(password);
        member.setDisplayName(displayName);
        memberRepository.save(member);
        return "redirect:/login";
    }
    //추가
    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 틀렸습니다.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        var memberOptional = memberRepository.findByUsername(username);
        if (memberOptional.isPresent()) {
            var member = memberOptional.get();
            if (member.getPassword().equals(password)) {
                session.setAttribute("loggedInMember",member);
                model.addAttribute("member", member);
                return "welcome";
            }
        }

        model.addAttribute("error", "아이디 또는 비밀번호가 틀렸습니다.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }
}

