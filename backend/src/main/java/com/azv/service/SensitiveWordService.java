package com.azv.service;

import com.azv.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    // volatile：刷新词库时，其他线程立即可见新引用
    private volatile List<String> words = new ArrayList<>();

    /** Bean 初始化后执行：启动时加载词库 */
    @PostConstruct
    public void init() {
        refresh();
    }

    /** 刷新词库（阶段 4 后台增删词后调用） */
    public void refresh() {
        List<String> fresh = sensitiveWordMapper.selectList(null).stream()
                .map(w -> w.getWord())
                .filter(w -> w != null && !w.isEmpty())
                .toList();
        this.words = fresh;      // 换引用，不是改集合
    }

    /** 返回命中的敏感词列表（空 = 通过） */
    public List<String> filter(String text) {
        if (text == null || text.isEmpty()) return List.of();
        List<String> hit = new ArrayList<>();
        for (String word : words) {
            if (text.contains(word)) hit.add(word);
        }
        return hit;
    }

    /** 是否含敏感词（评论/投稿提交前调用） */
    public boolean contains(String text) {
        return !filter(text).isEmpty();
    }
}