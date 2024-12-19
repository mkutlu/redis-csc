package com.kutlu.rediscsc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.kutlu.rediscsc.repository")
@ComponentScan(basePackages = { "com.kutlu.rediscsc.*" })
@EntityScan(basePackages = "com.kutlu.rediscsc.entity")
public class RedisCscApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisCscApplication.class, args);
    }
}