package com.azv.controller;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.entity.enums.SourceType;
import com.azv.mapper.ContentMapper;
import com.azv.security.ClientIpResolver;
import com.azv.service.SensitiveWordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final ContentMapper contentMapper;
    private final SensitiveWordService sensitiveWordService;
    private final StringRedisTemplate redis;
    private final ClientIpResolver clientIpResolver;

    private static final int MAX_LEN = 500;
    private static final int MAX_PER_HOUR = 10;

    @PostMapping
    public R<Void> submit(@RequestBody CommentRequest payload,
                          HttpServletRequest request) {
        Long contentId = payload.contentId();
        String text = payload.text();
        String nickname = payload.nickname();
        // ① 参数校验
        if (contentId == null) throw new BizException("评论的目标不存在");
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) throw new BizException("评论内容不能为空");
        if (clean.length() > MAX_LEN) throw new BizException("评论最长" + MAX_LEN + "字");
        String ip = clientIpResolver.resolve(request);

        // ② 防刷限流（同一 IP 每小时最多 10 条）
        String limitKey = "comment:limit:" + ip;
        Long cnt = redis.opsForValue().increment(limitKey);
        if (cnt != null && cnt == 1) redis.expire(limitKey, Duration.ofHours(1));
        if (cnt != null && cnt > MAX_PER_HOUR) throw new BizException("评论太频繁，请稍后再试");

        // ③ 敏感词过滤（合规第一道闸）
        if (sensitiveWordService.contains(clean)) {
            throw new BizException("内容包含敏感词，请修改后提交");
        }

        // ④ 被评论的内容必须存在且已审核
        Content target = contentMapper.selectById(contentId);
        if (target == null || target.getStatus() != ContentStatus.APPROVED
                || target.getType() == ContentType.COMMENT) {
            throw new BizException("评论的目标不存在");
        }

        // ⑤ 落库：PENDING + 留痕 + 署名
        Content comment = new Content();
        comment.setType(ContentType.COMMENT);
        comment.setSource(SourceType.SUBMIT);
        comment.setParentId(contentId);                        // 关联帖子
        comment.setBody(clean);
        comment.setStatus(ContentStatus.PENDING);              // 先审后发！
        comment.setIp(ip);                                     // 留痕
        comment.setUa(limit(request.getHeader("User-Agent"), 255)); // 留痕
        comment.setAuthorLabel(nickname == null || nickname.isBlank()
                ? "游客 #" + ThreadLocalRandom.current().nextInt(1000, 10000)
                : nickname.trim().substring(0, Math.min(20, nickname.trim().length())));
        contentMapper.insert(comment);
        return R.ok(null);
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.substring(0, Math.min(value.length(), maxLength));
    }

    public record CommentRequest(Long contentId, String text, String nickname) {}

}
