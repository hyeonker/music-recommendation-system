package com.example.musicrecommendation.web;

import com.example.musicrecommendation.web.dto.ProfileDto;
import com.example.musicrecommendation.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService service;

    @GetMapping("/{id}/profile")
    public ResponseEntity<ProfileDto> get(@PathVariable Long id,
                                          @AuthenticationPrincipal Object principal) {
        // TODO: principal 검사(자기 자신만 접근) 필요 시 추가
        return ResponseEntity.ok(service.getOrInit(id));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ProfileDto> put(@PathVariable Long id,
                                          @RequestBody ProfileDto body,
                                          @AuthenticationPrincipal Object principal) {
        body.setUserId(id);
        return ResponseEntity.ok(service.save(id, body));
    }
}
