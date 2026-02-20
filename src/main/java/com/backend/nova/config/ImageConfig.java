package com.backend.nova.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageConfig implements WebMvcConfigurer {

    @Value("${file.dir}")
    private String uploadDir;

    @Value("${file.prefix}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 예: /images/** 요청이 들어오면 -> C:/Users/.../ 폴더로 연결
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations("file:" + uploadDir);
    }
}