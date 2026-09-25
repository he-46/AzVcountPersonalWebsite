package com.azv.dto;

import com.azv.entity.Content;
import com.azv.entity.enums.ContentType;

import java.time.LocalDateTime;

/** 公开详情字段；不暴露审核与访问者留痕。 */
public record PublicContentDetailDto(
        Long id,
        ContentType type,
        String title,
        String body,
        String mediaUrl,
        String thumbnailUrl,
        Integer likeCount,
        Integer viewCount,
        String authorLabel,
        LocalDateTime createdAt
) {
    public static PublicContentDetailDto from(Content content) {
        return new PublicContentDetailDto(
                content.getId(),
                content.getType(),
                content.getTitle(),
                content.getBody(),
                content.getMediaUrl(),
                content.getThumbnailUrl(),
                content.getLikeCount(),
                content.getViewCount(),
                content.getAuthorLabel(),
                content.getCreatedAt()
        );
    }
}
