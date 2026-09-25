package com.azv.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
// User.java —— Lombok + MP 注解
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;   // MP 自动映射 password_hash
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
}
