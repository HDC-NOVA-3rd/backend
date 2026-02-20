package com.backend.nova.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class ImageConfig implements WebMvcConfigurer {

    @Value("${file.dir}")
    private String uploadDir;

    @Value("${file.prefix}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 경로 포맷팅: 끝에 반드시 슬래시가 있도록 처리
        String formattedDir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
        // 리눅스/윈도우 공용 안전한 URI 생성
        String location = new File(formattedDir).toURI().toString();
        // 예: /images/** 요청이 들어오면 -> C:/Users/.../ 폴더로 연결
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations(location);
    }
}