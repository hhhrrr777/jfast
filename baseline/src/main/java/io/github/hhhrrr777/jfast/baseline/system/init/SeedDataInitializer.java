package io.github.hhhrrr777.jfast.baseline.system.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUser;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 认证域种子数据:admin/admin123(幂等,已存在则跳过)。
 * 文档提示首登后改密;密码 BCrypt 在启动时编码,不落明文到 SQL。
 */
@Component
public class SeedDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin123";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public SeedDataInitializer(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, ADMIN_USERNAME));
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUserName(ADMIN_USERNAME);
        admin.setNickName("系统管理员");
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setStatus("0");
        admin.setCreateBy("system");
        admin.setCreateTime(LocalDateTime.now());
        admin.setRemark("种子管理员,首次登录后请修改密码");
        userMapper.insert(admin);
        log.info("已创建种子管理员账号 admin(默认密码 admin123,请首次登录后修改)");
    }
}
