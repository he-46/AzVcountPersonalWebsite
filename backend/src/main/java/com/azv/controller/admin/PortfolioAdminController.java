package com.azv.controller.admin;

import com.azv.common.R;
import com.azv.entity.Portfolio;
import com.azv.mapper.PortfolioMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/portfolio")
@RequiredArgsConstructor
public class PortfolioAdminController {

    private final PortfolioMapper portfolioMapper;

    @GetMapping("/list")
    public R<List<Portfolio>> list() {
        return R.ok(portfolioMapper.selectList(
                new LambdaQueryWrapper<Portfolio>().orderByAsc(Portfolio::getSortOrder)));
    }

    /** 新增（实体直接接收表单参数——对象绑定） */
    @PostMapping
    public R<Void> add(Portfolio p) {
        portfolioMapper.insert(p);
        return R.ok(null);
    }

    /** 修改 */
    @PostMapping("/{id}")
    public R<Void> update(@PathVariable Long id, Portfolio p) {
        p.setId(id);
        portfolioMapper.updateById(p);   // 只更新非 null 字段
        return R.ok(null);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        portfolioMapper.deleteById(id);
        return R.ok(null);
    }
}
