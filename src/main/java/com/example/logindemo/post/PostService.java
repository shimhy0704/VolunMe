package com.example.logindemo.post;

import com.example.logindemo.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    // 모든 게시글 조회
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public List<Post> findAllPosts() {
        return postRepository.findAll();
    }

    // ID로 게시글 조회
    @Transactional(readOnly = true)
    public Optional<Post> findPostById(Long id) {
        return postRepository.findById(id);
    }

    // 새 게시글 저장
    @Transactional
    public Post createPost(String title, String content, Member author) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setAuthor(author);
        return postRepository.save(post);
    }

    // 게시글 수정
    @Transactional
    public Optional<Post> updatePost(Long id, String title, String content, Member currentMember) {
        return postRepository.findById(id).flatMap(post -> {
            // 작성자만 수정 가능하도록 확인
            if (post.getAuthor().getId() != currentMember.getId()) {
                return Optional.empty(); // 권한이 없으면 빈 Optional 반환
            }
            post.setTitle(title);
            post.setContent(content);
            return Optional.of(postRepository.save(post));
        });
    }

    // 게시글 삭제
    @Transactional
    public boolean deletePost(Long id, Member currentMember) {
        Optional<Post> postOptional = postRepository.findById(id);
        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            // 작성자만 삭제 가능하도록 확인
            if (post.getAuthor().getId() != currentMember.getId()) {
                return false; // 권한이 없으면 false 반환
            }
            postRepository.delete(post);
            return true;
        }
        return false; // 게시글이 존재하지 않는 경우
    }
}
