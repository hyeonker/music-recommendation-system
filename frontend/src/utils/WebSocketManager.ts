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

    // 이벤트 핸들러들
    private onConnectHandler?: () => void;
    private onDisconnectHandler?: () => void;
    private onErrorHandler?: (error: any) => void;
    private onMessageHandler?: (message: WebSocketMessage) => void;
    private onChatMessageHandler?: (message: ChatMessage) => void;

    constructor() {
        this.setupClient();
    }

    private setupClient() {
        this.client = new Client({
            // SockJS를 사용한 WebSocket 연결
            webSocketFactory: () => new SockJS('http://localhost:9090/ws'),

            // 연결 옵션
            connectHeaders: {
                'userId': '1' // 현재 사용자 ID
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
                console.log('WebSocket 연결 성공:', frame);
                this.isConnected = true;
                this.reconnectAttempts = 0;
                this.setupSubscriptions();
                this.onConnectHandler?.();
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
        if (!this.client || !this.isConnected) return;

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

        // 매칭 결과 구독
        const matchingSub = this.client.subscribe('/user/queue/matching-result', (message: IMessage) => {
            try {
                const data = JSON.parse(message.body);
                this.handleMessage({
                    type: data.success ? 'MATCHING_SUCCESS' : 'MATCHING_FAILED',
                    data: data,
                    timestamp: new Date().toISOString()
                });
            } catch (error) {
                console.error('매칭 결과 파싱 오류:', error);
            }
        });
        this.subscriptions.set('matching', matchingSub);

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

    // 채팅방 구독
    public subscribeToChatRoom(roomId: string): void {
        if (!this.client || !this.isConnected) {
            console.warn('WebSocket이 연결되지 않았습니다');
            return;
        }

        // 이미 구독 중인 채팅방이 있다면 구독 해제
        if (this.subscriptions.has('chat')) {
            this.subscriptions.get('chat').unsubscribe();
        }

        // 새 채팅방 구독
        const chatSub = this.client.subscribe(`/topic/chat/${roomId}`, (message: IMessage) => {
            try {
                const chatMessage: ChatMessage = JSON.parse(message.body);
                this.onChatMessageHandler?.(chatMessage);
            } catch (error) {
                console.error('채팅 메시지 파싱 오류:', error);
            }
        });
        this.subscriptions.set('chat', chatSub);
        console.log(`채팅방 ${roomId} 구독 완료`);
    }

    // 채팅 메시지 전송
    public sendChatMessage(roomId: string, content: string, senderId: number, senderName: string): void {
        if (!this.client || !this.isConnected) {
            console.warn('WebSocket이 연결되지 않았습니다');
            return;
        }

        const message = {
            roomId,
            senderId,
            senderName,
            content,
            timestamp: new Date().toISOString()
        };

        this.client.publish({
            destination: '/app/send-chat-message',
            body: JSON.stringify(message)
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

    // 상태 확인
    public getConnectionStatus(): 'connected' | 'connecting' | 'disconnected' {
        if (this.isConnected) return 'connected';
        if (this.client?.active) return 'connecting';
        return 'disconnected';
    }
}