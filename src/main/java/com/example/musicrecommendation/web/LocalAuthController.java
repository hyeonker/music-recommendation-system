package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.service.UserService;
import com.example.musicrecommendation.web.dto.AuthResponse;
import com.example.musicrecommendation.web.dto.LoginRequest;
import com.example.musicrecommendation.web.dto.SignupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 아이디/비밀번호 기반 로컬 인증 컨트롤러
 * OAuth2와 분리된 전통적인 로그인/회원가입 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/local")
@Tag(name = "로컬 인증 API", description = "아이디/비밀번호 기반 회원가입 및 로그인 API")
public class LocalAuthController {

    private final UserService userService;

    public LocalAuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 아이디/비밀번호 기반 회원가입
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 새 계정을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 오류 또는 중복 이메일"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        try {
            // 비밀번호 확인 검증
            if (!request.isPasswordMatching()) {
                return ResponseEntity.badRequest()
                    .body(AuthResponse.failure("비밀번호가 일치하지 않습니다."));
            }

            // 회원가입 처리
            User newUser = userService.createUserWithPassword(
                request.getEmail(), 
                request.getName(), 
                request.getPassword()
            );

            log.info("새 사용자 회원가입 완료: {}", newUser.getEmail());
            
            return ResponseEntity.ok(
                AuthResponse.success("회원가입이 완료되었습니다.", newUser)
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패 - 입력값 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(AuthResponse.failure(e.getMessage()));
                
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(AuthResponse.failure("회원가입 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 아이디/비밀번호 기반 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 입력값"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 인증 시도
            Optional<User> userOpt = userService.authenticateUser(
                request.getEmail(), 
                request.getPassword()
            );

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // 세션에 사용자 정보 저장 (간단한 세션 기반 인증)
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("userId", user.getId());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userName", user.getName());
                session.setAttribute("authProvider", "LOCAL");
                session.setMaxInactiveInterval(7200); // 2시간 세션 유지
                
                log.info("사용자 로그인 성공: {} (세션 ID: {})", user.getEmail(), session.getId());
                
                return ResponseEntity.ok(
                    AuthResponse.success("로그인에 성공했습니다.", user)
                );
                
            } else {
                log.warn("로그인 실패 - 잘못된 인증 정보: {}", request.getEmail());
                return ResponseEntity.status(401)
                    .body(AuthResponse.failure("이메일 또는 비밀번호가 올바르지 않습니다."));
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("로그인 실패 - 계정 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                .body(AuthResponse.failure(e.getMessage()));
                
        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(AuthResponse.failure("로그인 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 이메일 중복 확인
     */
    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 확인", description = "회원가입 전 이메일 중복을 확인합니다.")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        try {
            boolean exists = userService.isEmailExists(email);
            return ResponseEntity.ok(exists);
            
        } catch (Exception e) {
            log.error("이메일 중복 확인 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(false);
        }
    }

    /**
     * 현재 로그인한 사용자 정보 조회 (LOCAL 인증)
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<AuthResponse> getMyInfo(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            
            log.info("=== /api/auth/local/me 호출 ===");
            log.info("세션 존재 여부: {}", session != null);
            if (session != null) {
                log.info("세션 ID: {}", session.getId());
                log.info("세션 속성들: userId={}, authProvider={}", 
                    session.getAttribute("userId"), session.getAttribute("authProvider"));
            }
            
            if (session == null) {
                log.warn("세션이 없어서 로그인 필요 응답");
                return ResponseEntity.status(401)
                    .body(AuthResponse.failure("로그인이 필요합니다."));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            String authProvider = (String) session.getAttribute("authProvider");
            
            if (userId == null || !"LOCAL".equals(authProvider)) {
                return ResponseEntity.status(401)
                    .body(AuthResponse.failure("유효하지 않은 세션입니다."));
            }
            
            Optional<User> userOpt = userService.findUserById(userId);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return ResponseEntity.ok(
                    AuthResponse.success("사용자 정보 조회 성공", user)
                );
            } else {
                // 세션 무효화
                session.invalidate();
                return ResponseEntity.status(401)
                    .body(AuthResponse.failure("사용자를 찾을 수 없습니다."));
            }
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(AuthResponse.failure("사용자 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 로그아웃합니다.")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                String userEmail = (String) session.getAttribute("userEmail");
                log.info("사용자 로그아웃: {} (세션 ID: {})", userEmail, session.getId());
                session.invalidate();
            }
            
            return ResponseEntity.ok(
                AuthResponse.success("로그아웃되었습니다.", null)
            );
            
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(AuthResponse.failure("로그아웃 처리 중 오류가 발생했습니다."));
        }
    }
}