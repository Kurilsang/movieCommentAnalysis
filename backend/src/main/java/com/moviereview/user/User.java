package com.moviereview.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;  // BCrypt 哈希
    private UserRole role;
    private Boolean enabled;   // 是否启用（管理员可禁用）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
