package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.service.UserService;
import com.example.musicrecommendation.web.dto.UserCreateRequest;
import com.example.musicrecommendation.web.dto.UserResponse;
import com.example.musicrecommendation.web.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 관련 REST API를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "사용자 API", description = "사용자 관리 관련 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 전체 사용자 목록 조회
     */
    @GetMapping
    @Operation(summary = "전체 사용자 조회", description = "시스템에 등록된 모든 사용자 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserResponse> responses = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 사용자 조회
     */
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 단건 조회", description = "ID로 특정 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId) {

        return userService.findUserById(userId)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 이메일로 사용자 조회
     */
    @GetMapping("/email/{email}")
    @Operation(summary = "이메일로 사용자 조회", description = "이메일 주소로 사용자를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserResponse> getUserByEmail(
            @Parameter(description = "사용자 이메일", example = "user@example.com")
            @PathVariable String email) {

        return userService.findUserByEmail(email)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 새 사용자 생성
     */
    @PostMapping
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다. (테스트/관리자용)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 중복 등)")
    })
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        try {
            User createdUser = userService.createUser(
                    request.getEmail(),
                    request.getName(),
                    request.getProfileImageUrl(),
                    request.getProvider(),
                    request.getProviderId()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UserResponse.from(createdUser));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 사용자 정보 수정
     */
    @PutMapping("/{userId}")
    @Operation(summary = "사용자 정보 수정", description = "사용자의 이름과 프로필 이미지를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {

        try {
            User updatedUser = userService.updateUserProfile(
                    userId,
                    request.getName(),
                    request.getProfileImageUrl()
            );

            return ResponseEntity.ok(UserResponse.from(updatedUser));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 사용자 삭제
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제", description = "사용자를 시스템에서 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId) {

        try {
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 이메일 중복 확인
     */
    @GetMapping("/check-email/{email}")
    @Operation(summary = "이메일 중복 확인", description = "이메일이 이미 사용 중인지 확인합니다.")
    @ApiResponse(responseCode = "200", description = "확인 완료")
    public ResponseEntity<Boolean> checkEmailExists(
            @Parameter(description = "확인할 이메일", example = "user@example.com")
            @PathVariable String email) {

        boolean exists = userService.isEmailExists(email);
        return ResponseEntity.ok(exists);
    }

    /**
     * 전체 사용자 수 조회
     */
    @GetMapping("/count")
    @Operation(summary = "전체 사용자 수 조회", description = "시스템에 등록된 총 사용자 수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Long> getUserCount() {
        long count = userService.getTotalUserCount();
        return ResponseEntity.ok(count);
    }
}