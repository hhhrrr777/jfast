package io.github.hhhrrr777.jfast.baseline.auth.service;

import io.github.hhhrrr777.jfast.baseline.config.SecurityProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破:按账号内存计数,连续失败达阈值后锁定一段时间。
 * 阈值与锁定时长见 SecurityProperties(模板可改常量)。内存实现,重启即清零,不持久化。
 *
 * <p>非 Spring 组件——由 SecurityConfig 以 @Bean 注册,从而保留单一公开构造,
 * 纯逻辑单测可直接 new 注入阈值与锁定时长。
 */
public class LoginFailureGuard {

    private final int maxFailCount;
    private final Duration lockDuration;

    /** key = 登录账号。 */
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public LoginFailureGuard(int maxFailCount, Duration lockDuration) {
        this.maxFailCount = maxFailCount;
        this.lockDuration = lockDuration;
    }

    /** 由配置属性构造。 */
    public static LoginFailureGuard fromProperties(SecurityProperties properties) {
        return new LoginFailureGuard(
                properties.getLoginMaxFailCount(),
                Duration.ofSeconds(properties.getLoginLockDurationSeconds()));
    }

    private static final class Entry {
        private int failCount;
        private Instant lockedUntil;
    }

    /**
     * 账号当前是否处于锁定状态。
     */
    public boolean isLocked(String username) {
        Entry entry = entries.get(username);
        if (entry == null || entry.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isBefore(entry.lockedUntil)) {
            return true;
        }
        // 锁定期已过,清除记录允许重试
        entries.remove(username, entry);
        return false;
    }

    /**
     * 记录一次登录失败;达到阈值即锁定。返回锁定剩余秒数,未锁定返回 0。
     */
    public long recordFailure(String username) {
        Entry entry = entries.computeIfAbsent(username, k -> new Entry());
        synchronized (entry) {
            entry.failCount++;
            if (entry.failCount >= maxFailCount) {
                entry.lockedUntil = Instant.now().plus(lockDuration);
                return lockDuration.getSeconds();
            }
            return 0;
        }
    }

    /**
     * 登录成功,清零失败计数与锁定状态。
     */
    public void recordSuccess(String username) {
        entries.remove(username);
    }

    /** 清空全部计数与锁定(测试隔离用)。 */
    public void reset() {
        entries.clear();
    }

    /** 剩余锁定秒数,未锁定返回 0。 */
    public long remainingLockSeconds(String username) {
        Entry entry = entries.get(username);
        if (entry == null || entry.lockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), entry.lockedUntil).getSeconds();
        return Math.max(seconds, 0);
    }
}
