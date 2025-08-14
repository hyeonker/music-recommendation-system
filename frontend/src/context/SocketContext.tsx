import { createContext, useContext } from 'react';

// Socket Context 타입 정의
interface SocketContextType {
    socket: any;
}

// Socket Context 생성
export const SocketContext = createContext<SocketContextType | null>(null);

// Socket Context Hook
export const useSocket = () => {
    const context = useContext(SocketContext);
    if (!context) {
        return { socket: null }; // 기본값 반환
    }
    return context;
};