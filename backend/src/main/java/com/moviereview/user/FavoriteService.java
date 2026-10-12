package com.moviereview.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收藏服务。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    /**
     * 收藏电影。
     */
    public void addFavorite(Long userId, String movieId) {
        // 检查是否已收藏
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getMovieId, movieId));
        if (count > 0) {
            throw new IllegalArgumentException("已收藏该电影");
        }
        favoriteMapper.insert(new Favorite(userId, movieId));
    }

    /**
     * 取消收藏。
     */
    public void removeFavorite(Long userId, String movieId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getMovieId, movieId));
    }

    /**
     * 查看用户的收藏列表。
     */
    public List<Favorite> listFavorites(Long userId) {
        return favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt));
    }

    /**
     * 检查是否已收藏。
     */
    public boolean isFavorited(Long userId, String movieId) {
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getMovieId, movieId)) > 0;
    }
}
