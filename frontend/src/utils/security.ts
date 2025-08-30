// XSS 및 SQL 인젝션 방어를 위한 보안 유틸리티

/**
 * XSS 공격을 방지하기 위한 HTML 이스케이프
 */
export const escapeHtml = (str: string): string => {
  if (!str) return '';
  
  const htmlEscapeMap: { [key: string]: string } = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '/': '&#x2F;',
    '`': '&#x60;',
    '=': '&#x3D;',
  };
  
  return str.replace(/[&<>"'`=/]/g, (match) => htmlEscapeMap[match]);
};

/**
 * SQL 인젝션 패턴 탐지
 */
export const detectSqlInjection = (str: string): boolean => {
  if (!str) return false;
  
  const sqlPatterns = [
    // SQL 키워드
    /(\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|UNION|FROM|WHERE|ORDER|GROUP|HAVING)\b)/i,
    // SQL 주석
    /(--|\#|\/\*|\*\/)/,
    // SQL 문자열 탈출
    /('.*'|".*")/,
    // SQL 함수
    /(\b(EXEC|EXECUTE|CAST|CONVERT|SUBSTRING|ASCII|CHAR|NCHAR|UPPER|LOWER|LEN|LEFT|RIGHT|REPLACE|REVERSE|STUFF|LTRIM|RTRIM|PATINDEX|CHARINDEX)\b)/i,
    // 위험한 패턴
    /(;|\|\||&&|\bOR\b|\bAND\b)/i,
    // 스크립트 태그
    /(<script|<\/script|javascript:|vbscript:|onload|onerror|onclick|onmouseover)/i,
  ];
  
  return sqlPatterns.some(pattern => pattern.test(str));
};

/**
 * XSS 패턴 탐지
 */
export const detectXss = (str: string): boolean => {
  if (!str) return false;
  
  const xssPatterns = [
    // 스크립트 태그
    /<script[\s\S]*?>[\s\S]*?<\/script>/gi,
    // 이벤트 핸들러
    /on\w+\s*=/gi,
    // Javascript 프로토콜
    /javascript:/gi,
    // VBScript 프로토콜
    /vbscript:/gi,
    // Data URL with base64
    /data:\w+\/\w+;base64,/gi,
    // Style 태그
    /<style[\s\S]*?>[\s\S]*?<\/style>/gi,
    // iframe, object, embed
    /<(iframe|object|embed|applet|form)/gi,
    // meta refresh
    /<meta[\s\S]*?http-equiv[\s\S]*?refresh/gi,
    // 위험한 속성들
    /(expression|behavior|binding|eval|fromCharCode)/gi,
  ];
  
  return xssPatterns.some(pattern => pattern.test(str));
};

/**
 * 입력값 전체 보안 검증
 */
export const validateInput = (input: string, fieldName: string = 'Input'): { 
  isValid: boolean; 
  sanitized: string; 
  error?: string 
} => {
  if (!input) {
    return { isValid: true, sanitized: '' };
  }
  
  // XSS 탐지
  if (detectXss(input)) {
    return {
      isValid: false,
      sanitized: '',
      error: `${fieldName}에 허용되지 않는 스크립트 코드가 포함되어 있습니다.`
    };
  }
  
  // SQL 인젝션 탐지
  if (detectSqlInjection(input)) {
    return {
      isValid: false,
      sanitized: '',
      error: `${fieldName}에 허용되지 않는 데이터베이스 명령어가 포함되어 있습니다.`
    };
  }
  
  // HTML 이스케이프 적용
  const sanitized = escapeHtml(input.trim());
  
  return { isValid: true, sanitized };
};

/**
 * 안전한 문자열만 허용 (알파벳, 숫자, 일부 특수문자, 한글)
 */
export const sanitizeText = (str: string): string => {
  if (!str) return '';
  
  // 허용되는 문자: 한글, 영문, 숫자, 공백, 기본 특수문자
  const allowedChars = /[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\s\-_.,!?()[\]{}@#$%&*+=|\\:;"'<>\/~`]/g;
  
  return str.replace(allowedChars, '').trim();
};

/**
 * 사용자 입력을 안전하게 처리하는 훅
 */
export const useSafeInput = () => {
  const validateAndSanitize = (value: string, fieldName: string = 'Input') => {
    const result = validateInput(value, fieldName);
    return result;
  };
  
  return { validateAndSanitize };
};

/**
 * 리뷰 텍스트 전용 검증 (더 관대한 규칙)
 */
export const validateReviewText = (text: string): {
  isValid: boolean;
  sanitized: string;
  error?: string;
} => {
  if (!text) {
    return { isValid: true, sanitized: '' };
  }
  
  // 스크립트 태그만 엄격하게 차단
  const scriptPattern = /<script[\s\S]*?>[\s\S]*?<\/script>|javascript:|vbscript:|on\w+\s*=/gi;
  
  if (scriptPattern.test(text)) {
    return {
      isValid: false,
      sanitized: '',
      error: '리뷰에 허용되지 않는 스크립트 코드가 포함되어 있습니다.'
    };
  }
  
  // 기본 HTML 이스케이프만 적용 (SQL 인젝션은 덜 엄격하게)
  const sanitized = escapeHtml(text.trim());
  
  return { isValid: true, sanitized };
};