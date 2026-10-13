package com.moviereview.movie;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档级分析结果（一条评论一行）。
 */
@Data
@TableName("comment_analysis")
public class CommentAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private String polarity;
    private Double confidence;
    private String triggerWords;
    private String analyzerName;
    private String rawJson;
    private Long elapsedMs;
    private LocalDateTime createdAt;
}
