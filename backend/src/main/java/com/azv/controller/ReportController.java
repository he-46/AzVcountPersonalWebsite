package com.azv.controller;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.Report;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.entity.enums.ReportStatus;
import com.azv.mapper.ContentMapper;
import com.azv.mapper.ReportMapper;
import com.azv.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ContentMapper contentMapper;
    private final ReportMapper reportMapper;
    private final StringRedisTemplate redis;
    private final ClientIpResolver clientIpResolver;

    @PostMapping
    public R<Void> report(@RequestParam Long contentId,
                          @RequestParam String reason,
                          HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty()) throw new BizException("请填写举报原因");
        if (cleanReason.length() > 255) throw new BizException("举报原因最长255字");

        // ① 防刷限流（每小时最多 10 次举报）
        String limitKey = "report:limit:" + ip;
        Long cnt = redis.opsForValue().increment(limitKey);
        if (cnt != null && cnt == 1) redis.expire(limitKey, Duration.ofHours(1));
        if (cnt != null && cnt > 10) throw new BizException("举报太频繁，请稍后再试");

        // ② 内容必须存在（举报的对象得是真的）
        Content c = contentMapper.selectById(contentId);
        if (c == null || c.getStatus() != ContentStatus.APPROVED
                || c.getType() == ContentType.COMMENT) {
            throw new BizException("内容不存在");
        }

        // ③ 落库 OPEN + 留痕（站长后台待处理）
        Report r = new Report();
        r.setContentId(contentId);
        r.setReason(cleanReason);
        r.setReporterIp(ip);
        r.setStatus(ReportStatus.OPEN);
        reportMapper.insert(r);
        return R.ok(null);
    }

}
