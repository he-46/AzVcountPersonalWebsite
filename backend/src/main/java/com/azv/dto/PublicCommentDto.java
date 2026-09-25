package com.azv.dto;

import com.azv.entity.Content;

import java.time.LocalDateTime;

/** 评论区所需的最小公开字段。 */
public record PublicCommentDto(
        Long id,
        String body,
        String authorLabel,
        LocalDateTime createdAt
) {
    public static PublicCommentDto from(Content content) {
        return new PublicCommentDto(
                content.getId(),
                content.getBody(),
                content.getAuthorLabel(),
                content.getCreatedAt()
        );
    }
}
