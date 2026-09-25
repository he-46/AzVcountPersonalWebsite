package com.azv.entity;

import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.entity.enums.SourceType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content")
public class Content {
    @TableId(type = IdType.AUTO)
    private Long id;
    private ContentType type;
    private SourceType source;
    private String title;
    private String body;
    private String mediaUrl;
    private String thumbnailUrl;
    private ContentStatus status;
    private Integer likeCount;
    private Integer viewCount;
    private String ip;
    private String ua;
    private LocalDateTime createdAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private Long parentId; // 父内容 ID（评论的目标）
    private String authorLabel; // 作者标签（评论署名）
    private Integer sortWeight;
}