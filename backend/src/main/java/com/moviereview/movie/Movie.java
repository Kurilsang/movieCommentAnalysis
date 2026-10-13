package com.moviereview.movie;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电影实体。
 */
@Data
@TableName("movie")
public class Movie {
    /** 业务 ID（M + md5(title) 前 8 位，来自数据导入脚本）。 */
    @TableId(type = IdType.INPUT)
    private String id;
    private String title;
    private String director;
    private String actors;
    private Integer releaseYear;
    private String posterUrl;
    private String synopsis;
    private Integer commentCount;
    private LocalDateTime createdAt;
}
