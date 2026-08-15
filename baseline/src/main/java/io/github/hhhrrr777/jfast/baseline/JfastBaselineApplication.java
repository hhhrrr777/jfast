package io.github.hhhrrr777.jfast.baseline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// 认证走 JWT 双 token,不用 Spring 默认的内存 UserDetailsService,排除以免生成临时密码噪音
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class JfastBaselineApplication {

    public static void main(String[] args) {
        SpringApplication.run(JfastBaselineApplication.class, args);
    }
}
