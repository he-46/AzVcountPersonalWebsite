package com.azv.controller.admin;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.SensitiveWord;
import com.azv.mapper.SensitiveWordMapper;
import com.azv.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/words")
@RequiredArgsConstructor
public class WordAdminController {

    private final SensitiveWordMapper wordMapper;
    private final SensitiveWordService sensitiveWordService;

    @GetMapping("/list")
    public R<List<SensitiveWord>> list() {
        return R.ok(wordMapper.selectList(null));
    }

    /** 新增敏感词：入库 + 内存刷新（实时生效，不用重启） */
    @PostMapping
    public R<Void> add(@RequestParam String word) {
        if (word == null || word.trim().isEmpty()) throw new BizException("敏感词不能为空");
        SensitiveWord w = new SensitiveWord();
        w.setWord(word.trim());
        try {
            wordMapper.insert(w);
        } catch (DuplicateKeyException e) {
            throw new BizException("该敏感词已存在");   // UNIQUE 约束冲突转业务提示
        }
        sensitiveWordService.refresh();   // 关键：刷新内存词库，立即生效
        return R.ok(null);
    }

    /** 删除敏感词 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wordMapper.deleteById(id);
        sensitiveWordService.refresh();
        return R.ok(null);
    }
}