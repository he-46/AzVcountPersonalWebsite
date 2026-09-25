package com.azv.controller;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.Content;
import com.azv.entity.enums.ContentStatus;
import com.azv.entity.enums.ContentType;
import com.azv.mapper.ContentMapper;
import com.azv.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/like")
@RequiredArgsConstructor
public class LikeController {

    private final ContentMapper contentMapper;
    private final StringRedisTemplate redis;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/{contentId}")
    @Transactional
    public R<Integer> like(@PathVariable Long contentId, HttpServletRequest request) {
        // ① 内容必须存在且已审核
        Content c = contentMapper.selectById(contentId);
        if (c == null || c.getStatus() != ContentStatus.APPROVED
                || c.getType() == ContentType.COMMENT) {
            throw new BizException("内容不存在");
        }

        // ② IP 判重（Redis SET，原子操作）
        String ip = clientIpResolver.resolve(request);
        String setKey = "like:set:" + contentId;
        Long added = redis.opsForSet().add(setKey, ip);
        if (added == null) {
            throw new BizException("点赞服务暂不可用，请稍后重试");
        }
        if (added == 0) {
            throw new BizException("你已经点过赞了");
        }

        // ③ 数据库原子 +1；任何数据库失败都撤销 Redis 判重，允许用户重试。
        try {
            if (contentMapper.incrementLikeCount(contentId) != 1) {
                throw new BizException("内容不存在");
            }
            Integer count = contentMapper.selectLikeCount(contentId);
            if (count == null) throw new BizException("内容不存在");
            return R.ok(count);
        } catch (RuntimeException exception) {
            try {
                redis.opsForSet().remove(setKey, ip);
            } catch (RuntimeException ignored) {
                // 保留原始数据库异常，Redis 可由运维清理。
            }
            throw exception;
        }
    }

}
