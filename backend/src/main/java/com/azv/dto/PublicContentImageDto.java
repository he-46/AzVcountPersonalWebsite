package com.azv.dto;

import com.azv.entity.ContentImage;

/** 图片公开字段；隐藏内容外键及数据库写入时间。 */
public record PublicContentImageDto(
        Long id,
        String url,
        String thumbnailUrl,
        Integer sortOrder
) {
    public static PublicContentImageDto from(ContentImage image) {
        return new PublicContentImageDto(
                image.getId(),
                image.getUrl(),
                image.getThumbnailUrl(),
                image.getSortOrder()
        );
    }
}
