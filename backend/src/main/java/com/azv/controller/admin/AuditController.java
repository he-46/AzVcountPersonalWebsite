package com.azv.controller.admin;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.Report;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.entity.enums.ReportStatus;
import com.azv.mapper.ContentMapper;
import com.azv.mapper.ReportMapper;
import com.azv.service.ContentMediaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final ContentMapper contentMapper;
    private final ReportMapper reportMapper;
    private final ContentMediaService contentMediaService;

    /** 待审列表：PENDING 优先处理，可按类型过滤（评论/投稿） */
    @GetMapping("/list")
    public R<Page<Content>> list(@RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String type) {
        LambdaQueryWrapper<Content> w = new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, ContentStatus.PENDING)
                .eq(type != null && !type.isEmpty(), Content::getType, type)
                .orderByAsc(Content::getCreatedAt);      // 先来的先审
        return R.ok(contentMapper.selectPage(new Page<>(page, size), w));
    }

    /** 通过：APPROVED + 审核留痕 */
    @PostMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id, HttpServletRequest request) {
        mustPending(id);                                  // 防重复审核
        Content update = new Content();
        update.setId(id);
        update.setStatus(ContentStatus.APPROVED);
        update.setReviewedBy(currentUserId(request));
        update.setReviewedAt(LocalDateTime.now());
        contentMapper.updateById(update);
        return R.ok(null);
    }

    /** 驳回：投稿物理删文件，评论逻辑删除（REJECTED） */
    @PostMapping("/reject/{id}")
    @Transactional
    public R<Void> reject(@PathVariable Long id, HttpServletRequest request) {
        Content c = mustPending(id);
        contentMediaService.deleteAll(c);
        Content update = new Content();
        update.setId(id);
        update.setStatus(ContentStatus.REJECTED);
        update.setReviewedBy(currentUserId(request));
        update.setReviewedAt(LocalDateTime.now());
        contentMapper.updateById(update);
        return R.ok(null);
    }

    /** 取待审内容，非 PENDING 直接拒绝（防并发/重复审核） */
    private Content mustPending(Long id) {
        Content c = contentMapper.selectById(id);
        if (c == null || c.getStatus() != ContentStatus.PENDING) {
            throw new BizException("内容不存在或已被处理");
        }
        return c;
    }

    private Long currentUserId(HttpServletRequest request) {
        return Long.valueOf((String) request.getAttribute("userId"));
    }
    // 放 AuditController 里加一个方法即可
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Map<String, Object> m = new HashMap<>();
        // 待审数（评论 + 投稿）
        Long pendingComment = contentMapper.selectCount(
                new LambdaQueryWrapper<Content>().eq(Content::getStatus, ContentStatus.PENDING)
                        .eq(Content::getType, ContentType.COMMENT));
        Long pendingPost = contentMapper.selectCount(
                new LambdaQueryWrapper<Content>().eq(Content::getStatus, ContentStatus.PENDING)
                        .eq(Content::getType, ContentType.POST));
        // 待处理举报
        Long openReports = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, ReportStatus.OPEN));
        // 内容总数
        Long total = contentMapper.selectCount(null);
        m.put("pendingComment", pendingComment);
        // 保留既有字段名，避免旧后台在升级期间中断；统计口径已改为 POST 投稿。
        m.put("pendingImage", pendingPost);
        m.put("openReports", openReports);
        m.put("total", total);
        return R.ok(m);
    }

}
