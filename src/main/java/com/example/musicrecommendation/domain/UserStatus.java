package com.example.musicrecommendation.domain;

/**
 * 사용자 상태 열거형
 */
public enum UserStatus {
    /**
     * 정상 활성 상태
     */
    ACTIVE("정상"),
    
    /**
     * 정지된 상태 (로그인 불가, 활동 제한)
     */
    SUSPENDED("정지"),
    
    /**
     * 삭제된 상태 (탈퇴 처리)
     */
    DELETED("탈퇴");
    
    private final String description;
    
    UserStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}