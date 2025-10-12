package com.example.logindemo.member;

import org.springframework.stereotype.Service;

@Service
public class ExperienceService {

    public static class ExpView {
        public final int level;
        public final long exp;
        public final long expToLevel;
        public final int percent;

        public ExpView(int level, long exp, long expToLevel, int percent) {
            this.level = level;
            this.exp = exp;
            this.expToLevel = expToLevel;
            this.percent = percent;
        }
    }

    public ExpView build(Member m) {
        if (m == null) return new ExpView(1, 0, 100, 0);

        int level = safeLevel(m);
        long exp = safeExp(m);

        // ① Member에 getExpToLevel()이 있으면 그걸 우선 사용
        Long expToLevel = tryGetExpToLevel(m);
        if (expToLevel == null || expToLevel <= 0) {
            // ② 없다면 간단 공식 (원하는 규칙으로 대체)
            expToLevel = Math.max(1, level * 100L);
        }

        int percent = (int) Math.round(Math.min(100.0, Math.max(0.0, exp * 100.0 / expToLevel)));
        return new ExpView(level, exp, expToLevel, percent);
    }

    private int safeLevel(Member m) {
        try {
            var mm = m.getClass().getMethod("getLevel");
            Object v = mm.invoke(m);
            if (v instanceof Number n) return Math.max(1, n.intValue());
        } catch (Throwable ignored) {}
        return 1;
    }

    private long safeExp(Member m) {
        try {
            var mm = m.getClass().getMethod("getExp");
            Object v = mm.invoke(m);
            if (v instanceof Number n) return Math.max(0, n.longValue());
        } catch (Throwable ignored) {}
        return 0L;
    }

    private Long tryGetExpToLevel(Member m) {
        try {
            var mm = m.getClass().getMethod("getExpToLevel");
            Object v = mm.invoke(m);
            if (v instanceof Number n) return n.longValue();
        } catch (Throwable ignored) {}
        return null;
    }
}
