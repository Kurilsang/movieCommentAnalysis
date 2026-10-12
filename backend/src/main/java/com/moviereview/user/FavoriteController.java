package com.moviereview.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏控制器。
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<?> add(@PathVariable String movieId) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        try {
            favoriteService.addFavorite(userId, movieId);
            return ResponseEntity.ok(Map.of("message", "收藏成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> remove(@PathVariable String movieId) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        favoriteService.removeFavorite(userId, movieId);
        return ResponseEntity.ok(Map.of("message", "取消收藏"));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        List<Favorite> favorites = favoriteService.listFavorites(userId);
        return ResponseEntity.ok(Map.of("favorites", favorites));
    }

    @GetMapping("/{movieId}/status")
    public ResponseEntity<?> status(@PathVariable String movieId) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        boolean favorited = favoriteService.isFavorited(userId, movieId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }
}
