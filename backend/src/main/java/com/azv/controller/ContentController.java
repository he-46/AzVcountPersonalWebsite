package com.azv.controller;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.dto.PublicCommentDto;
import com.azv.dto.PublicContentDetailDto;
import com.azv.dto.PublicContentDetailResponse;
import com.azv.dto.PublicContentImageDto;
import com.azv.dto.PublicContentSummaryDto;
import com.azv.entity.Content;
import com.azv.entity.ContentImage;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.mapper.ContentImageMapper;
import com.azv.mapper.ContentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private static final long MAX_PAGE_SIZE = 100;
    private static final long MAX_COMMENT_PAGE_SIZE = 50;

    private final ContentMapper contentMapper;
    private final ContentImageMapper contentImageMapper;

    /**
     * 公开内容流：只返回已审核的非评论内容。withMedia=true 时由数据库筛选
     * 带封面、缩略图或 content_image 记录的内容，供图库分页使用。
     */
    @GetMapping("/list")
    public R<Page<PublicContentSummaryDto>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean withMedia) {
        long safePage = Math.max(1, page);
        long safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        ContentType requestedType = parsePublicType(type);

        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, ContentStatus.APPROVED)
                .ne(Content::getType, ContentType.COMMENT)
                .eq(requestedType != null, Content::getType, requestedType)
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(Content::getTitle, keyword.trim())
                        .or().like(Content::getBody, keyword.trim())
                        .or().like(Content::getAuthorLabel, keyword.trim()));

        if (withMedia) {
            wrapper.and(query -> query
                    .isNotNull(Content::getMediaUrl)
                    .ne(Content::getMediaUrl, "")
                    .or().isNotNull(Content::getThumbnailUrl)
                    .ne(Content::getThumbnailUrl, "")
                    .or().exists("SELECT 1 FROM content_image ci WHERE ci.content_id = content.id"));
        }

        wrapper.orderByDesc(Content::getSortWeight)
                .orderByDesc(Content::getCreatedAt);

        Page<Content> entityPage = contentMapper.selectPage(
                new Page<>(safePage, safeSize), wrapper);
        Page<PublicContentSummaryDto> publicPage = new Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        publicPage.setRecords(entityPage.getRecords().stream()
                .map(PublicContentSummaryDto::from)
                .toList());
        return R.ok(publicPage);
    }

    /** 公开详情：拒绝评论 ID，浏览数在数据库中原子递增后返回。 */
    @GetMapping("/{id}")
    public R<PublicContentDetailResponse> detail(@PathVariable Long id) {
        Content content = requirePublicPost(id);
        if (contentMapper.incrementViewCount(id) != 1) {
            throw new BizException("内容不存在");
        }

        // 重新读取，确保响应中的 viewCount 至少包含本次访问。
        content = contentMapper.selectById(id);
        List<PublicContentImageDto> images = contentImageMapper.selectList(
                        new LambdaQueryWrapper<ContentImage>()
                                .eq(ContentImage::getContentId, id)
                                .orderByAsc(ContentImage::getSortOrder))
                .stream()
                .map(PublicContentImageDto::from)
                .toList();

        if (images.isEmpty() && content.getMediaUrl() != null && !content.getMediaUrl().isBlank()) {
            images = List.of(new PublicContentImageDto(
                    null, content.getMediaUrl(), content.getThumbnailUrl(), 0));
        }

        return R.ok(new PublicContentDetailResponse(
                PublicContentDetailDto.from(content), images));
    }

    /** 某帖子的已审核评论；评论实体的审核和访问者信息不会出现在响应中。 */
    @GetMapping("/{id}/comments")
    public R<Page<PublicCommentDto>> comments(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "20") long size) {
        requirePublicPost(id);
        long safePage = Math.max(1, page);
        long safeSize = Math.max(1, Math.min(size, MAX_COMMENT_PAGE_SIZE));
        Page<Content> entityPage = contentMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getParentId, id)
                        .eq(Content::getType, ContentType.COMMENT)
                        .eq(Content::getStatus, ContentStatus.APPROVED)
                        .orderByAsc(Content::getCreatedAt)
                        .orderByAsc(Content::getId));
        Page<PublicCommentDto> publicPage = new Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        publicPage.setRecords(entityPage.getRecords().stream().map(PublicCommentDto::from).toList());
        return R.ok(publicPage);
    }

    private Content requirePublicPost(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null
                || content.getStatus() != ContentStatus.APPROVED
                || content.getType() == ContentType.COMMENT) {
            throw new BizException("内容不存在");
        }
        return content;
    }

    private ContentType parsePublicType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            ContentType parsed = ContentType.valueOf(type.trim().toUpperCase(Locale.ROOT));
            if (parsed == ContentType.COMMENT) throw new BizException("不支持的内容类型");
            return parsed;
        } catch (IllegalArgumentException ignored) {
            throw new BizException("不支持的内容类型");
        }
    }
}
