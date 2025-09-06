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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
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
    
    /**
     * 관리자 전용: 모든 사용자 조회 (상태 포함)
     */
    @GetMapping("/all")
    @Operation(summary = "관리자용 전체 사용자 조회", description = "관리자가 모든 사용자 목록을 상태와 함께 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<UserResponse>> getAllUsersForAdmin(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = extractEmailFromOAuth2User(oauth2User.getAttributes());
        if (!userService.isAdminByEmail(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<User> users = userService.findAllUsers();
        List<UserResponse> responses = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
    
    
    /**
     * 닉네임 중복 확인
     */
    @GetMapping("/check-name/{name}")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임이 이미 사용 중인지 확인합니다.")
    @ApiResponse(responseCode = "200", description = "확인 완료")
    public ResponseEntity<Map<String, Object>> checkNameExists(
            @Parameter(description = "확인할 닉네임", example = "뮤직러버")
            @PathVariable String name,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        try {
            // 현재 사용자의 ID를 가져오기
            Long currentUserId = null;
            if (oauth2User != null) {
                String email = extractEmailFromOAuth2User(oauth2User.getAttributes());
                if (email != null) {
                    currentUserId = userService.findUserByEmail(email)
                            .map(User::getId)
                            .orElse(null);
                }
            }
            
            // 운영자 권한 확인 - ID가 1이고 이름이 "운영자"인 경우 항상 허용
            if (currentUserId != null && userService.isAdmin(currentUserId)) {
                return ResponseEntity.ok(Map.of("exists", false, "isAdmin", true));
            }
            
            boolean exists = userService.isNameExistsExcludingUser(name, currentUserId);
            return ResponseEntity.ok(Map.of("exists", exists));
            
        } catch (IllegalArgumentException e) {
            // 유효성 검사 실패 시 400 에러와 메시지 반환
            return ResponseEntity.badRequest().body(Map.of(
                "exists", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 현재 로그인한 사용자 정보 조회 - OAuth2 + 로컬 로그인 지원
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        // 1. OAuth2 로그인 확인
        if (oauth2User != null) {
            String email = extractEmailFromOAuth2User(oauth2User.getAttributes());
            if (email == null) {
                return ResponseEntity.badRequest().build();
            }
            
            return userService.findUserByEmail(email)
                    .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                    .orElse(ResponseEntity.notFound().build());
        }
        
        // 2. 로컬 로그인 확인
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            String authProvider = (String) session.getAttribute("authProvider");
            
            if (userId != null && "LOCAL".equals(authProvider)) {
                return userService.findUserById(userId)
                        .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                        .orElse(ResponseEntity.notFound().build());
            }
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    /**
     * 현재 로그인한 사용자의 기본 정보 수정
     */
    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 이름을 수정합니다.")
    public ResponseEntity<UserResponse> updateMyInfo(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = extractEmailFromOAuth2User(oauth2User.getAttributes());
        if (email == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String newName = request.get("name");
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            
            // 운영자가 아닌 경우에만 닉네임 중복 체크 (자신은 제외)
            if (!userService.isAdmin(user.getId()) && userService.isNameExistsExcludingUser(newName.trim(), user.getId())) {
                return ResponseEntity.badRequest().build();
            }
                    
            User updatedUser = userService.updateUserProfile(
                    user.getId(),
                    newName.trim(),
                    user.getProfileImageUrl()
            );
            
            return ResponseEntity.ok(UserResponse.from(updatedUser));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private String extractEmailFromOAuth2User(Map<String, Object> attrs) {
        // Google 로그인의 경우
        if (attrs.containsKey("sub")) {
            return (String) attrs.get("email");
        }
        // Kakao 로그인의 경우  
        else if (attrs.containsKey("id")) {
            Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
            if (account != null) {
                return (String) account.get("email");
            }
        }
        // Naver 로그인의 경우
        else if (attrs.containsKey("response")) {
            Map<String, Object> response = (Map<String, Object>) attrs.get("response");
            if (response != null) {
                return (String) response.get("email");
            }
        }
        
        return null;
    }

    /**
     * 사용자 계정 정지 (관리자용)
     */
    @PostMapping("/{userId}/suspend")
    @Operation(summary = "사용자 계정 정지", description = "관리자가 사용자 계정을 정지합니다.")
    public ResponseEntity<Map<String, String>> suspendUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = extractEmailFromOAuth2User(oauth2User.getAttributes());
        if (!userService.isAdminByEmail(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            userService.suspendUser(userId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(Map.of("message", "사용자가 정지되었습니다."));
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 스택 트레이스 출력
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 사용자 계정 활성화 (관리자용)
     */
    @PostMapping("/{userId}/activate")
    @Operation(summary = "사용자 계정 활성화", description = "관리자가 정지된 사용자 계정을 활성화합니다.")
    public ResponseEntity<Map<String, String>> activateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = extractEmailFromOAuth2User(oauth2User.getAttributes());
        if (!userService.isAdminByEmail(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            userService.activateUser(userId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(Map.of("message", "사용자가 활성화되었습니다."));
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 스택 트레이스 출력
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 사용자 계정 탈퇴 처리 (관리자용)
     */
    @PostMapping("/{userId}/delete")
    @Operation(summary = "사용자 계정 탈퇴", description = "관리자가 사용자 계정을 탈퇴 처리합니다.")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = extractEmailFromOAuth2User(oauth2User.getAttributes());
        if (!userService.isAdminByEmail(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            userService.deleteUser(userId); // 하드 삭제로 변경
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(Map.of("message", "사용자가 완전히 삭제되었습니다."));
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 스택 트레이스 출력
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body(Map.of("error", e.getMessage()));
        }
    }
}