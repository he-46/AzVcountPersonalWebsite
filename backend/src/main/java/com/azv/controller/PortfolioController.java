package com.azv.controller;

import com.azv.common.R;
import com.azv.entity.Portfolio;
import com.azv.mapper.PortfolioMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioMapper portfolioMapper;

    /** 前台作品集展示（/about 页数据源） */
    @GetMapping
    public R<List<Portfolio>> list() {
        return R.ok(portfolioMapper.selectList(
                new LambdaQueryWrapper<Portfolio>()
                        .eq(Portfolio::getStatus, "APPROVED")     // 只展示已发布
                        .orderByAsc(Portfolio::getSortOrder)));
    }
}