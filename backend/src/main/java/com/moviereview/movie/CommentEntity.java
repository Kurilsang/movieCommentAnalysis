package com.moviereview.movie;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体。
 *
 * rating 是唯一的真实标签（1-5 星），评测基线必须用它。
 */
@Data
@TableName("comment")
public class CommentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String movieId;
    private String text;
    private String author;
    private String publishTime;
    private Integer likeCount;
    private Integer rating;
    private LocalDateTime createdAt;
}
