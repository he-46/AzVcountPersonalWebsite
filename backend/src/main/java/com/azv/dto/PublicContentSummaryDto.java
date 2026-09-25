package com.azv.dto;

import com.azv.entity.Content;
import com.azv.entity.enums.ContentType;

import java.time.LocalDateTime;

/** 公开内容流字段；刻意不包含审核状态、来源、IP、UA 等内部数据。 */
public record PublicContentSummaryDto(
        Long id,
        ContentType type,
        String title,
        String excerpt,
        String mediaUrl,
        String thumbnailUrl,
        Integer likeCount,
        Integer viewCount,
        String authorLabel,
        LocalDateTime createdAt
) {
    private static final int EXCERPT_LENGTH = 180;

    public static PublicContentSummaryDto from(Content content) {
        return new PublicContentSummaryDto(
                content.getId(),
                content.getType(),
                content.getTitle(),
                excerpt(content.getBody()),
                content.getMediaUrl(),
                content.getThumbnailUrl(),
                content.getLikeCount(),
                content.getViewCount(),
                content.getAuthorLabel(),
                content.getCreatedAt()
        );
    }

    private static String excerpt(String body) {
        if (body == null || body.isBlank()) return "";

        // 内容流只需要纯文本摘要；详情页仍由前端的 Markdown 组件渲染完整正文。
        String plain = body
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", "")
                .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                .replaceAll("[`*_>~]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (plain.length() <= EXCERPT_LENGTH) return plain;
        return plain.substring(0, EXCERPT_LENGTH).stripTrailing() + "…";
    }
}
