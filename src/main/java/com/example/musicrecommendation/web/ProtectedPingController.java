package com.example.musicrecommendation.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/protected")
public class ProtectedPingController {

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        // 인증 성공해서 여기까지 왔다는 뜻
        return ResponseEntity.ok(Map.of("ok", true, "message", "authenticated ping"));
    }
}
