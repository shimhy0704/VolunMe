package com.example.logindemo.post;

import com.example.logindemo.member.Member;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Deprecated
    @EntityGraph(attributePaths = {"author"})
    List<Post> findByAuthorOrderByCreatedDateDesc(Member author);

    @Query("""
      select distinct p
      from Post p
      left join fetch p.attachments a
      where p.author = :author
        and p.recruitId is not null
      order by p.createdDate desc
    """)
    List<Post> findByAuthorWithAttachmentsOrderByCreatedDateDesc(@Param("author") Member author);

    /**
     * (변경) 피드: '후기글'만 페이징. author는 fetch join 해도 컬렉션이 아니라서 OK.
     */
    @Query(
            value = "select p from Post p join fetch p.author order by p.createdDate desc",
            countQuery = "select count(p) from Post p"
    )
    Page<Post> findFeed(Pageable pageable);


    @Query("""
  select p
  from Post p
  where p.author.id = :userId
    and p.recruitId is not null
  order by p.createdDate desc
""")
    List<Post> findReviewsByAuthorId(@Param("userId") Long userId);

    @Query("""
  select count(p)
  from Post p
  where p.author.id = :userId
    and p.recruitId is not null
""")
    long countReviewsByAuthorId(@Param("userId") Long userId);




    /**
     * 특정 작성자의 '후기글'만 반환 (마이페이지용).
     */
    @Query("""
      select p
      from Post p
      where p.author = :author
        and p.recruitId is not null
      order by p.createdDate desc
    """)
    List<Post> findReviewsByAuthor(@Param("author") Member author);

    @Query("select count(pl) from PostLike pl where pl.post.id = :postId and pl.member.id = :memberId")
    long countLikeByMember(@Param("postId") Long postId, @Param("memberId") Long memberId);

    @Query("select m from PostMedia m where m.post.id in :postIds order by m.post.id asc, m.sortOrder asc")
    List<PostMedia> loadMediasByPostIdsV2(@Param("postIds") Collection<Long> postIds);
}