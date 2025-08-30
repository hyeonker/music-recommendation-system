package com.example.musicrecommendation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 데이터 접근을 위한 Repository 인터페이스
 * Spring Data JPA가 자동으로 구현체를 생성
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 찾기
     * OAuth2 로그인 시 기존 사용자 확인용
     *
     * @param email 사용자 이메일
     * @return 사용자 Optional (없으면 empty)
     */
    Optional<User> findByEmail(String email);

    /**
     * OAuth2 제공자와 제공자 ID로 사용자 찾기
     * 동일한 소셜 계정으로 재로그인 시 기존 사용자 확인용
     *
     * @param provider OAuth2 제공자 (GOOGLE, GITHUB 등)
     * @param providerId 제공자에서의 사용자 고유 ID
     * @return 사용자 Optional (없으면 empty)
     */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * 이메일 존재 여부 확인
     * 회원가입 시 이메일 중복 검사용
     *
     * @param email 확인할 이메일
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByEmail(String email);

    /**
     * OAuth2 제공자와 제공자 ID로 사용자 존재 여부 확인
     *
     * @param provider OAuth2 제공자
     * @param providerId 제공자에서의 사용자 ID
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);
    
    /**
     * 닉네임 존재 여부 확인
     *
     * @param name 확인할 닉네임
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByName(String name);
    
    /**
     * 특정 사용자를 제외한 닉네임 존재 여부 확인
     *
     * @param name 확인할 닉네임
     * @param excludeId 제외할 사용자 ID
     * @return 중복되면 true, 없으면 false
     */
    boolean existsByNameAndIdNot(String name, Long excludeId);
}