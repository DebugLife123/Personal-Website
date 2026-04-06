package com.wang.website;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wang.website.mapper") // 必须指向你的 mapper 包路径
public class PersonalWebsiteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalWebsiteBackendApplication.class, args);
    }

}
