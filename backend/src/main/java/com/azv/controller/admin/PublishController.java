package com.azv.controller.admin;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.ContentImage;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.entity.enums.SourceType;
import com.azv.mapper.ContentImageMapper;
import com.azv.mapper.ContentMapper;
import com.azv.security.ClientIpResolver;
import com.azv.storage.FileStorageService;
import com.azv.storage.StoredImage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/publish")
@RequiredArgsConstructor
public class PublishController {

    private final ContentMapper contentMapper;
    private final ContentImageMapper contentImageMapper;
    private final FileStorageService storageService;
    private final ClientIpResolver clientIpResolver;

    /** 发帖（可附带 0-3 张图）：站长免审，直接 APPROVED */
    @PostMapping("/post")
    @Transactional                                  // 话题+图片 要么全成功要么全回滚
    public R<Void> publish(@RequestParam String title,
                           @RequestParam String body,
                           @RequestParam(value = "files", required = false) List<MultipartFile> files,
                           HttpServletRequest request) {
        if (title == null || title.trim().isEmpty()) throw new BizException("标题不能为空");
        if (body == null || body.trim().isEmpty()) throw new BizException("正文不能为空");
        if (title.trim().length() > 120) throw new BizException("标题最长120字");
        if (body.trim().length() > 60_000) throw new BizException("正文最长60000字");

        // ① 可选图片：最多 3 张
        List<StoredImage> storedList = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            if (files.size() > 3) throw new BizException("最多上传 3 张图片");
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                try {
                    storedList.add(storageService.store(f));
                } catch (BizException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BizException("图片处理失败，请重试");
                }
            }
        }

        // ② 落库：type=POST, source=ADMIN, APPROVED（免审）
        Content c = new Content();
        c.setType(ContentType.POST);
        c.setSource(SourceType.ADMIN);
        c.setTitle(title.trim());
        c.setBody(body.trim());
        if (!storedList.isEmpty()) {
            c.setMediaUrl(storedList.get(0).url());      // 第一张作封面
            c.setThumbnailUrl(storedList.get(0).thumbnailUrl());
        }
        c.setStatus(ContentStatus.APPROVED);
        c.setIp(clientIpResolver.resolve(request));
        c.setUa(limit(request.getHeader("User-Agent"), 255));
        contentMapper.insert(c);

        // ③ 图片表（可选）
        for (int i = 0; i < storedList.size(); i++) {
            StoredImage s = storedList.get(i);
            ContentImage img = new ContentImage();
            img.setContentId(c.getId());
            img.setUrl(s.url());
            img.setThumbnailUrl(s.thumbnailUrl());
            img.setSortOrder(i);
            contentImageMapper.insert(img);
        }
        return R.ok(null);
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.substring(0, Math.min(value.length(), maxLength));
    }

}
