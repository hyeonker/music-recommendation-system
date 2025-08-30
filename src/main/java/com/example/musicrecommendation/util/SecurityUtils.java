package com.example.musicrecommendation.util;

import org.springframework.web.util.HtmlUtils;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;

/**
 * 보안 관련 유틸리티 클래스
 * XSS, SQL Injection 등의 공격을 방어합니다.
 */
public class SecurityUtils {
    
    // SQL 인젝션 탐지 패턴들
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|UNION|FROM|WHERE|ORDER|GROUP|HAVING)\\b)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(--|#|/\\*|\\*/)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("('.*'|\".*\")", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\b(EXEC|EXECUTE|CAST|CONVERT|SUBSTRING|ASCII|CHAR|NCHAR|UPPER|LOWER)\\b)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(;|\\|\\||&&|\\bOR\\b|\\bAND\\b)", Pattern.CASE_INSENSITIVE)
    );
    
    // XSS 탐지 패턴들
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
        Pattern.compile("<script[\\s\\S]*?>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:\\w+/\\w+;base64,", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<style[\\s\\S]*?>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<(iframe|object|embed|applet|form)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<meta[\\s\\S]*?http-equiv[\\s\\S]*?refresh", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(expression|behavior|binding|eval|fromCharCode)", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * SQL 인젝션 패턴 탐지
     *
     * @param input 검사할 문자열
     * @return SQL 인젝션 패턴이 발견되면 true
     */
    public static boolean detectSqlInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        return SQL_INJECTION_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(input).find());
    }
    
    /**
     * XSS 패턴 탐지
     *
     * @param input 검사할 문자열
     * @return XSS 패턴이 발견되면 true
     */
    public static boolean detectXss(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        return XSS_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(input).find());
    }
    
    /**
     * 입력값 전체 보안 검증
     *
     * @param input 검사할 문자열
     * @param fieldName 필드명 (에러 메시지용)
     * @return ValidationResult 객체
     */
    public static ValidationResult validateInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            return new ValidationResult(true, "", null);
        }
        
        // XSS 탐지
        if (detectXss(input)) {
            return new ValidationResult(false, "", 
                fieldName + "에 허용되지 않는 스크립트 코드가 포함되어 있습니다.");
        }
        
        // SQL 인젝션 탐지
        if (detectSqlInjection(input)) {
            return new ValidationResult(false, "", 
                fieldName + "에 허용되지 않는 데이터베이스 명령어가 포함되어 있습니다.");
        }
        
        // HTML 이스케이프 적용
        String sanitized = HtmlUtils.htmlEscape(input.trim());
        
        return new ValidationResult(true, sanitized, null);
    }
    
    /**
     * 리뷰 텍스트 전용 검증 (더 관대한 규칙)
     *
     * @param text 리뷰 텍스트
     * @return ValidationResult 객체
     */
    public static ValidationResult validateReviewText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ValidationResult(true, "", null);
        }
        
        // 스크립트 태그만 엄격하게 차단
        Pattern scriptPattern = Pattern.compile(
            "<script[\\s\\S]*?>[\\s\\S]*?</script>|javascript:|vbscript:|on\\w+\\s*=", 
            Pattern.CASE_INSENSITIVE
        );
        
        if (scriptPattern.matcher(text).find()) {
            return new ValidationResult(false, "", 
                "리뷰에 허용되지 않는 스크립트 코드가 포함되어 있습니다.");
        }
        
        // 기본 HTML 이스케이프만 적용
        String sanitized = HtmlUtils.htmlEscape(text.trim());
        
        return new ValidationResult(true, sanitized, null);
    }
    
    /**
     * 안전한 문자열만 허용 (알파벳, 숫자, 일부 특수문자, 한글)
     *
     * @param str 정제할 문자열
     * @return 정제된 문자열
     */
    public static String sanitizeText(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "";
        }
        
        // 허용되는 문자: 한글, 영문, 숫자, 공백, 기본 특수문자
        return str.replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s\\-_.,!?()\\[\\]{}@#$%&*+=|\\\\:;\"'<>/~`]", "").trim();
    }
    
    /**
     * 검증 결과를 담는 클래스
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String sanitized;
        private final String error;
        
        public ValidationResult(boolean valid, String sanitized, String error) {
            this.valid = valid;
            this.sanitized = sanitized;
            this.error = error;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getSanitized() {
            return sanitized;
        }
        
        public String getError() {
            return error;
        }
    }
}