package com.example.logindemo.post;

import com.example.logindemo.member.Member;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/admin/posts") // ★ FeedController(/posts)와 충돌 방지: 관리 전용
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostRepository postRepository;

    /** 1) 게시글 목록 (관리자 목록 화면) */
    @GetMapping
    public String listPosts(Model model) {
        model.addAttribute("posts", postService.findAllPosts());
        return "posts/list"; // templates/posts/list.html
    }

    /** (선택) 닫힌 글 목록 등 별도 경로로 리다이렉트가 필요하면 /closed 사용 */
    @GetMapping("/closed")
    public String closedRedirect() {
        return "redirect:/map";
    }

    /** 2) 새 게시글 작성 폼 */
    @GetMapping("/new")
    public String newPostForm(HttpSession session) {
        Member loggedInMember = (Member) session.getAttribute("loggedInMember");
        if (loggedInMember == null) return "redirect:/login";
        return "posts/form"; // templates/posts/form.html (또는 posts/new.html)
    }

    /** 3) 새 게시글 저장 */
    @PostMapping
    public String createPost(@RequestParam String title,
                             @RequestParam String content,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Member loggedInMember = (Member) session.getAttribute("loggedInMember");
        if (loggedInMember == null) {
            return "redirect:/login";
        }

        postService.createPost(title, content, loggedInMember);
        redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 작성되었습니다.");
        return "redirect:/admin/posts";
    }

    /** (관리 보기) 게시글 단건 보기 — 관리자 템플릿이 있는 경우 유지 */
    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        model.addAttribute("post", post);
        return "posts/view"; // templates/posts/view.html (기존 detail 템플릿명과 맞춰주세요)
    }

    /** 4) 게시글 수정 폼 */
    @GetMapping("/{id}/edit")
    public String editPostForm(@PathVariable Long id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Member loggedInMember = (Member) session.getAttribute("loggedInMember");
        if (loggedInMember == null) {
            return "redirect:/login";
        }

        Optional<Post> postOptional = postService.findPostById(id);
        if (postOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "게시글을 찾을 수 없습니다.");
            return "redirect:/admin/posts";
        }

        Post post = postOptional.get();
        if (!Objects.equals(post.getAuthor().getId(), loggedInMember.getId())) {
            redirectAttributes.addFlashAttribute("error", "수정 권한이 없습니다.");
            return "redirect:/admin/posts";
        }

        model.addAttribute("post", post);
        return "posts/form"; // templates/posts/form.html
    }

    /** 5) 게시글 수정 처리 */
    @PostMapping("/{id}/update")
    public String updatePost(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam String content,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Member loggedInMember = (Member) session.getAttribute("loggedInMember");
        if (loggedInMember == null) {
            return "redirect:/login";
        }

        Optional<Post> updatedPost = postService.updatePost(id, title, content, loggedInMember);
        if (updatedPost.isPresent()) {
            redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 수정되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("error", "게시글 수정에 실패했습니다. (권한이 없거나 게시글이 존재하지 않습니다.)");
        }
        return "redirect:/admin/posts";
    }

    /** 6) 게시글 삭제 */
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Member loggedInMember = (Member) session.getAttribute("loggedInMember");
        if (loggedInMember == null) {
            return "redirect:/login";
        }

        boolean deleted = postService.deletePost(id, loggedInMember);
        if (deleted) {
            redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 삭제되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("error", "게시글 삭제에 실패했습니다. (권한이 없거나 게시글이 존재하지 않습니다.)");
        }
        return "redirect:/admin/posts";
    }

    // ===== 댓글 서비스 =====
    @Service
    @RequiredArgsConstructor
    public static class PostCommentService {
        private final PostRepository postRepository;
        private final PostCommentRepository commentRepository;

        @Transactional
        public PostComment addComment(Long postId, Member writer, String content, Long parentId) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("post not found"));
            PostComment c = new PostComment();
            c.setPost(post);
            c.setMember(writer);
            c.setContent(content);
            if (parentId != null) {
                PostComment parent = commentRepository.findById(parentId)
                        .orElseThrow(() -> new IllegalArgumentException("parent not found"));
                c.setParent(parent);
            }
            return commentRepository.save(c);
        }

        @Transactional(Transactional.TxType.SUPPORTS)
        public List<PostComment> topLevelComments(Long postId) {
            // ★ Repository 메서드 시그니처에 맞춤
            return commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId);
        }

        @Transactional(Transactional.TxType.SUPPORTS)
        public List<PostComment> replies(Long parentId) {
            return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
        }
    }
}
