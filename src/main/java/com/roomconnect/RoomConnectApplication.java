package com.roomconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRedisRepositories(enabled = false)
public class RoomConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomConnectApplication.class, args);
    }
}
