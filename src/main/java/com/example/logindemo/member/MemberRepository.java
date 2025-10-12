package com.example.logindemo.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Member 엔티티에 대한 DB 처리
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    // username(=이메일)로 회원 조회
    Optional<Member> findByUsername(String username);

    // ① member.id == users.id 매핑일 때
    @Query(value = """
            SELECT u.profile_image_url
            FROM users u
            WHERE u.id = :memberId
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findUsersProfileImageUrlByMemberId(@Param("memberId") Long memberId);

    // ② member.username == users.email 매핑(조인, collate 통일)
    @Query(value = """
            SELECT u.profile_image_url
            FROM users u
            JOIN member m
              ON m.username COLLATE utf8mb4_0900_ai_ci
               = u.email    COLLATE utf8mb4_0900_ai_ci
            WHERE m.id = :memberId
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findUsersProfileImageUrlByMemberEmailJoin(@Param("memberId") Long memberId);

    // ③ 이메일로 직접 조회 (users.username 컬럼은 없으므로 email만 비교)
    @Query(value = """
            SELECT u.profile_image_url
            FROM users u
            WHERE u.email COLLATE utf8mb4_0900_ai_ci
               = CONVERT(:email USING utf8mb4) COLLATE utf8mb4_0900_ai_ci
            ORDER BY u.updated_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findUsersProfileImageUrlByEmail(@Param("email") String email);

    // (선택) ④ media(scope='profile') 최신 경로 폴백 (bucket/path 조합)
    @Query(value = """
SELECT CONCAT(COALESCE(m2.bucket, ''), '/', m2.path)
FROM member mem
JOIN users u
  ON (u.email COLLATE utf8mb4_general_ci = mem.username COLLATE utf8mb4_general_ci)
JOIN media m2
  ON (m2.owner_user_id = u.id AND m2.scope COLLATE utf8mb4_general_ci = 'profile' COLLATE utf8mb4_general_ci)
WHERE mem.id = :memberId
ORDER BY m2.created_at DESC
LIMIT 1
""", nativeQuery = true)
    Optional<String> findLatestProfileMediaUrlByMemberId(@Param("memberId") Long memberId);


    // 이메일 기준 프로필 이미지 URL 업데이트
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE users
               SET profile_image_url = :url
             WHERE email COLLATE utf8mb4_0900_ai_ci
                 = CONVERT(:email USING utf8mb4) COLLATE utf8mb4_0900_ai_ci
            """, nativeQuery = true)
    int updateUsersProfileImageUrlByEmail(@Param("email") String email, @Param("url") String url);

    // 카카오 ID 기준 프로필 이미지 URL 업데이트 (auth_providers 통해 연결)
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE users u
            JOIN auth_providers ap
              ON ap.user_id = u.id
             AND ap.provider = 'kakao'
             AND ap.provider_user_id = :kakaoId
               SET u.profile_image_url = :url
            """, nativeQuery = true)
    int updateUsersProfileImageUrlByKakaoId(@Param("kakaoId") String kakaoId, @Param("url") String url);

    // 1) 상태메세지 읽기
    @Query(value = "SELECT bio FROM user_profiles WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<String> findStatusMessageByMemberId(@Param("userId") Long userId);

    // 2) 상태메세지 업데이트
    @Modifying
    @Query(value = "UPDATE user_profiles SET bio = :bio, updated_at = NOW() WHERE user_id = :userId", nativeQuery = true)
    int updateStatusMessage(@Param("userId") Long userId, @Param("bio") String bio);

    // 3) 프로필 행이 없을 때 삽입 (avatar_type 기본값 'kakao' 보장)
    @Modifying
    @Query(value = "INSERT INTO user_profiles (user_id, bio, created_at, updated_at) VALUES (:userId, :bio, NOW(), NOW())", nativeQuery = true)
    int insertStatusRow(@Param("userId") Long userId, @Param("bio") String bio);

    /** email로 users 레코드가 없으면 생성 */
    @Modifying
    @Transactional
    @Query(
            value = """
              INSERT INTO users (email, profile_image_url, created_at, updated_at)
              SELECT :email, :profileUrl, NOW(), NOW()
              WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = :email)
              """,
            nativeQuery = true
    )
    int ensureUsersByEmail(@Param("email") String email, @Param("profileUrl") String profileUrl);

    /** email로 users.id 조회 */
    @Query(value = "SELECT id FROM users WHERE email = :email LIMIT 1", nativeQuery = true)
    Long findUsersIdByEmail(@Param("email") String email);

    /** user_profiles upsert (users.id를 FK로 사용) */
    @Modifying
    @Transactional
    @Query(
            value = """
              INSERT INTO user_profiles (user_id, bio, created_at, updated_at)
              VALUES (:usersId, :bio, NOW(), NOW())
              ON DUPLICATE KEY UPDATE bio = VALUES(bio), updated_at = NOW()
              """,
            nativeQuery = true
    )
    int upsertUserProfile(@Param("usersId") Long usersId, @Param("bio") String bio);
}


