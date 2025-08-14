import { createContext, useContext } from 'react';

// User 타입 정의
interface User {
    id: number;
    name: string;
}

// User Context 타입 정의
interface UserContextType {
    user: User;
    setUser: (user: User) => void;
}

// User Context 생성
export const UserContext = createContext<UserContextType | null>(null);

// User Context Hook
export const useUser = () => {
    const context = useContext(UserContext);
    if (!context) {
        return {
            user: { id: 1, name: '기본사용자' },
            setUser: () => {}
        }; // 기본값 반환
    }
    return context;
};