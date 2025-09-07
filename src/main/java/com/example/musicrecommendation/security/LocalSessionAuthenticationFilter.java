package com.example.musicrecommendation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * LOCAL 세션 기반 인증을 Spring Security에 통합하는 필터
 * 세션에 userId와 authProvider가 있으면 인증된 사용자로 처리
 */
@Component
@Slf4j
public class LocalSessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // 이미 인증된 사용자가 있으면 스킵
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // LOCAL 세션 확인
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            String authProvider = (String) session.getAttribute("authProvider");
            
            if (userId != null && "LOCAL".equals(authProvider)) {
                log.debug("LOCAL 세션 인증 적용: userId={}", userId);
                
                // LOCAL 사용자를 위한 Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId.toString(), // principal
                        null, // credentials
                        List.of(new SimpleGrantedAuthority("ROLE_USER")) // authorities
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("LOCAL 세션 사용자 인증 완료: userId={}", userId);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}