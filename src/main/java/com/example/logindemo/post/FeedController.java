package com.example.logindemo.post;

import com.example.logindemo.media.LocalStorageService;
import com.example.logindemo.media.Media;
import com.example.logindemo.media.MediaRepository;
import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import com.example.logindemo.volunteer.dto.MarkerBriefResponse;
import com.example.logindemo.volunteer.service.VolunteerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class FeedController {

    @PersistenceContext
    private EntityManager em;                        // ★ PostMedia 저장을 EM으로 처리

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final LocalStorageService storageService;
    private final MediaRepository mediaRepository;   // 파일 메타 저장
    private final PostCommentRepository postCommentRepository;
    private final VolunteerService volunteerService;
    private final MemberRepository memberRepository;

    // 피드 화면
    @GetMapping("/posts")
    public String feed(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       HttpSession session, Model model) {
        Page<Post> slice = postRepository.findFeed(PageRequest.of(page, size));
        Member me = (Member) session.getAttribute("loggedInMember");
        Long myId = (me == null) ? null : Long.valueOf(me.getId());

        Map<Long, Boolean> liked = new HashMap<>();
        if (myId != null) {
            for (Post p : slice.getContent()) {
                liked.put(p.getId(), postRepository.countLikeByMember(p.getId(), myId) > 0);
            }
        }

        // ★ 여기 이름을 'slice'로 넣습니다.
        model.addAttribute("slice", slice);
        model.addAttribute("liked", liked);
        model.addAttribute("me", me);

        // ★ 미디어 맵도 같이 넣는 코드가 반드시 있어야 합니다.
        List<Long> ids = slice.getContent().stream().map(Post::getId).toList();
        Map<Long, List<PostMedia>> mediaMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<PostMedia> all = em.createQuery(
                    "select pm from PostMedia pm join fetch pm.media " +
                            "where pm.post.id in :ids order by pm.post.id, pm.sortOrder",
                    PostMedia.class
            ).setParameter("ids", ids).getResultList();

            for (PostMedia pm : all) {
                mediaMap.computeIfAbsent(pm.getPost().getId(), k -> new ArrayList<>()).add(pm);
            }
        }
        model.addAttribute("mediaMap", mediaMap);

        Map<Long, String> avatarMap = buildAvatarMap(slice.getContent());
        model.addAttribute("avatarMap", avatarMap);

        return "posts/feed";
    }

    // 글 작성 폼
    @GetMapping("/posts/new")
    public String newPostForm() {
        return "posts/new";
    }

    // 글 작성 처리 (텍스트 + 파일 여러 개)
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createPost(@RequestParam String title,
                             @RequestParam String content,
                             @RequestParam(value = "files", required = false) List<MultipartFile> files,
                             HttpSession session) {

        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) return "redirect:/OAuth2/kakao/login";

        Post p = new Post();
        p.setTitle(title);
        p.setContent(content);
        p.setMember(me);
        Post saved = postRepository.save(p);

        Long ownerUserId = (me == null) ? null : Long.valueOf(me.getId());
        String firstImageUrl = null;

        if (files != null) {
            int order = 0;
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;

                // 1) 디스크 저장 + Media 엔티티 생성
                Media m = storageService.saveForPost(f, ownerUserId);
                mediaRepository.save(m);

                // 2) Post ↔ Media 연결 (PostMedia) → EntityManager로 저장
                PostMedia pm = new PostMedia();
                pm.setPost(saved);
                pm.setMedia(m);
                pm.setSortOrder(order++);
                em.persist(pm);  // ★ Repository 없이 저장

                // 3) 첫 이미지면 썸네일 후보 기억
                if (firstImageUrl == null) {
                    try {
                        String mime = m.getMime();    // Media 필드명에 맞춰 사용 (mime/path)
                        if (mime != null && mime.startsWith("image")) {
                            firstImageUrl = m.getPath();
                        }
                    } catch (Exception ignore) {}
                }
            }
        }

        // (선택) Post에 썸네일 필드가 있다면 세팅
        try {
            if (firstImageUrl != null) {
                saved.getClass().getMethod("setThumbnailUrl", String.class).invoke(saved, firstImageUrl);
                postRepository.save(saved);
            }
        } catch (Exception ignore) { /* 썸네일 필드 없으면 무시 */ }

        return "redirect:/posts";
    }

    private Map<Long, String> buildAvatarMap(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return Collections.emptyMap();

        Map<Long, String> map = new HashMap<>();
        for (Post p : posts) {
            Member author = p.getMember(); // Post#getMember() == author
            if (author == null) continue;
            Long authorId = author.getId();
            if (authorId == null) continue;

            String url = resolveProfileUrl(author);
            map.put(authorId, url);
        }
        return map;
    }

    private String resolveProfileUrl(Member user) {
        // 1) users.email 기반
        try {
            String email = user.getUsername();
            Optional<String> byEmail = memberRepository.findUsersProfileImageUrlByEmail(email);
            if (byEmail.isPresent() && hasText(byEmail.get())) {
                return normalizeUrl(byEmail.get());
            }
        } catch (Exception ignored) {}

        // 2) users.member_id 기반
        try {
            Optional<String> byMemberId = memberRepository.findUsersProfileImageUrlByMemberId(user.getId());
            if (byMemberId.isPresent() && hasText(byMemberId.get())) {
                return normalizeUrl(byMemberId.get());
            }
        } catch (Exception ignored) {}

        // 3) media 최신 프로필(있으면)
        try {
            Optional<String> fromMedia = memberRepository.findLatestProfileMediaUrlByMemberId(user.getId());
            if (fromMedia.isPresent() && hasText(fromMedia.get())) {
                return normalizeUrl(fromMedia.get());
            }
        } catch (Exception ignored) {}

        // 기본 아바타
        return "/img/bearhead.png";
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // http → https 정규화 (혼합콘텐츠 방지)
    private String normalizeUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("http://")) {
            return "https://" + s.substring("http://".length());
        }
        return s;
    }

    // 좋아요 토글 API (AJAX)
    @PostMapping("/api/posts/{postId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long postId, HttpSession session) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<PostLike> existing = postLikeRepository.findByPostIdAndMemberId(postId, me.getId());
        boolean liked;
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            liked = false;
        } else {
            PostLike pl = new PostLike();
            pl.setPost(postRepository.getReferenceById(postId));
            pl.setMember(me);
            postLikeRepository.save(pl);
            liked = true;
        }
        long count = postLikeRepository.countByPostId(postId);
        Map<String, Object> body = Map.of("liked", liked, "count", count);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/posts/{id}")
    public String deprecatedDetailRedirect(@PathVariable Long id) {
        return "redirect:/map";
    }

    @GetMapping(value = "/posts/brief", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> legacyBrief(@RequestParam double minLat,
                                           @RequestParam double minLng,
                                           @RequestParam double maxLat,
                                           @RequestParam double maxLng,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String status) {
        List<MarkerBriefResponse> markers =
                volunteerService.getMarkers(minLat, minLng, maxLat, maxLng, q, category, status);
        return Map.of("markers", markers);
    }

    @GetMapping(value = "/posts/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String legacyPostDetailHtml(@PathVariable Long id) {
        return "redirect:/map";
    }
}
