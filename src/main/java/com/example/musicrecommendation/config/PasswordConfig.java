package com.example.musicrecommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화를 위한 설정 클래스
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 패스워드 인코더
     * - 소금값을 자동으로 생성하여 보안 강화
     * - 해싱 라운드는 기본값 10 사용 (운영 환경에서 적절한 성능/보안 균형)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}