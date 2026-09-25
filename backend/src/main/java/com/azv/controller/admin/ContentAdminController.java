package com.azv.controller.admin;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.enums.ContentStatus;
import com.azv.mapper.ContentMapper;
import com.azv.service.ContentMediaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class ContentAdminController {

    private final ContentMapper contentMapper;
    private final ContentMediaService contentMediaService;

    /** 后台列表：全部内容（所有状态），type/status 筛选 + 分页 */
    @GetMapping("/list")
    public R<Page<Content>> list(@RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Content> w = new LambdaQueryWrapper<Content>()
                .eq(type != null && !type.isEmpty(), Content::getType, type)
                .eq(status != null && !status.isEmpty(), Content::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), k -> k
                    .like(Content::getTitle, keyword)
                    .or().like(Content::getBody, keyword)
                    .or().like(Content::getAuthorLabel, keyword))
                .orderByDesc(Content::getSortWeight)
                .orderByDesc(Content::getCreatedAt);
        return R.ok(contentMapper.selectPage(new Page<>(page, size), w));
    }

    /** 隐藏：DELETED 状态（软删除——数据保留，前台不可见） */
    @PostMapping("/{id}/hide")
    public R<Void> hide(@PathVariable Long id) {
        mustExist(id);
        updateStatus(id, ContentStatus.DELETED);
        return R.ok(null);
    }

    /** 删除：DELETED + 投稿文件物理删除（彻底清除） */
    @PostMapping("/{id}/delete")
    @Transactional
    public R<Void> delete(@PathVariable Long id) {
        Content c = mustExist(id);
        contentMediaService.deleteAll(c);
        updateStatus(id, ContentStatus.DELETED);
        return R.ok(null);
    }

    /** 置顶：sort_weight=1，取消置顶传 pin=false */
    @PostMapping("/{id}/pin")
    public R<Void> pin(@PathVariable Long id,
                       @RequestParam(defaultValue = "true") boolean pin) {
        mustExist(id);
        Content update = new Content();
        update.setId(id);
        update.setSortWeight(pin ? 1 : 0);
        contentMapper.updateById(update);
        return R.ok(null);
    }

    private Content mustExist(Long id) {
        Content c = contentMapper.selectById(id);
        if (c == null) throw new BizException("内容不存在");
        return c;
    }

    private void updateStatus(Long id, ContentStatus status) {
        Content update = new Content();
        update.setId(id);
        update.setStatus(status);
        contentMapper.updateById(update);
    }
}
