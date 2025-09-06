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
    const [currentUserId, setCurrentUserId] = useState<number>(0); // 기본값을 0으로 변경
    const [currentUserName, setCurrentUserName] = useState<string>('뮤직러버');

    // 로그인된 사용자 정보 가져오기
    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                // OAuth 인증 확인
                const response = await fetch('/api/auth/me');
                const data = await response.json();
                if (data?.authenticated && data?.user?.id) {
                    const userId = Number(data.user.id);
                    const userName = data.user.name || `사용자${userId}`;
                    setCurrentUserId(userId);
                    setCurrentUserName(userName);
                    console.log('SocketContext: OAuth 사용자 설정됨', { userId, userName });
                } else {
                    // 로컬 인증 확인
                    try {
                        const localResponse = await fetch('/api/auth/local/me');
                        const localData = await localResponse.json();
                        if (localData?.success && localData?.user?.id) {
                            const userId = Number(localData.user.id);
                            const userName = localData.user.username || `사용자${userId}`;
                            setCurrentUserId(userId);
                            setCurrentUserName(userName);
                            console.log('SocketContext: 로컬 사용자 설정됨', { userId, userName });
                        } else {
                            console.log('SocketContext: 인증된 사용자 없음, 기본값 유지');
                        }
                    } catch (localError) {
                        console.error('SocketContext: 로컬 인증 확인 실패', localError);
                    }
                }
            } catch (error) {
                console.error('SocketContext: OAuth 인증 확인 실패', error);
                // 기본값 유지
            }
        };
        
        fetchCurrentUser();
    }, []);

    useEffect(() => {
        // 중복 연결 방지
        if (wsManager) {
            console.log('🔄 SocketContext: WebSocket 이미 연결됨, 중복 방지');
            return;
        }

        // 사용자 ID가 설정될 때까지 대기
        if (currentUserId === 0) {
            console.log('⏳ SocketContext: 사용자 ID 설정 대기 중...');
            return;
        }

        console.log(`🔌 SocketContext: 새로운 WebSocket 연결 생성 (userId: ${currentUserId})`);
        
        // WebSocket 매니저 생성 및 설정 - 실제 사용자 ID 전달
        const manager = new WebSocketManager(currentUserId.toString());
        let isConnected = false; // 중복 연결 상태 추적
        
        console.log('🔧 SocketContext: WebSocketManager 생성 완료, 이벤트 핸들러 설정 중...');

        // 연결 이벤트 핸들러
        manager.onConnect(() => {
            if (isConnected) {
                console.log('🔄 SocketContext: 이미 연결된 상태, 중복 메시지 방지');
                return;
            }
            
            isConnected = true;
            console.log('🎉 SocketContext: WebSocket 연결 완료');
            setIsConnected(true);
            setConnectionStatus('connected');
            // 중복 메시지 방지를 위해 한 번만 표시
            toast.success('🔗 실시간 연결 활성화', { 
                duration: 2000,
                id: 'socket-connected' // 중복 방지 ID
            });
        });

        // 연결 해제 이벤트 핸들러
        manager.onDisconnect(() => {
            isConnected = false;
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

        // 일반 메시지 핸들러 (중복 방지 추가)
        let lastMatchingMessage = '';
        manager.onMessage((message: WebSocketMessage) => {
            console.log('🔥 SocketContext: WebSocket 메시지 수신:', message);

            switch (message.type) {
                case 'MATCHING_SUCCESS':
                    const messageKey = `${message.data?.roomId || 'unknown'}_${Date.now()}`;
                    if (lastMatchingMessage === messageKey) {
                        console.log('🔄 SocketContext: 중복 매칭 성공 메시지 방지');
                        return;
                    }
                    lastMatchingMessage = messageKey;
                    
                    console.log('🎉 SocketContext: 매칭 성공 메시지 처리중, 데이터:', message.data);
                    
                    // 토스트는 Matching.tsx에서 실제 이름 확인 후 표시 (중복 방지)
                    
                    // 매칭 성공 이벤트 발생
                    console.log('📢 SocketContext: matchingSuccess 커스텀 이벤트 발생');
                    window.dispatchEvent(new CustomEvent('matchingSuccess', { detail: message.data }));
                    break;

                case 'MATCHING_FAILED':
                    console.log('❌ SocketContext: 매칭 실패');
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
        
        // 디버깅을 위해 전역에 등록
        (window as any).wsManager = manager;
        console.log('🔧 SocketContext: WebSocketManager를 전역에 등록함');

        // WebSocket 연결 시작
        setConnectionStatus('connecting');
        console.log('🔌 SocketContext: WebSocket 연결 시작...');
        manager.connect();

        // 정리 함수
        return () => {
            manager.disconnect();
        };
    }, [currentUserId]); // currentUserId 의존성 추가

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