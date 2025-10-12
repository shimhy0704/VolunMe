package com.example.logindemo.volunteer.bridge;

import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import com.example.logindemo.media.LocalStorageService;
import com.example.logindemo.media.Media;
import com.example.logindemo.media.MediaRepository;
import com.example.logindemo.post.Post;
import com.example.logindemo.post.PostMedia;
import com.example.logindemo.post.PostRepository;
import com.example.logindemo.post.PostService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostBridge {

    @PersistenceContext
    private EntityManager em;

    private final PostService postService;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    private final LocalStorageService localStorageService;
    private final MediaRepository mediaRepository;

    /**
     * 후기: 일반 Post 생성 + 첨부파일(PostMedia) 연결
     */
    public Long createReviewPost(Long authorUserId,
                                 String title,
                                 String body,
                                 MultipartFile[] files) {

        Member author = memberRepository.findById(authorUserId).orElseThrow();

        String safeTitle = (title == null || title.isBlank()) ? "제목 없음" : title;
        String safeBody  = (body  == null) ? "" : body;

        // 1) 게시글 생성 (후기)
        Post post = postService.createPost("[후기] " + safeTitle, safeBody, author);
        postRepository.save(post);

        // 2) 첨부 저장 및 연결
        String firstImageUrl = null;
        int sort = 0;

        if (files != null) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;

                // 2-1) 로컬 저장 + Media 엔티티 생성 (path/mime/ownerUserId 등 세팅됨)
                Media media = localStorageService.saveForPost(f, authorUserId);
                media = mediaRepository.save(media);

                // 2-2) Post ↔ Media 연결
                PostMedia pm = new PostMedia();
                pm.setPost(post);
                pm.setMedia(media);
                pm.setSortOrder(sort++);
                em.persist(pm);

                // 2-3) 첫 이미지면 썸네일 후보 저장
                if (firstImageUrl == null) {
                    String mime = media.getMime(); // LocalStorageService가 세팅
                    if (mime != null && mime.startsWith("image")) {
                        firstImageUrl = media.getPath(); // /uploads/post/xxx.ext
                    }
                }
            }
        }

        // 3) (선택) 썸네일 필드가 있는 경우만 세팅 시도
        if (firstImageUrl != null) {
            try {
                post.getClass().getMethod("setThumbnailUrl", String.class).invoke(post, firstImageUrl);
                postRepository.save(post);
            } catch (Exception ignore) {
                // Post에 thumbnailUrl이 없으면 무시
            }
        }

        return post.getId();
    }

    /**
     * 모집 공지용 게시글 생성 (recruitId 연결)
     */
    public Long createRecruitAnnouncement(Long authorId,
                                          Long recruitId,
                                          String title,
                                          String description,
                                          String address) {

        Member author = memberRepository.findById(authorId).orElseThrow();

        String safeDesc = (description == null) ? "" : description;

        Post post = postService.createPost(
                "[모집] " + ((title == null || title.isBlank()) ? "제목 없음" : title),
                safeDesc,            // 본문에는 설명만
                author
        );

        post.setRecruitId(recruitId); // Post 엔티티에 recruitId 존재해야 함
        postRepository.save(post);

        return post.getId();
    }
}
