package com.moviereview.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏实体。
 */
@Data
@TableName("user_favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String movieId;
    private LocalDateTime createdAt;

    public Favorite() {}

    public Favorite(Long userId, String movieId) {
        this.userId = userId;
        this.movieId = movieId;
        this.createdAt = LocalDateTime.now();
    }
}
