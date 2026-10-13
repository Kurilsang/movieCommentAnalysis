package com.moviereview.movie;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 方面级结果（纵表：一条评论 × 一个维度一行）。
 *
 * 用纵表而非宽表：维度集合可能调整，纵表零迁移成本。
 */
@Data
@TableName("comment_aspect")
public class CommentAspect {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private String aspectName;
    private Boolean mentioned;
    private String polarity;
    private Double confidence;
    private Integer sampleCount;
    private String triggerWords;
    private LocalDateTime createdAt;
}
