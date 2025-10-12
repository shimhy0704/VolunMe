package com.example.logindemo.post;

import com.example.logindemo.media.Media;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@NoArgsConstructor
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어느 게시글의 미디어인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 실제 파일/URL 정보를 가진 Media 엔티티 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    /** 같은 글 내 노출 순서 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /* ========= 템플릿/컨트롤러에서 쓰기 쉬운 편의 게터 ========= */

    /** 렌더링 URL: Media.getPath() 우선, 없으면 getUrl() 시도 */
    @Transient
    public String getUrl() {
        if (media == null) return null;
        try {
            // path가 있으면 우선 사용
            var m = media.getClass().getMethod("getPath").invoke(media);
            if (m != null) return (String) m;
        } catch (Exception ignore) {}
        try {
            var m = media.getClass().getMethod("getUrl").invoke(media);
            if (m != null) return (String) m;
        } catch (Exception ignore) {}
        return null;
    }

    /** contentType: Media.getMime() 우선, 없으면 getContentType() 시도 */
    @Transient
    public String getContentType() {
        if (media == null) return null;
        try {
            var m = media.getClass().getMethod("getMime").invoke(media);
            if (m != null) return (String) m;
        } catch (Exception ignore) {}
        try {
            var m = media.getClass().getMethod("getContentType").invoke(media);
            if (m != null) return (String) m;
        } catch (Exception ignore) {}
        return null;
    }
}
