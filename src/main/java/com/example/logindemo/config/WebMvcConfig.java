// Config 클래스 하나 생성
package com.example.logindemo.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private Path uploadDir;

    // 서버 시작 시 실행되어 uploads 폴더를 자동 생성
    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

        if (Files.notExists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir.toString() + "/");
    }
}
