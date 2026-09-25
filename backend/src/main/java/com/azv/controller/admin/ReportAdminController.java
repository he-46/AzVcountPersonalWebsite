package com.azv.controller.admin;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.Report;
import com.azv.entity.enums.ReportStatus;
import com.azv.mapper.ContentMapper;
import com.azv.mapper.ReportMapper;
import com.azv.service.ContentMediaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportAdminController {
    private final ReportMapper reportMapper;
    private final ContentMapper contentMapper;
    private final ContentMediaService contentMediaService;
    @GetMapping("/list")
    public R<Page<Report>> list(@RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "10") long size,
                                @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Report> w = new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, ReportStatus.OPEN)
                .orderByAsc(Report::getCreatedAt);
        return R.ok(reportMapper.selectPage(new Page<>(page, size), w));
    }
    @PostMapping("/{id}/take-down")
    @Transactional
    public R<Void> takeDown(@PathVariable Long id) {
        Report report = mustOpen(id);
        Content content = contentMapper.selectById(report.getContentId());
        if (content != null) {
            contentMediaService.deleteAll(content);
            Content update = new Content();
            update.setId(report.getContentId());
            update.setStatus(ContentStatus.DELETED);
            contentMapper.updateById(update);
        }
        markHandled(report);
        return R.ok(null);
    }
    @PostMapping("/{id}/ignore")
    public R<Void> ignore(@PathVariable Long id) {
        Report report = mustOpen(id);
        Report update = new Report();
        update.setId(report.getId());
        update.setStatus(ReportStatus.IGNORED);
        update.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(update);
        return R.ok(null);
    }
    private Report mustOpen(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null || report.getStatus() != ReportStatus.OPEN) {
            throw new BizException("举报不存在或已处理");
        }
        return report;
    }
    private void markHandled(Report report) {
        Report update = new Report();
        update.setId(report.getId());
        update.setStatus(ReportStatus.HANDLED);
        update.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(update);
    }
}
