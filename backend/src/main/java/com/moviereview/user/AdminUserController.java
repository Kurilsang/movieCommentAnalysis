package com.moviereview.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员用户管理控制器。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 禁用/启用用户。
     */
    @PutMapping("/{userId}/enabled")
    public ResponseEntity<?> setEnabled(@PathVariable Long userId,
                                        @RequestParam boolean enabled) {
        try {
            userService.setEnabled(userId, enabled);
            return ResponseEntity.ok(Map.of("message", enabled ? "已启用" : "已禁用"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重置密码。
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> resetPassword(@PathVariable Long userId,
                                           @RequestBody Map<String, String> body) {
        try {
            String newPassword = body.get("newPassword");
            userService.resetPassword(userId, newPassword);
            return ResponseEntity.ok(Map.of("message", "密码已重置"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
