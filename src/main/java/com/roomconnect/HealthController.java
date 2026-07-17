package com.roomconnect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");

        try {
            if (jdbcTemplate != null) {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                status.put("database", "UP");
            } else {
                status.put("database", "UNKNOWN");
            }
        } catch (Exception e) {
            status.put("database", "DOWN (" + e.getMessage() + ")");
        }

        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().get("ping");
                status.put("redis", "UP");
            } else {
                status.put("redis", "UNKNOWN");
            }
        } catch (Exception e) {
            status.put("redis", "DOWN (" + e.getMessage() + ")");
        }

        return ResponseEntity.ok(status);
    }
}
