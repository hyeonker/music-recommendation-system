package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.AuthProvider;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.UserRepository;
import com.example.musicrecommendation.util.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 관련 비즈니스 로직을 담당하는 서비스
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 모든 사용자 조회
     *
     * @return 전체 사용자 목록
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * ID로 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 Optional (없으면 empty)
     */
    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 사용자 이메일
     * @return 사용자 Optional (없으면 empty)
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * OAuth2 제공자와 제공자 ID로 사용자 조회
     * 소셜 로그인 시 기존 사용자 확인용
     *
     * @param provider OAuth2 제공자
     * @param providerId 제공자에서의 사용자 ID
     * @return 사용자 Optional (없으면 empty)
     */
    public Optional<User> findUserByProviderAndProviderId(AuthProvider provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    /**
     * 새 사용자 생성 (OAuth2 로그인용)
     *
     * @param email 이메일
     * @param name 실명
     * @param profileImageUrl 프로필 이미지 URL
     * @param provider OAuth2 제공자
     * @param providerId 제공자에서의 사용자 ID
     * @return 생성된 사용자
     */
    @Transactional
    public User createUser(String email, String name, String profileImageUrl,
                           AuthProvider provider, String providerId) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다: " + email);
        }

        // 동일한 소셜 계정으로 중복 가입 체크
        if (userRepository.existsByProviderAndProviderId(provider, providerId)) {
            throw new IllegalArgumentException("이미 가입된 소셜 계정입니다: " + provider + " - " + providerId);
        }

        User newUser = new User(email, name, profileImageUrl, provider, providerId);
        return userRepository.save(newUser);
    }

    /**
     * 아이디/비밀번호 기반 회원가입
     *
     * @param email 이메일 (아이디 역할)
     * @param name 실명
     * @param password 평문 비밀번호
     * @return 생성된 사용자
     */
    @Transactional
    public User createUserWithPassword(String email, String name, String password) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다: " + email);
        }

        // 비밀번호 유효성 검사
        validatePassword(password);

        // 이름 유효성 검사
        validateName(name, null);

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        User newUser = new User(email, name.trim(), encodedPassword);
        return userRepository.save(newUser);
    }

    /**
     * 로그인 검증
     *
     * @param email 이메일
     * @param password 평문 비밀번호
     * @return 인증 성공 시 사용자 정보, 실패 시 empty
     */
    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // LOCAL 로그인 사용자만 비밀번호 인증
            if (user.getProvider() == AuthProvider.LOCAL && 
                user.getPasswordHash() != null &&
                passwordEncoder.matches(password, user.getPasswordHash())) {
                
                // 계정 상태 확인
                if (!user.isActive()) {
                    throw new IllegalArgumentException("비활성화된 계정입니다.");
                }
                
                return Optional.of(user);
            }
        }
        
        return Optional.empty();
    }

    /**
     * 비밀번호 유효성 검사
     */
    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        String trimmedPassword = password.trim();

        if (trimmedPassword.length() < 6) {
            throw new IllegalArgumentException("비밀번호는 최소 6자 이상이어야 합니다.");
        }

        if (trimmedPassword.length() > 100) {
            throw new IllegalArgumentException("비밀번호는 최대 100자까지 가능합니다.");
        }

        // 보안 강화: 간단한 패턴 체크
        boolean hasLetter = trimmedPassword.matches(".*[a-zA-Z].*");
        boolean hasDigit = trimmedPassword.matches(".*\\d.*");
        
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("비밀번호는 영문자와 숫자를 모두 포함해야 합니다.");
        }
    }

    /**
     * 사용자 프로필 업데이트
     * OAuth2 로그인 시 변경된 정보 업데이트용
     *
     * @param userId 사용자 ID
     * @param name 새로운 이름
     * @param profileImageUrl 새로운 프로필 이미지 URL
     * @return 업데이트된 사용자
     */
    @Transactional
    public User updateUserProfile(Long userId, String name, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.updateProfile(name, profileImageUrl);
        return userRepository.save(user);
    }

    /**
     * 사용자 삭제
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        System.out.println("사용자 완전 삭제 시작 - 사용자 ID: " + userId);
        
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
        }

        // 사용자 정보 로깅
        userRepository.findById(userId).ifPresent(user -> {
            System.out.println("삭제할 사용자: " + user.getName() + " (" + user.getEmail() + ")");
        });

        userRepository.deleteById(userId);
        System.out.println("사용자 완전 삭제 완료 - 사용자 ID: " + userId);
    }

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 존재하면 true, 없으면 false
     */
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * 닉네임 중복 확인 (특정 사용자 제외)
     *
     * @param name 확인할 닉네임
     * @param excludeUserId 제외할 사용자 ID (자신의 ID)
     * @return 중복되면 true, 없으면 false
     */
    public boolean isNameExistsExcludingUser(String name, Long excludeUserId) {
        // 기본적인 null/empty 체크만 수행 (상세 유효성 검사는 별도로)
        if (name == null || name.trim().isEmpty()) {
            return false; // 빈 이름은 중복이 아니라 유효하지 않은 이름
        }
        
        String trimmedName = name.trim();
        
        if (excludeUserId == null) {
            return userRepository.existsByName(trimmedName);
        }
        return userRepository.existsByNameAndIdNot(trimmedName, excludeUserId);
    }
    
    /**
     * 닉네임 유효성 검사만 수행 (중복 체크와 분리)
     *
     * @param name 확인할 닉네임
     * @param excludeUserId 제외할 사용자 ID (관리자는 예외)
     */
    public void validateNameOnly(String name, Long excludeUserId) {
        validateName(name, excludeUserId);
    }
    
    /**
     * 닉네임 유효성 검사
     *
     * @param name 확인할 닉네임
     * @param excludeUserId 제외할 사용자 ID (관리자는 예외)
     */
    private void validateName(String name, Long excludeUserId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요");
        }
        
        String trimmedName = name.trim();
        
        // 길이 체크 (2-15자)
        if (trimmedName.length() < 2) {
            throw new IllegalArgumentException("닉네임은 최소 2자 이상이어야 합니다");
        }
        if (trimmedName.length() > 15) {
            throw new IllegalArgumentException("닉네임은 최대 15자까지 가능합니다");
        }
        
        // 보안 검증 사용
        SecurityUtils.ValidationResult result = SecurityUtils.validateInput(trimmedName, "닉네임");
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getError());
        }
        
        // 관리자는 금지된 단어 체크 제외
        if (excludeUserId != null && isAdmin(excludeUserId)) {
            return;
        }
        
        // 금지된 단어 체크
        String[] forbiddenWords = {
            "운영자", "관리자", "어드민", "admin", "administrator",
            "매니저", "manager", "스태프", "staff", "마스터", "master",
            "시스템", "system", "루트", "root", "슈퍼", "super",
            "모더레이터", "moderator", "mod", "개발자", "developer",
            "dev", "테스트", "test", "공지", "공식", "official"
        };
        
        String lowerName = trimmedName.toLowerCase();
        for (String word : forbiddenWords) {
            if (lowerName.contains(word.toLowerCase())) {
                throw new IllegalArgumentException("사용할 수 없는 닉네임입니다");
            }
        }
    }

    /**
     * 전체 사용자 수 조회
     *
     * @return 총 사용자 수
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }
    
    /**
     * 관리자 권한 확인 - 보안 강화 버전
     * 특정 사용자 ID를 관리자로 설정 (변경 불가능한 고유 식별자 사용)
     *
     * @param userId 사용자 ID
     * @return 관리자면 true, 아니면 false
     */
    public boolean isAdmin(Long userId) {
        // 관리자 계정의 실제 사용자 ID (데이터베이스에서 확인 필요)
        // 이 값은 절대 변경되지 않는 PRIMARY KEY
        return userId != null && userId.equals(1L); // 관리자 계정의 실제 ID
    }
    
    /**
     * 관리자 권한 확인 (이메일 + provider 기반) - 더 안전
     *
     * @param email 사용자 이메일
     * @return 관리자면 true, 아니면 false
     */
    public boolean isAdminByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        
        Optional<User> user = findUserByEmail(email);
        if (user.isPresent()) {
            User u = user.get();
            System.out.println("관리자 권한 확인 - 이메일: " + email + ", 사용자 ID: " + u.getId() + ", 이름: " + u.getName());
            
            // 관리자 계정의 실제 정보로 검증 (이메일 + provider + 닉네임)
            boolean isTargetEmail = email.equals(u.getEmail()); // 실제 이메일 확인 필요
            boolean isTargetName = "운영자".equals(u.getName()); // 관리자 닉네임 확인
            
            // 추가 보안: 사용자 ID도 함께 확인
            boolean isTargetId = u.getId().equals(1L);
            
            System.out.println("권한 체크 - 이메일 일치: " + isTargetEmail + ", 이름 일치: " + isTargetName + ", ID 일치: " + isTargetId);
            
            return isTargetEmail && isTargetName && isTargetId;
        }
        System.out.println("사용자를 찾을 수 없음 - 이메일: " + email);
        return false;
    }
    
    /**
     * OAuth 정보를 포함한 관리자 권한 확인 (최고 보안)
     *
     * @param email OAuth 이메일
     * @param provider OAuth 제공자
     * @param providerId OAuth 제공자 ID
     * @return 관리자면 true, 아니면 false
     */
    public boolean isAdminByOAuth(String email, AuthProvider provider, String providerId) {
        if (email == null || provider == null || providerId == null) return false;
        
        Optional<User> user = findUserByProviderAndProviderId(provider, providerId);
        if (user.isPresent()) {
            User u = user.get();
            // 모든 정보가 일치해야 관리자로 인정
            boolean isValidName = "운영자".equals(u.getName());
            return u.getId().equals(1L) && 
                   email.equals(u.getEmail()) && 
                   isValidName &&
                   provider.equals(u.getProvider()) &&
                   providerId.equals(u.getProviderId());
        }
        return false;
    }

    /**
     * 사용자 계정 정지
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        if (user.isDeleted()) {
            throw new IllegalArgumentException("이미 탈퇴한 사용자입니다.");
        }
        
        user.suspend();
        userRepository.save(user);
    }

    /**
     * 사용자 계정 활성화 (정지 해제)
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void activateUser(Long userId) {
        System.out.println("사용자 활성화 시작 - 사용자 ID: " + userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        System.out.println("활성화할 사용자: " + user.getName() + ", 현재 상태: " + user.getStatus());
        
        // 탈퇴한 사용자도 활성화 가능하도록 제한 제거
        user.activate();
        userRepository.save(user);
        
        System.out.println("사용자 활성화 완료 - 새로운 상태: " + user.getStatus());
    }

    /**
     * 사용자 계정 탈퇴 처리 (소프트 삭제)
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        System.out.println("탈퇴 처리 시작 - 사용자 ID: " + userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        System.out.println("찾은 사용자: " + user.getName() + ", 현재 상태: " + user.getStatus());
        
        if (user.isDeleted()) {
            throw new IllegalArgumentException("이미 탈퇴한 사용자입니다.");
        }
        
        user.delete();
        System.out.println("상태 변경 후: " + user.getStatus());
        
        userRepository.save(user);
        System.out.println("탈퇴 처리 완료 - 사용자 ID: " + userId);
    }
}