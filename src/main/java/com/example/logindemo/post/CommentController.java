package com.example.logindemo.post;

import com.example.logindemo.member.Member;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;

    // 목록 (최상위 댓글 페이징)
    @GetMapping
    public Map<String,Object> list(@PathVariable Long postId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        var top = commentRepository.findByPostIdAndParentIsNullOrderByIdDesc(
                postId, PageRequest.of(page, size)
        );

        List<Map<String, Object>> items = new ArrayList<>();
        for (PostComment c : top.getContent()) {
            // ★ createdAt 오름차순으로 대댓글 정렬 (Repository 시그니처와 일치)
            var replies = commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId());

            items.add(Map.of(
                    "id", c.getId(),
                    "memberName", c.getMember().getDisplayName(),
                    "content", c.getContent(),
                    "createdAt", c.getCreatedAt(),
                    "replies", replies.stream().map(r -> Map.of(
                            "id", r.getId(),
                            "memberName", r.getMember().getDisplayName(),
                            "content", r.getContent(),
                            "createdAt", r.getCreatedAt()
                    )).toList()
            ));
        }

        return Map.of(
                "content", items,
                "page", top.getNumber(),
                "totalPages", top.getTotalPages(),
                "totalElements", top.getTotalElements()
        );
    }

    // 작성 (댓글 또는 대댓글)
    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long postId,
                                    @RequestParam String content,
                                    @RequestParam(required = false) Long parentId,
                                    HttpSession session) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        PostComment c = new PostComment();
        c.setPost(postRepository.getReferenceById(postId));
        c.setMember(me);
        c.setContent(content);

        if (parentId != null) {
            PostComment parent = commentRepository.findById(parentId)
                    .orElseThrow();
            c.setParent(parent);
        }

        PostComment saved = commentRepository.save(c);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "memberName", me.getDisplayName(),
                "content", saved.getContent(),
                "createdAt", saved.getCreatedAt(),
                "parentId", parentId
        ));
    }

    // 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> delete(@PathVariable Long postId,
                                    @PathVariable Long commentId,
                                    HttpSession session) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        PostComment c = commentRepository.findById(commentId).orElse(null);
        if (c == null || !Objects.equals(c.getMember().getId(), me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        commentRepository.delete(c);
        return ResponseEntity.noContent().build();
    }


}
