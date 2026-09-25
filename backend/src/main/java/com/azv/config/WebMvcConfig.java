package com.azv.config;

import com.azv.security.AdminInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;

    /** 上传目录（与 application.yml 的 upload.dir 同一配置源） */
    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    public WebMvcConfig(AdminInterceptor adminInterceptor) {
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");   // 只拦后台
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 图片访问通道：/uploads/** → 磁盘 uploads 目录
        // 关键：file: 前缀 + 目录末尾必须带 /（否则路径拼接错误）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
