// frontend/src/api/client.ts
import axios from 'axios';

const resolvedBase =
    process.env.REACT_APP_API_BASE_URL ||
    (window.location.origin.includes(':3000')
        ? window.location.origin.replace(':3000', ':9090')
        : window.location.origin);

const normalize = (u: string) => u.replace(/\/+$/, '');
const baseURL = normalize(resolvedBase);

console.log('[API baseURL]', baseURL);   // ⬅️ 어디로 쏘고 있는지 항상 로그

const api = axios.create({
    baseURL,
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' },
    withCredentials: false,
});

// 요청/응답 디버그
api.interceptors.request.use((cfg) => {
    console.log('[REQ]', cfg.method?.toUpperCase(), `${cfg.baseURL}${cfg.url}`, cfg.params ?? cfg.data ?? '');
    return cfg;
});
api.interceptors.response.use(
    (res) => {
        console.log('[RES]', res.status, res.config.url);
        return res;
    },
    (err) => {
        console.log('[ERR]', err.code, err.message, err.response?.status, err.response?.data);
        return Promise.reject(err);
    }
);

export default api;
