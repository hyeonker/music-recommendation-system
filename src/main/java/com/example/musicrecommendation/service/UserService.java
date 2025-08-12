package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.AuthProvider;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.UserRepository;
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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
        }

        userRepository.deleteById(userId);
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
     * 전체 사용자 수 조회
     *
     * @return 총 사용자 수
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }
}