package com.example.logindemo.post;

import com.example.logindemo.member.Member;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post") // MySQL 테이블명과 매핑
@EntityListeners(AuditingEntityListener.class)
@NamedEntityGraph( // 리포지토리에서 엔티티그래프 사용할 수 있게 사전 정의
        name = "Post.withAuthor",
        attributeNodes = @NamedAttributeNode("author")
)
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"author", "attachments", "likes", "comments"}) // Lazy 로딩 안전
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * 여러 게시글(N)이 하나의 회원(1)에 속함
     * 실제 컬럼명은 member_id이고, 필드명은 author임
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author; // DB 컬럼은 member_id

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50) // 리스트 렌더링 시 N+1 완화(선택)
    private List<PostMedia> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<PostLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<PostComment> comments = new ArrayList<>();

    /** 좋아요 개수 헬퍼 */
    public int likeCount() {
        return (likes == null) ? 0 : likes.size();
    }

    /**
     * ✅ 템플릿/컨트롤러 호환용 브릿지
     * feed.html에서 ${post.member.displayName}를 그대로 쓰도록 제공
     * (실제 필드명은 author)
     */
    @Transient
    public Member getMember() { return this.author; }
    public void setMember(Member m) { this.author = m; }

    // 모집글과의 연결(없으면 일반 게시물)
    @jakarta.persistence.Column(name = "recruit_id")
    private Long recruitId;
}
