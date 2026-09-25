package com.azv.controller;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.entity.enums.SourceType;
import com.azv.entity.ContentImage;
import com.azv.mapper.ContentMapper;
import com.azv.mapper.ContentImageMapper;
import com.azv.security.ClientIpResolver;
import com.azv.service.SensitiveWordService;
import com.azv.storage.FileStorageService;
import com.azv.storage.StoredImage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/submit")
@RequiredArgsConstructor

public class SubmitController {

    private final ContentMapper contentMapper;
    private final SensitiveWordService sensitiveWordService;
    private final FileStorageService storageService;
    private final StringRedisTemplate redis;
    private final ContentImageMapper contentImageMapper;
    private final ClientIpResolver clientIpResolver;

    private static final int MAX_PER_HOUR = 5;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_BODY_LENGTH = 60_000;
    private static final int MAX_NICKNAME_LENGTH = 50;

    @PostMapping
    @Transactional                                                  // ← 新知识点！
    public R<Void> submit(@RequestParam(required = false) String title,
                        @RequestParam(required = false) String nickname,
                        @RequestParam String body,
                        @RequestParam(value = "files", required = false) List<MultipartFile> files,
                        HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);

        // ① 限流
        String limitKey = "submit:limit:" + ip;
        Long cnt = redis.opsForValue().increment(limitKey);
        if (cnt != null && cnt == 1) redis.expire(limitKey, Duration.ofHours(1));
        if (cnt != null && cnt > MAX_PER_HOUR) throw new BizException("投稿太频繁，请稍后再试");

        // ② 正文校验 + 敏感词（不变）
        String cleanBody = body == null ? "" : body.trim();
        if (cleanBody.isEmpty()) throw new BizException("正文不能为空");
        if (cleanBody.length() > MAX_BODY_LENGTH) throw new BizException("正文最长60000字");
        String t = title == null ? "" : title.trim();
        if (t.length() > MAX_TITLE_LENGTH) throw new BizException("标题最长120字");
        if (sensitiveWordService.contains(t) || sensitiveWordService.contains(cleanBody)) {
            throw new BizException("内容包含敏感词，请修改后提交");
        }

        // ③ 图片可选：最多 3 张，逐张存储
        List<StoredImage> storedList = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            if (files.size() > 3) throw new BizException("最多上传 3 张图片");
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                try {
                    storedList.add(storageService.store(f));   // 每张走魔数/大小/缩略图
                } catch (BizException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BizException("图片处理失败，请重试");
                }
            }
        }

        // ④ 先存话题，拿到 id
        Content c = new Content();
        c.setType(ContentType.POST);
        c.setSource(SourceType.SUBMIT);
        c.setTitle(t);
        c.setBody(cleanBody);
        if (!storedList.isEmpty()) {
            c.setMediaUrl(storedList.get(0).url());            // 第一张作封面（兼容旧逻辑）
            c.setThumbnailUrl(storedList.get(0).thumbnailUrl());
        }
        c.setStatus(ContentStatus.PENDING);
        c.setIp(ip);
        c.setUa(limit(request.getHeader("User-Agent"), 255));
        String cleanNickname = nickname == null ? "" : nickname.trim();
        c.setAuthorLabel(cleanNickname.isBlank()
                ? "游客 #" + ThreadLocalRandom.current().nextInt(1000, 10000)
                : limit(cleanNickname, MAX_NICKNAME_LENGTH));
        contentMapper.insert(c);                               // 注意：insert 后 c.getId() 有值

        // ⑤ 再存图片表（主外键关联）
        for (int i = 0; i < storedList.size(); i++) {
            StoredImage s = storedList.get(i);
            ContentImage img = new ContentImage();
            img.setContentId(c.getId());                       // 关键：用话题的 id 关联
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
