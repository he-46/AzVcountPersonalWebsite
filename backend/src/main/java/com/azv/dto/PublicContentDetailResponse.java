package com.azv.dto;

import java.util.List;

/** 保持前端既有的 { content, images } 响应形状。 */
public record PublicContentDetailResponse(
        PublicContentDetailDto content,
        List<PublicContentImageDto> images
) {
}
