import axios from 'axios';

/**
 * 백엔드 API 베이스 URL 결정
 * - 기본은 window.origin
 * - 프론트가 3000에서 뜨고 백엔드 9090이면, 9090으로 치환
 * - 환경변수 REACT_APP_API_BASE_URL이 있으면 그것을 우선
 */
const resolvedBase =
    process.env.REACT_APP_API_BASE_URL ||
    (window.location.origin.includes(':3000')
        ? window.location.origin.replace(':3000', ':9090')
        : window.location.origin);

const normalize = (u: string) => u.replace(/\/+$/, '');
const baseURL = normalize(resolvedBase);

console.log('[API baseURL]', baseURL);

const api = axios.create({
    baseURL,
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' },
    // ✅ 세션(OAuth2 로그인) 쿠키 포함
    withCredentials: true,
});

// 요청/응답 로깅
api.interceptors.request.use((cfg) => {
    console.log('[REQ]', cfg.method?.toUpperCase(), `${cfg.baseURL}${cfg.url}`, cfg.params ?? cfg.data ?? '');
    return cfg;
});
api.interceptors.response.use(
    (res) => {
        console.log('[RES]', res.status, res.config.url, res.data);
        return res;
    },
    (err) => {
        console.log('[ERR]', err.code, err.message, err.response?.status, err.response?.data);
        return Promise.reject(err);
    }
);

export default api;
