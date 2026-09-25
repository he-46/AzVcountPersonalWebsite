package com.azv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_image")
public class ContentImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contentId;
    private String url;
    private String thumbnailUrl;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
