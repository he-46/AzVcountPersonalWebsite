package com.azv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// User.java —— Lombok + MP 注解
@SpringBootApplication
@MapperScan("com.azv.mapper")
public class AzvApplication {
    public static void main(String[] args) {
        SpringApplication.run(AzvApplication.class, args);
    }
}
