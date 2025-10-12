package com.example.logindemo.media;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 업로드 소유자(선택) */
    private Long ownerUserId;   // member와 users 혼재 중이면 null 가능

    /**
     * 저장소 구분:
     * - "local"  : 서버 로컬(uploads 폴더, WebMvcConfig에서 /uploads/** 매핑)
     * - 그 외    : S3 버킷명 등
     */
    private String bucket;

    /**
     * 저장 경로:
     * - local : "/uploads/2025/10/04/abc.png" 또는 "2025/10/04/abc.png" 등
     * - S3    : "folder/abc.png" 또는 완전한 URL("https://...")일 수도 있음
     */
    private String path;

    /** MIME 타입: "image/jpeg", "video/mp4" 등 */
    private String mime;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('profile','cert','post','other')")
    private Scope scope = Scope.post;

    public enum Scope { profile, cert, post, other }

    /* ========================= 편의 게터 ========================= */

    /**
     * 상세 페이지 렌더링에 사용할 URL을 반환.
     * - bucket 이 "local" 이면 /uploads/** 경로로 정규화
     * - path 가 완전한 URL(https?://)이면 그대로 반환
     * - 그 외(bucket != local)는 S3 표준 퍼블릭 URL 형식으로 조합
     */
    @Transient
    public String getUrl() {
        if (path == null || path.isBlank()) return null;

        // 이미 절대 URL이면 그대로 반환
        String p = path.trim();
        if (startsWithHttp(p)) return p;

        // 로컬 저장소: /uploads/** 로 정규화
        if (isLocalBucket(bucket)) {
            // 이미 "/uploads/..." 형태면 그대로
            if (p.startsWith("/uploads/")) return p;
            // "uploads/..." 형태면 앞에 "/"만 보정
            if (p.startsWith("uploads/")) return "/" + p;
            // 그 외 상대경로면 "/uploads/" 접두
            return "/uploads/" + trimLeadingSlashes(p);
        }

        // 외부(S3 등) 저장소: 버킷이 있으면 표준 S3 URL로 조합
        String normalized = trimLeadingSlashes(p);
        if (bucket != null && !bucket.isBlank()) {
            // 기본 리전 도메인 사용(필요시 프로젝트 설정으로 교체 가능)
            return "https://" + bucket + ".s3.amazonaws.com/" + normalized;
        }

        // 버킷 정보가 없고 절대 URL도 아니면 최후의 보정(로컬처럼 취급)
        return "/uploads/" + normalized;
    }

    /** 컨텐트 타입(MIME)을 그대로 돌려줌. 없으면 null */
    @Transient
    public String getContentType() {
        return (mime == null || mime.isBlank()) ? null : mime;
    }

    /** image/* 여부 */
    @Transient
    public boolean isImage() {
        return mime != null && mime.toLowerCase().startsWith("image");
    }

    /** video/* 여부 */
    @Transient
    public boolean isVideo() {
        return mime != null && mime.toLowerCase().startsWith("video");
    }

    /* ========================= 내부 유틸 ========================= */

    private static boolean isLocalBucket(String b) {
        return b == null || b.isBlank() || "local".equalsIgnoreCase(b);
    }

    private static boolean startsWithHttp(String s) {
        String lower = s.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String trimLeadingSlashes(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == '/') i++;
        return s.substring(i);
    }
}
