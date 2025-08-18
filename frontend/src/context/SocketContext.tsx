import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { WebSocketManager, WebSocketMessage, ChatMessage } from '../utils/WebSocketManager';
import { toast } from 'react-hot-toast';

interface SocketContextType {
    wsManager: WebSocketManager | null;
    isConnected: boolean;
    connectionStatus: 'connected' | 'connecting' | 'disconnected';
    sendChatMessage: (roomId: string, content: string) => void;
    subscribeToChatRoom: (roomId: string) => void;
    requestMatching: () => void;
    shareMusic: (roomId: string, musicData: any) => void;
}

export const SocketContext = createContext<SocketContextType | null>(null);

export const SocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [wsManager, setWsManager] = useState<WebSocketManager | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const [connectionStatus, setConnectionStatus] = useState<'connected' | 'connecting' | 'disconnected'>('disconnected');

    const currentUserId = 1; // 현재 사용자 ID
    const currentUserName = '뮤직러버'; // 현재 사용자 이름

    useEffect(() => {
        // WebSocket 매니저 생성 및 설정
        const manager = new WebSocketManager();

        // 연결 이벤트 핸들러
        manager.onConnect(() => {
            console.log('WebSocket Context: 연결됨');
            setIsConnected(true);
            setConnectionStatus('connected');
            toast.success('실시간 연결이 활성화되었습니다');
        });

        // 연결 해제 이벤트 핸들러
        manager.onDisconnect(() => {
            console.log('WebSocket Context: 연결 해제됨');
            setIsConnected(false);
            setConnectionStatus('disconnected');
            toast.error('실시간 연결이 끊어졌습니다');
        });

        // 오류 이벤트 핸들러
        manager.onError((error) => {
            console.error('WebSocket Context: 오류 발생', error);
            setConnectionStatus('disconnected');
        });

        // 일반 메시지 핸들러
        manager.onMessage((message: WebSocketMessage) => {
            console.log('WebSocket 메시지 수신:', message);

            switch (message.type) {
                case 'MATCHING_SUCCESS':
                    toast.success('매칭 성공! 새로운 음악 친구를 찾았습니다');
                    // 매칭 성공 이벤트 발생
                    window.dispatchEvent(new CustomEvent('matchingSuccess', { detail: message.data }));
                    break;

                case 'MATCHING_FAILED':
                    toast.error('매칭에 실패했습니다. 다시 시도해주세요');
                    window.dispatchEvent(new CustomEvent('matchingFailed', { detail: message.data }));
                    break;

                case 'USER_STATUS':
                    // 사용자 상태 업데이트
                    window.dispatchEvent(new CustomEvent('userStatusUpdate', { detail: message.data }));
                    break;
            }
        });

        // 채팅 메시지 핸들러
        manager.onChatMessage((message: ChatMessage) => {
            console.log('새 채팅 메시지:', message);
            // 채팅 메시지 이벤트 발생
            window.dispatchEvent(new CustomEvent('newChatMessage', { detail: message }));
        });

        setWsManager(manager);

        // WebSocket 연결 시작
        setConnectionStatus('connecting');
        manager.connect();

        // 정리 함수
        return () => {
            manager.disconnect();
        };
    }, []);

    // 연결 상태 업데이트
    useEffect(() => {
        if (wsManager) {
            const interval = setInterval(() => {
                const status = wsManager.getConnectionStatus();
                setConnectionStatus(status);
                setIsConnected(status === 'connected');
            }, 1000);

            return () => clearInterval(interval);
        }
    }, [wsManager]);

    // 채팅 메시지 전송
    const sendChatMessage = useCallback((roomId: string, content: string) => {
        if (wsManager && isConnected) {
            wsManager.sendChatMessage(roomId, content, currentUserId, currentUserName);
        } else {
            toast.error('연결이 끊어졌습니다. 다시 시도해주세요');
        }
    }, [wsManager, isConnected, currentUserId, currentUserName]);

    // 채팅방 구독
    const subscribeToChatRoom = useCallback((roomId: string) => {
        if (wsManager && isConnected) {
            wsManager.subscribeToChatRoom(roomId);
        }
    }, [wsManager, isConnected]);

    // 매칭 요청
    const requestMatching = useCallback(() => {
        if (wsManager && isConnected) {
            wsManager.requestMatching(currentUserId);
            toast.success('매칭 요청을 전송했습니다');
        } else {
            toast.error('연결이 끊어졌습니다. 다시 시도해주세요');
        }
    }, [wsManager, isConnected, currentUserId]);

    // 음악 공유
    const shareMusic = useCallback((roomId: string, musicData: any) => {
        if (wsManager && isConnected) {
            wsManager.shareMusic(roomId, musicData);
            toast.success('음악을 공유했습니다');
        } else {
            toast.error('연결이 끊어졌습니다. 다시 시도해주세요');
        }
    }, [wsManager, isConnected]);

    const contextValue: SocketContextType = {
        wsManager,
        isConnected,
        connectionStatus,
        sendChatMessage,
        subscribeToChatRoom,
        requestMatching,
        shareMusic
    };

    return (
        <SocketContext.Provider value={contextValue}>
            {children}
        </SocketContext.Provider>
    );
};

// Hook for using socket context
export const useSocket = () => {
    const context = useContext(SocketContext);
    if (!context) {
        return {
            wsManager: null,
            isConnected: false,
            connectionStatus: 'disconnected' as const,
            sendChatMessage: () => {},
            subscribeToChatRoom: () => {},
            requestMatching: () => {},
            shareMusic: () => {}
        };
    }
    return context;
};