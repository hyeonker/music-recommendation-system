import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketMessage {
    type: 'CHAT_MESSAGE' | 'MATCHING_SUCCESS' | 'MATCHING_FAILED' | 'USER_STATUS';
    data: any;
    timestamp: string;
}

export interface ChatMessage {
    id: string;
    roomId: string;
    senderId: number;
    senderName: string;
    content: string;
    timestamp: string;
}

export class WebSocketManager {
    private client: Client | null = null;
    private isConnected: boolean = false;
    private reconnectAttempts: number = 0;
    private maxReconnectAttempts: number = 5;
    private subscriptions: Map<string, any> = new Map();
    private currentUserId: string = '1'; // 기본값

    // 이벤트 핸들러들
    private onConnectHandler?: () => void;
    private onDisconnectHandler?: () => void;
    private onErrorHandler?: (error: any) => void;
    private onMessageHandler?: (message: WebSocketMessage) => void;
    private onChatMessageHandler?: (message: ChatMessage) => void;

    constructor(userId?: string) {
        if (userId) {
            this.currentUserId = userId;
            console.log('🚀 WebSocketManager 사용자 ID 설정:', userId);
        }
        // 초기화만 하고 연결은 외부에서 호출하도록 변경
        this.initializeUserId().then(() => {
            this.setupClient();
            console.log('🚀 WebSocketManager 초기화 완료, 연결 대기');
        });
    }

    private async initializeUserId() {
        // 생성자에서 이미 ID가 설정되었으면 추가 인증 불필요
        if (this.currentUserId !== '1') {
            console.log('WebSocketManager: 사용자 ID 이미 설정됨:', this.currentUserId);
            return;
        }
        
        try {
            // OAuth 인증 확인
            const response = await fetch('/api/auth/me');
            const data = await response.json();
            if (data?.authenticated && data?.user?.id) {
                this.currentUserId = data.user.id.toString();
                console.log('WebSocketManager: OAuth 사용자 ID 설정됨:', this.currentUserId);
            } else {
                // 로컬 인증 확인
                try {
                    const localResponse = await fetch('/api/auth/local/me');
                    const localData = await localResponse.json();
                    if (localData?.success && localData?.user?.id) {
                        this.currentUserId = localData.user.id.toString();
                        console.log('WebSocketManager: 로컬 사용자 ID 설정됨:', this.currentUserId);
                    }
                } catch (localError) {
                    console.error('WebSocketManager: 로컬 인증 확인 실패:', localError);
                }
            }
        } catch (error) {
            console.error('WebSocketManager: OAuth 인증 확인 실패:', error);
        }
    }

    private setupClient() {
        this.client = new Client({
            // SockJS를 사용한 WebSocket 연결 - URL 파라미터로 userId 전달
            webSocketFactory: () => new SockJS(`http://localhost:9090/ws?userId=${this.currentUserId}`),

            // 연결 옵션
            connectHeaders: {
                'userId': this.currentUserId // 동적 사용자 ID (헤더로도 전달)
            },

            // 하트비트 설정
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            // 재연결 설정
            reconnectDelay: 2000,

            // 디버그 로그
            debug: (str) => {
                console.log('STOMP Debug:', str);
            },

            // 연결 성공 콜백
            onConnect: (frame) => {
                console.log('🎉 WebSocket 연결 성공:', frame);
                this.isConnected = true;
                this.reconnectAttempts = 0;
                
                // 연결 후 잠깐 대기 후 구독 설정 (안정성 향상)
                setTimeout(() => {
                    console.log('📡 구독 설정 시작...');
                    this.setupSubscriptions();
                    this.onConnectHandler?.();
                }, 100);
            },

            // 연결 해제 콜백
            onDisconnect: () => {
                console.log('WebSocket 연결 해제');
                this.isConnected = false;
                this.onDisconnectHandler?.();
            },

            // 연결 오류 콜백
            onStompError: (frame) => {
                console.error('STOMP 오류:', frame);
                this.onErrorHandler?.(frame);
            },

            // WebSocket 오류 콜백
            onWebSocketError: (event) => {
                console.error('WebSocket 오류:', event);
                this.onErrorHandler?.(event);
            }
        });
    }

    // WebSocket 연결
    public connect(): void {
        if (this.client && !this.isConnected) {
            try {
                console.log('🔌 WebSocket 연결 시도 중...');
                this.client.activate();
            } catch (error) {
                console.error('WebSocket 연결 실패:', error);
                this.scheduleReconnect();
            }
        }
    }

    // WebSocket 연결 해제
    public disconnect(): void {
        if (this.client) {
            this.client.deactivate();
            this.isConnected = false;
            this.subscriptions.clear();
        }
    }

    // 구독 설정
    private setupSubscriptions(): void {
        if (!this.client || !this.isConnected) {
            console.warn('⚠️ WebSocket 클라이언트가 없거나 연결되지 않음');
            return;
        }
        
        console.log('🔧 WebSocket 구독 설정 중...');

        // 개인 알림 구독
        const personalSub = this.client.subscribe('/user/queue/notifications', (message: IMessage) => {
            try {
                const data = JSON.parse(message.body);
                this.handleMessage({
                    type: data.type,
                    data: data,
                    timestamp: new Date().toISOString()
                });
            } catch (error) {
                console.error('개인 알림 파싱 오류:', error);
            }
        });
        this.subscriptions.set('personal', personalSub);
        console.log('✅ 개인 알림 구독 완료: /user/queue/notifications');

        // 매칭 결과 구독
        const matchingSub = this.client.subscribe('/user/queue/matching-result', (message: IMessage) => {
            try {
                const data = JSON.parse(message.body);
                console.log('🔥 WebSocket 매칭 결과 전체 데이터:', data);
                console.log('📊 데이터 구조 분석:');
                console.log('  - data.status:', data.status);
                console.log('  - data.success:', data.success); 
                console.log('  - data.type:', data.type);
                console.log('  - data.matchingResult:', data.matchingResult);
                
                // status가 "WAITING"이면 대기 상태로 처리하고 즉시 return
                if (data.status === 'WAITING') {
                    console.log('⏳ 매칭 대기 상태 - 매칭 성공 처리하지 않음');
                    return; // WAITING 상태일 때는 매칭 성공 처리 중단
                }
                
                // 매칭 성공 처리 - Event 기반 데이터 구조 고려
                if (data.type === 'MATCHING_SUCCESS' || 
                    (data.success && (data.status === 'MATCHED' || data.status === 'ALREADY_MATCHED')) || 
                    (data.matchingResult && data.matchingResult.status === 'MATCHED')) {
                    
                    console.log('매칭 성공 감지, 이벤트 발생:', data);
                    this.handleMessage({
                        type: 'MATCHING_SUCCESS',
                        data: data,
                        timestamp: new Date().toISOString()
                    });
                } else if (!data.success && data.status !== 'ALREADY_MATCHED') {
                    this.handleMessage({
                        type: 'MATCHING_FAILED',
                        data: data,
                        timestamp: new Date().toISOString()
                    });
                }
            } catch (error) {
                console.error('매칭 결과 파싱 오류:', error);
            }
        });
        this.subscriptions.set('matching', matchingSub);
        console.log('✅ 매칭 결과 구독 완료: /user/queue/matching-result');

        // 시스템 상태 구독
        const systemSub = this.client.subscribe('/topic/system-status', (message: IMessage) => {
            try {
                const data = JSON.parse(message.body);
                this.handleMessage({
                    type: 'USER_STATUS',
                    data: data,
                    timestamp: new Date().toISOString()
                });
            } catch (error) {
                console.error('시스템 상태 파싱 오류:', error);
            }
        });
        this.subscriptions.set('system', systemSub);
    }

    // 채팅방 구독 - 재시도 메커니즘 추가
    public subscribeToChatRoom(roomId: string): void {
        const attemptSubscribe = (attempt: number = 1): void => {
            if (!this.client) {
                console.warn('⚠️ WebSocket 클라이언트가 없음 - 채팅방 구독 불가');
                return;
            }
            
            if (!this.isConnected) {
                if (attempt <= 3) {
                    console.log(`🔄 WebSocket 연결 대기 중... (${attempt}/3)`);
                    setTimeout(() => attemptSubscribe(attempt + 1), 1000 * attempt);
                } else {
                    console.error('❌ WebSocket 연결 실패 - 채팅방 구독 중단');
                }
                return;
            }

            console.log('🏠 채팅방 구독 시작:', roomId, `(attempt: ${attempt})`);

            // 이미 구독 중인 채팅방이 있다면 구독 해제
            if (this.subscriptions.has('chat')) {
                console.log('📤 기존 채팅방 구독 해제');
                try {
                    this.subscriptions.get('chat').unsubscribe();
                } catch (e) {
                    console.warn('기존 구독 해제 오류:', e);
                }
            }
            
            // 정규화된 채팅방 구독도 해제
            if (this.subscriptions.has('chat_normalized')) {
                console.log('📤 기존 정규화 채팅방 구독 해제');
                try {
                    this.subscriptions.get('chat_normalized').unsubscribe();
                } catch (e) {
                    console.warn('기존 정규화 구독 해제 오류:', e);
                }
            }

            // 새 채팅방 구독 - 원본 roomId와 정규화 둘 다 구독
            try {
                const subscriptionTopic = `/topic/room.${roomId}`;
                console.log(`🔄 채팅방 구독 시도: ${subscriptionTopic}`);
                
                const chatSub = this.client.subscribe(subscriptionTopic, (message: IMessage) => {
                    try {
                        console.log(`💬 채팅 메시지 수신 [${subscriptionTopic}]:`, message.body);
                        const chatMessage: ChatMessage = JSON.parse(message.body);
                        
                        // SocketContext로 전달
                        this.onChatMessageHandler?.(chatMessage);
                    } catch (error) {
                        console.error(`채팅 메시지 파싱 오류 [${subscriptionTopic}]:`, error);
                    }
                });
                
                this.subscriptions.set('chat', chatSub);
                console.log(`✅ 채팅방 구독 완료: ${subscriptionTopic}`);
                
                // 정규화된 roomId로도 추가 구독 (호환성)
                const normalizedRoomId = this.normalizeRoomId(roomId);
                if (normalizedRoomId !== roomId) {
                    const normalizedTopic = `/topic/room.${normalizedRoomId}`;
                    console.log(`🔄 정규화 채팅방 구독 시도: ${normalizedTopic}`);
                    
                    const normalizedSub = this.client.subscribe(normalizedTopic, (message: IMessage) => {
                        try {
                            console.log(`💬 정규화 메시지 수신 [${normalizedTopic}]:`, message.body);
                            const chatMessage: ChatMessage = JSON.parse(message.body);
                            this.onChatMessageHandler?.(chatMessage);
                        } catch (error) {
                            console.error(`정규화 메시지 파싱 오류 [${normalizedTopic}]:`, error);
                        }
                    });
                    
                    this.subscriptions.set('chat_normalized', normalizedSub);
                    console.log(`✅ 정규화 채팅방 구독 완료: ${normalizedTopic}`);
                }
                
            } catch (error) {
                console.error('채팅방 구독 오류:', error);
                if (attempt <= 2) {
                    setTimeout(() => attemptSubscribe(attempt + 1), 2000);
                }
            }
        };
        
        attemptSubscribe();
    }

    // 채팅 메시지 전송 - 안정성 개선
    public sendChatMessage(roomId: string, content: string, senderId?: number, senderName?: string): Promise<void> {
        return new Promise((resolve, reject) => {
            if (!this.client || !this.isConnected) {
                const error = 'WebSocket이 연결되지 않았습니다';
                console.warn(error);
                reject(new Error(error));
                return;
            }

            try {
                const payload = {
                    senderId: senderId || parseInt(this.currentUserId),
                    content: content,
                    type: 'TEXT'
                };
                
                console.log('🚀 WebSocket 메시지 전송:', { 
                    destination: `/app/chat.sendMessage/${roomId}`,
                    payload 
                });

                this.client.publish({
                    destination: `/app/chat.sendMessage/${roomId}`,
                    body: JSON.stringify(payload)
                });
                
                // 성공적으로 전송됨
                resolve();
                
            } catch (error) {
                console.error('메시지 전송 오류:', error);
                reject(error);
            }
        });
    }

    // 매칭 요청
    public requestMatching(userId: number): void {
        if (!this.client || !this.isConnected) {
            console.warn('WebSocket이 연결되지 않았습니다');
            return;
        }

        this.client.publish({
            destination: '/app/request-matching',
            body: JSON.stringify({ userId })
        });
    }

    // 음악 공유
    public shareMusic(roomId: string, musicData: any): void {
        if (!this.client || !this.isConnected) {
            console.warn('WebSocket이 연결되지 않았습니다');
            return;
        }

        this.client.publish({
            destination: '/app/share-music',
            body: JSON.stringify({ roomId, ...musicData })
        });
    }

    // 핑 전송 (연결 확인)
    public ping(): void {
        if (!this.client || !this.isConnected) return;

        this.client.publish({
            destination: '/app/ping',
            body: JSON.stringify({ timestamp: Date.now() })
        });
    }

    // 메시지 처리
    private handleMessage(message: WebSocketMessage): void {
        this.onMessageHandler?.(message);
    }

    // 재연결 스케줄링
    private scheduleReconnect(): void {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('최대 재연결 시도 횟수 초과');
            return;
        }

        this.reconnectAttempts++;
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);

        setTimeout(() => {
            console.log(`재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
            this.connect();
        }, delay);
    }

    // 이벤트 핸들러 설정
    public onConnect(handler: () => void): void {
        this.onConnectHandler = handler;
    }

    public onDisconnect(handler: () => void): void {
        this.onDisconnectHandler = handler;
    }

    public onError(handler: (error: any) => void): void {
        this.onErrorHandler = handler;
    }

    public onMessage(handler: (message: WebSocketMessage) => void): void {
        this.onMessageHandler = handler;
    }

    public onChatMessage(handler: (message: ChatMessage) => void): void {
        this.onChatMessageHandler = handler;
    }

    // roomId 정규화 (Chat.tsx와 동일한 로직)
    private normalizeRoomId(roomIdLike: string): string {
        const s = String(roomIdLike);
        const digits = s.replace(/\D+/g, '');
        return digits || s; // 숫자 없으면 원문 반환
    }

    // 상태 확인
    public getConnectionStatus(): 'connected' | 'connecting' | 'disconnected' {
        if (this.isConnected) return 'connected';
        if (this.client?.active) return 'connecting';
        return 'disconnected';
    }
}