package com.example.logindemo.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * 로컬 디스크에 파일을 저장하고, WebMvcConfig 의 /uploads/** 매핑을 통해
 * 브라우저에서 접근 가능한 public URL 을 제공한다.
 *
 * 기본 루트 디렉터리: app.upload.local-dir (기본값: "uploads")
 * public URL prefix: "/uploads"
 */
@Service
public class LocalStorageService {

    @Value("${app.upload.local-dir:uploads}")
    private String uploadDirProp;

    // 허용 확장자(소문자)
    private static final Set<String> ALLOWED_EXT = Set.of(
            // 이미지
            "jpg","jpeg","png","gif","webp","bmp","heic",
            // 동영상 (필요 시 추가)
            "mp4","mov","avi","mkv"
    );

    // 확장자 → 간단 MIME 매핑 (요청 contentType 이 모호할 때 보완)
    private static final Map<String, String> EXT_TO_MIME = Map.ofEntries(
            Map.entry("jpg",  "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png",  "image/png"),
            Map.entry("gif",  "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp",  "image/bmp"),
            Map.entry("heic", "image/heic"),
            Map.entry("mp4",  "video/mp4"),
            Map.entry("mov",  "video/quicktime"),
            Map.entry("avi",  "video/x-msvideo"),
            Map.entry("mkv",  "video/x-matroska")
    );

    // 업로드 최대 용량 (기본 200MB)
    private static final long MAX_BYTES = 200L * 1024 * 1024;

    /**
     * 단순 저장용: 파일을 로컬에 저장하고 브라우저 접근 가능한 public URL 을 반환한다.
     * (PostBridge 등에서 URL 문자열만 필요할 때 사용)
     */
    public String save(MultipartFile file) {
        Stored s = storeInternal(file, "misc"); // 기본 서브폴더 "misc"
        return s.publicPath;
    }

    /**
     * 게시글(Post) 전용 저장: 파일을 저장하고 Media 엔티티를 만들어 반환한다.
     * (DB 저장은 호출자에서 MediaRepository.save(media) 수행)
     */
    public Media saveForPost(MultipartFile file, Long ownerUserId) {
        Stored s = storeInternal(file, "post"); // 게시글 전용 하위폴더

        Media media = new Media();
        media.setOwnerUserId(ownerUserId);
        media.setBucket("local");           // 로컬 저장소 식별자
        media.setPath(s.publicPath);        // 예) /uploads/post/2025/10/uuid.jpg
        media.setMime(s.contentType);       // 예) image/jpeg
        media.setScope(Media.Scope.post);   // 게시글 범위

        // Media 엔티티에 필드가 있다면 주석 해제해서 사용
        // media.setOriginalName(s.originalName);
        // media.setSize(s.size);

        return media;
    }

    /* ============================ 내부 구현 ============================ */

    private static final String PUBLIC_PREFIX = "/uploads";

    /**
     * 실제 저장 로직 (공용)
     * @param file 업로드 파일
     * @param subdir 하위 폴더명 (예: "post", "misc"...)
     * @return 저장된 파일 메타
     */
    private Stored storeInternal(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 없습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("파일 용량 초과: 최대 " + (MAX_BYTES / (1024 * 1024)) + "MB");
        }

        // 원본 파일명/확장자
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        String ext = extractExtLower(originalName);
        if (ext.isEmpty() || !ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + ext);
        }

        // 저장 파일명: UUID.ext
        String uuid = UUID.randomUUID().toString();
        String filename = uuid + "." + ext;

        // 저장 루트 (절대경로 정규화)
        Path uploadRoot = Paths.get(Optional.ofNullable(uploadDirProp).orElse("uploads").trim())
                .toAbsolutePath()
                .normalize();

        // 날짜 기반 하위 경로: /{subdir}/YYYY/MM/
        LocalDate today = LocalDate.now();
        Path saveDir = uploadRoot
                .resolve(subdir)
                .resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .normalize();

        try {
            Files.createDirectories(saveDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉터리 생성 실패: " + saveDir, e);
        }

        Path target = saveDir.resolve(filename).normalize();

        // 저장 루트 밖으로 이탈 방지
        if (!target.startsWith(uploadRoot)) {
            throw new SecurityException("잘못된 저장 경로입니다.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }

        // MIME 결정
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank() ||
                "application/octet-stream".equalsIgnoreCase(contentType)) {
            contentType = EXT_TO_MIME.getOrDefault(ext, "application/octet-stream");
        }

        // 브라우저 접근 경로: /uploads/{subdir}/YYYY/MM/filename
        String publicPath = PUBLIC_PREFIX + "/" + subdir + "/"
                + today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/"
                + filename;

        return new Stored(publicPath, contentType, originalName, file.getSize());
    }

    private static String extractExtLower(String filename) {
        int idx = (filename == null) ? -1 : filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private record Stored(String publicPath, String contentType, String originalName, long size) {}
}
