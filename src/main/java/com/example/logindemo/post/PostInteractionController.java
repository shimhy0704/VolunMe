package com.example.logindemo.post;

import com.example.logindemo.member.Member;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class PostInteractionController {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;

    /* =========================
       기존: 폼/페이지 전용 엔드포인트(리다이렉트)
       ========================= */

    @PostMapping("/posts/{id}/like")
    public String toggleLike(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        postLikeRepository.findByPostIdAndMemberId(id, me.getId())
                .ifPresentOrElse(
                        postLikeRepository::delete,
                        () -> {
                            PostLike pl = new PostLike();
                            pl.setPost(post);
                            pl.setMember(me);
                            postLikeRepository.save(pl);
                        }
                );

        ra.addAttribute("id", id);
        return "redirect:/posts/{id}";
    }

    @PostMapping("/posts/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             HttpSession session,
                             RedirectAttributes ra) {

        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content required");
        }

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        PostComment c = new PostComment();
        c.setPost(post);
        c.setMember(me); // ← 필드명은 member
        c.setContent(content);
        postCommentRepository.save(c);

        ra.addAttribute("id", id);
        return "redirect:/posts/{id}";
    }

    @PostMapping("/posts/{id}/comments/{parentId}")
    public String replyComment(@PathVariable Long id,
                               @PathVariable Long parentId,
                               @RequestParam String content,
                               HttpSession session,
                               RedirectAttributes ra) {

        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content required");
        }

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PostComment parent = postCommentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent not found"));

        PostComment reply = new PostComment();
        reply.setPost(post);
        reply.setMember(me); // ← 필드명은 member
        reply.setParent(parent);
        reply.setContent(content);
        postCommentRepository.save(reply);

        ra.addAttribute("id", id);
        return "redirect:/posts/{id}";
    }

    /* =========================
       === [API] 피드용 AJAX(JSON) 엔드포인트 ===
       새로운 파일 만들지 않고 동일 컨트롤러에 추가
       ========================= */

    /** 상위/답글 등록 (parentId 유무로 분기) */
    @PostMapping(value = "/api/posts/{id}/comments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> addCommentApi(@PathVariable Long id,
                                                             @RequestParam String content,
                                                             @RequestParam(required = false) Long parentId,
                                                             HttpSession session) {
        Map<String, Object> body = new HashMap<>();

        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) {
            body.put("ok", false);
            body.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(body);
        }
        if (content == null || content.isBlank()) {
            body.put("ok", false);
            body.put("message", "내용을 입력해 주세요.");
            return ResponseEntity.status(400).body(body);
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            body.put("ok", false);
            body.put("message", "게시글을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(body);
        }

        PostComment parent = null;
        if (parentId != null) {
            parent = postCommentRepository.findById(parentId).orElse(null);
            if (parent == null) {
                body.put("ok", false);
                body.put("message", "원본 댓글을 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(body);
            }
        }

        PostComment c = new PostComment();
        c.setPost(post);
        c.setMember(me);
        c.setParent(parent);
        c.setContent(content);
        postCommentRepository.save(c);

        body.put("ok", true);
        body.put("id", c.getId());
        body.put("parentId", parent != null ? parent.getId() : null);
        body.put("message", "댓글이 등록되었습니다.");
        // 200으로 내려도 되지만 REST 표준으론 201이 적절
        return ResponseEntity.status(201).body(body);
    }

    /** 댓글 삭제 */
    @DeleteMapping(value = "/api/posts/{id}/comments/{commentId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteCommentApi(@PathVariable Long id,
                                              @PathVariable Long commentId,
                                              HttpSession session) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) {
            Map<String, Object> body = Map.of("ok", false, "message", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(body);
        }

        PostComment c = postCommentRepository.findById(commentId).orElse(null);
        if (c == null || c.getPost() == null || !Objects.equals(c.getPost().getId(), id)) {
            Map<String, Object> body = Map.of("ok", false, "message", "댓글을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(body);
        }

        // 권한: 작성자 본인만 삭제 (필요 시 보강)
        if (!Objects.equals(c.getMember().getId(), me.getId())) {
            Map<String, Object> body = Map.of("ok", false, "message", "삭제 권한이 없습니다.");
            return ResponseEntity.status(403).body(body);
        }

        postCommentRepository.delete(c);
        // 본문 없이 204
        return ResponseEntity.noContent().build();
    }
}
