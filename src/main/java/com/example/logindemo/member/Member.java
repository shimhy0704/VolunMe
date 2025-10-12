package com.example.logindemo.member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    @Column(name = "status_message", length = 200)
    private String statusMessage;

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private int exp = 0;

    private String profileImageUrl;
    private String region;

    /**
     * 경험치를 추가하고, 꽉 차면 바로 레벨업
     * - expToLevel = level * 5
     * - exp >= expToLevel → exp = 0, level++
     */
    public void addExp(int amount) {
        this.exp += amount;

        // 현재 레벨의 필요 경험치량
        int expToLevel = level * 5;

        // 꽉 차거나 초과하면 레벨업
        while (this.exp >= expToLevel) {
            this.exp -= expToLevel;  // 남은 경험치 이월 (ex: exp=7, 필요=5 → 남은 2)
            this.level++;
            expToLevel = level * 5;  // 다음 레벨 기준 재계산
        }

        // 만약 정확히 딱 채웠으면 exp를 0으로 (즉시 리셋)
        if (this.exp == 0) {
            exp = 0;
        }
    }
}
