package com.azv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("portfolio")
public class Portfolio {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String techStack;
    private String link;
    private String coverUrl;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
}
