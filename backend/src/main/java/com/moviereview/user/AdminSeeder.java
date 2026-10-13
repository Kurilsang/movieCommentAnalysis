package com.moviereview.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时确保存在一个管理员账号（幂等）。
 *
 * 为什么：sys_user 表为空时没人能登录后台，答辩现场会卡在登录页。
 * 默认账号 admin / admin123，可通过配置覆盖。
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserService userService;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userService.findByUsername(adminUsername) != null) {
                log.info("管理员账号已存在：{}", adminUsername);
                return;
            }
            userService.register(adminUsername, adminPassword, UserRole.ADMIN);
            log.info("已创建默认管理员账号：{} / {}（请在生产环境修改）", adminUsername, adminPassword);
        } catch (Exception e) {
            log.warn("创建管理员账号失败：{}", e.getMessage());
        }
    }
}
