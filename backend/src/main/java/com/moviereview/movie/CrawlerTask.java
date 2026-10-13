package com.moviereview.movie;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 爬虫任务。
 */
@Data
@TableName("crawler_task")
public class CrawlerTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String movieId;
    private String status;
    private Integer progress;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
