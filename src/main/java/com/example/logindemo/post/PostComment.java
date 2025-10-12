package com.example.logindemo.post;

import com.example.logindemo.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post_comments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 글의 댓글인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 작성자 (필드명은 기존대로 member 유지) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 대댓글(답글) 위한 parent (null이면 최상위 댓글) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PostComment parent;

    /** 자식들(대댓글 목록) */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC") // 또는 createdAt ASC
    private List<PostComment> children = new ArrayList<>();

    @Column(nullable = false, length = 2000)
    private String content;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ========= 템플릿 호환용 편의 게터 ========= */

    /**
     * 템플릿에서 c.author.displayName 처럼 접근하고 싶을 때 사용.
     * DB 스키마는 바꾸지 않고, 논리적 alias 제공.
     */
    @Transient
    public Member getAuthor() {
        return this.member;
    }
}
