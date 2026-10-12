package com.moviereview.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService 测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 清理测试数据（用不同用户名避免冲突）
    }

    @Test
    @DisplayName("register - 注册成功")
    void register_success() {
        User user = userService.register("testuser1", "password123", UserRole.USER);
        assertNotNull(user);
        assertEquals("testuser1", user.getUsername());
        assertEquals(UserRole.USER, user.getRole());
        assertNull(user.getPassword(), "不应返回密码");
    }

    @Test
    @DisplayName("register - 用户名重复抛 IllegalArgumentException")
    void register_duplicateUsername_throws() {
        userService.register("testuser2", "password123", UserRole.USER);
        assertThrows(IllegalArgumentException.class,
                () -> userService.register("testuser2", "password123", UserRole.USER));
    }

    @Test
    @DisplayName("register - 密码太短抛 IllegalArgumentException")
    void register_shortPassword_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.register("testuser3", "123", UserRole.USER));
    }

    @Test
    @DisplayName("login - 正确用户名密码登录成功")
    void login_success() {
        userService.register("testuser4", "password123", UserRole.USER);
        User user = userService.login("testuser4", "password123");
        assertNotNull(user);
        assertNull(user.getPassword());
    }

    @Test
    @DisplayName("login - 错误密码抛 IllegalArgumentException")
    void login_wrongPassword_throws() {
        userService.register("testuser5", "password123", UserRole.USER);
        assertThrows(IllegalArgumentException.class,
                () -> userService.login("testuser5", "wrongpassword"));
    }

    @Test
    @DisplayName("setEnabled - 禁用用户后无法登录")
    void setEnabled_disable_preventsLogin() {
        User user = userService.register("testuser6", "password123", UserRole.USER);
        userService.setEnabled(user.getId(), false);
        assertThrows(IllegalArgumentException.class,
                () -> userService.login("testuser6", "password123"));
    }

    @Test
    @DisplayName("resetPassword - 重置后用新密码登录")
    void resetPassword_works() {
        User user = userService.register("testuser7", "password123", UserRole.USER);
        userService.resetPassword(user.getId(), "newpassword456");
        User login = userService.login("testuser7", "newpassword456");
        assertNotNull(login);
    }
}
