package io.github.hhhrrr777.jfast.baseline.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防爆破纯逻辑单测:连续失败阈值、锁定、锁定期后放行、成功清零。
 */
class LoginFailureGuardTest {

    private static final int MAX = 5;
    private static final Duration LOCK = Duration.ofMinutes(10);

    private LoginFailureGuard newGuard() {
        return new LoginFailureGuard(MAX, LOCK);
    }

    @Test
    void notLockedBelowThreshold() {
        LoginFailureGuard guard = newGuard();
        for (int i = 0; i < MAX - 1; i++) {
            assertThat(guard.recordFailure("alice")).isZero();
        }
        assertThat(guard.isLocked("alice")).isFalse();
    }

    @Test
    void locksAtThreshold() {
        LoginFailureGuard guard = newGuard();
        long lockSeconds = 0;
        for (int i = 0; i < MAX; i++) {
            lockSeconds = guard.recordFailure("alice");
        }
        assertThat(lockSeconds).isEqualTo(LOCK.getSeconds());
        assertThat(guard.isLocked("alice")).isTrue();
        assertThat(guard.remainingLockSeconds("alice")).isGreaterThan(0);
    }

    @Test
    void successClearsFailureCount() {
        LoginFailureGuard guard = newGuard();
        for (int i = 0; i < MAX - 1; i++) {
            guard.recordFailure("alice");
        }
        guard.recordSuccess("alice");
        assertThat(guard.isLocked("alice")).isFalse();
        // 清零后需重新累计满阈值才锁
        for (int i = 0; i < MAX - 1; i++) {
            assertThat(guard.recordFailure("alice")).isZero();
        }
        assertThat(guard.isLocked("alice")).isFalse();
    }

    @Test
    void differentAccountsIndependent() {
        LoginFailureGuard guard = newGuard();
        for (int i = 0; i < MAX; i++) {
            guard.recordFailure("alice");
        }
        assertThat(guard.isLocked("alice")).isTrue();
        assertThat(guard.isLocked("bob")).isFalse();
    }

    @Test
    void lockExpiresAfterDuration() {
        // 用极短锁定时长验证过期放行
        LoginFailureGuard guard = new LoginFailureGuard(1, Duration.ofMillis(1));
        guard.recordFailure("alice");
        // 等锁定期过
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(guard.isLocked("alice")).isFalse();
        assertThat(guard.remainingLockSeconds("alice")).isZero();
    }
}
