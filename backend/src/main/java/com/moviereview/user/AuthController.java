package com.moviereview.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 注册。
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String roleStr = body.getOrDefault("role", "USER");
            UserRole role = "ADMIN".equalsIgnoreCase(roleStr) ? UserRole.ADMIN : UserRole.USER;

            User user = userService.register(username, password, role);
            return ResponseEntity.ok(Map.of("user", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 登录。
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            User user = userService.login(username, password);
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", user
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取当前用户信息（需认证）。
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute(name = "userId", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "用户不存在"));
        }
        user.setPassword(null);
        return ResponseEntity.ok(Map.of("user", user));
    }
}
