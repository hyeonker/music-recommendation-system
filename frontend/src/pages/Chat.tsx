// frontend/src/pages/Chat.tsx
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Send, Paperclip, Smile, User, Loader2, Music, ArrowLeft, CheckCheck, LogOut } from 'lucide-react';
import { toast } from 'react-hot-toast';
import api from '../api/client';
import { useSocket } from '../context/SocketContext';
import { validateInput, validateReviewText } from '../utils/security';

/* ===================== 타입 ===================== */
type ChatRoom = {
    id: string | number;
    name?: string;
    lastMessage?: string;
    unread?: number;
};

type ChatMessage = {
    id: string | number;
    roomId: string | number;
    senderId: number | string;
    senderName?: string;
    content: string;
    createdAt: string; // ISO
    type?: string;
};

type RepresentativeBadge = {
    id: number;
    badgeType: string;
    badgeName: string;
    description: string;
    iconUrl?: string;
    rarity: string;
    badgeColor: string;
};

type Me = { id: number; name?: string; email?: string | null };

/* ===================== 유틸 ===================== */
const useQuery = () => new URLSearchParams(useLocation().search);

const fmtTime = (iso: string) => {
    try {
        const d = new Date(iso);
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
        return '';
    }
};

// "room_2_2" → "22"
const normalizeRoomId = (roomIdLike: string | number): string => {
    const s = String(roomIdLike);
    const digits = s.replace(/\D+/g, '');
    return digits || s; // 숫자 없으면 원문 반환(백엔드도 정규화하므로 안전)
};

// 매칭된 사용자의 실제 이름 가져오기
const getSenderDisplayName = (message: ChatMessage): string => {
    if (String(message.senderId) === '0') {
        return '시스템';
    }
    
    // sessionStorage에서 매칭된 사용자 정보 확인
    try {
        const matchedUserData = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
        if (matchedUserData && matchedUserData.id === Number(message.senderId) && matchedUserData.name) {
            return matchedUserData.name;
        }
    } catch (e) {
        console.warn('매칭된 사용자 정보 파싱 실패:', e);
    }
    
    // 폴백: 메시지에 포함된 이름 또는 기본값
    return message.senderName ?? '상대';
};

/* ===================== 서브 컴포넌트 ===================== */
const ChatMessageItem: React.FC<{
    message: ChatMessage;
    mine: boolean;
    fetchUserBadge: (userId: number) => Promise<RepresentativeBadge | null>;
}> = ({ message, mine, fetchUserBadge }) => {
    const [badge, setBadge] = useState<RepresentativeBadge | null>(null);
    const [badgeLoading, setBadgeLoading] = useState(false);
    
    // 시스템 메시지 확인 (senderId가 0, -1이거나 senderName이 "시스템"인 경우)
    const isSystemMessage = String(message.senderId) === '0' || String(message.senderId) === '-1' || message.senderName === '시스템';

    useEffect(() => {
        if (!mine && !isSystemMessage && message.senderId && String(message.senderId) !== '0') {
            setBadgeLoading(true);
            fetchUserBadge(Number(message.senderId))
                .then(setBadge)
                .finally(() => setBadgeLoading(false));
        }
    }, [message.senderId, mine, isSystemMessage, fetchUserBadge]);

    // 시스템 메시지는 중앙 정렬로 특별 처리
    if (isSystemMessage) {
        return (
            <div className="flex justify-center mb-2">
                <div className="max-w-[85%] rounded-xl px-4 py-2 text-xs bg-amber-500/20 text-amber-200 border border-amber-500/30 text-center">
                    <div className="font-medium text-amber-100 mb-1">
                        {message.senderName || '시스템'}
                    </div>
                    <div className="whitespace-pre-wrap break-words">
                        {message.content === '[decrypt_error]' ? '🔒 복호화 오류 메시지' : message.content}
                    </div>
                    <div className="text-[10px] opacity-60 mt-1">{fmtTime(message.createdAt)}</div>
                </div>
            </div>
        );
    }

    return (
        <div className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${
                mine ? 'bg-blue-600 text-white' : 'bg-white/10 text-white'
            }`}>
                {!mine && (
                    <div className="flex items-center gap-2 mb-1">
                        <div className="text-[10px] opacity-70">
                            {getSenderDisplayName(message)}
                        </div>
                        {badge && (
                            <div className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-medium relative ${
                                badge.rarity === 'LEGENDARY' 
                                    ? 'legendary-badge-glow legendary-chat-border shadow-sm' 
                                    : ''
                            }`} 
                                 style={{ 
                                   backgroundColor: `${badge.badgeColor}20`, 
                                   color: badge.badgeColor,
                                   boxShadow: badge.rarity === 'LEGENDARY' 
                                     ? `0 0 4px ${badge.badgeColor}40, 0 0 6px ${badge.badgeColor}25`
                                     : 'none',
                                   border: badge.rarity === 'LEGENDARY'
                                     ? `1px solid ${badge.badgeColor}60`
                                     : 'none'
                                 }}>
                                {badge.rarity === 'LEGENDARY' && (
                                    <>
                                        <div className="absolute -inset-0.5 rounded-full animate-ping opacity-10"
                                             style={{ backgroundColor: badge.badgeColor }}></div>
                                        <div className="absolute top-0 left-0 w-full h-full rounded-full"
                                             style={{ 
                                               background: `linear-gradient(45deg, transparent, ${badge.badgeColor}30, transparent, ${badge.badgeColor}20, transparent)`,
                                               animation: 'legendary-wave 3s ease-in-out infinite'
                                             }}>
                                        </div>
                                    </>
                                )}
                                {badge.iconUrl && (
                                    <img src={badge.iconUrl} alt="" 
                                         className={`w-3 h-3 rounded-full relative z-10 ${
                                           badge.rarity === 'LEGENDARY' ? 'legendary-icon-float' : ''
                                         }`} />
                                )}
                                <span className={`relative z-10 font-bold ${
                                    badge.rarity === 'LEGENDARY' ? 'legendary-chat-text' : ''
                                }`}>{badge.badgeName}</span>
                                {badge.rarity === 'LEGENDARY' && (
                                    <span className="legendary-sparkle text-yellow-300 text-[8px]">✨</span>
                                )}
                            </div>
                        )}
                        {badgeLoading && <div className="w-3 h-3 bg-gray-400 animate-pulse rounded"></div>}
                    </div>
                )}
                <div className="whitespace-pre-wrap break-words">
                    {message.content === '[decrypt_error]' ? '🔒 복호화 오류 메시지' : message.content}
                </div>
                <div className="text-[10px] opacity-60 mt-1 text-right">{fmtTime(message.createdAt)}</div>
            </div>
        </div>
    );
};

/* ===================== 메인 컴포넌트 ===================== */
const Chat: React.FC = () => {
    const navigate = useNavigate();
    const query = useQuery();

    // 소켓(있을 수도 없음)
    const socket = useSocket() as any;
    const isSocketReady = !!socket?.isConnected;

    // 내 정보
    const [me, setMe] = useState<Me | null>(null);
    
    // 사용자별 대표 배지 캐시
    const [userBadges, setUserBadges] = useState<Map<number, RepresentativeBadge | null>>(new Map());

    // 방/메시지 상태
    const [rooms, setRooms] = useState<ChatRoom[]>([]);
    const [activeRoomId, setActiveRoomId] = useState<string | number | null>(null);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [input, setInput] = useState('');
    const [isTyping, setIsTyping] = useState(false);
    const [peerTyping, setPeerTyping] = useState(false);

    const inputRef = useRef<HTMLInputElement>(null);
    const endRef = useRef<HTMLDivElement>(null);

    /* -------- 대표 배지 조회 -------- */
    const fetchUserBadge = async (userId: number): Promise<RepresentativeBadge | null> => {
        if (userBadges.has(userId)) {
            return userBadges.get(userId) || null;
        }

        // sessionStorage에서 매칭된 사용자 정보 확인
        try {
            const matchedUserData = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
            if (matchedUserData && matchedUserData.id === userId && matchedUserData.badge) {
                const badge = matchedUserData.badge;
                setUserBadges(prev => new Map(prev.set(userId, badge)));
                return badge;
            }
        } catch (e) {
            console.warn('매칭된 사용자 정보 파싱 실패:', e);
        }

        // 세션에 없으면 API 호출
        try {
            const { data } = await api.get(`/api/badges/user/${userId}/representative`);
            const badge = data ? {
                id: data.id,
                badgeType: data.badgeType,
                badgeName: data.badgeName,
                description: data.description,
                iconUrl: data.iconUrl,
                rarity: data.rarity || 'COMMON',
                badgeColor: data.badgeColor || '#6B7280'
            } : null;
            
            setUserBadges(prev => new Map(prev.set(userId, badge)));
            return badge;
        } catch (error) {
            console.error(`사용자 ${userId}의 대표 배지 조회 실패:`, error);
            setUserBadges(prev => new Map(prev.set(userId, null)));
            return null;
        }
    };

    /* -------- URL/세션에서 roomId 복원 + 기존 데이터 클리어 -------- */
    useEffect(() => {
        // 🧹 기존 숫자형 roomId 클리어
        clearLegacyData();
        
        const rid = query.get('roomId') ?? sessionStorage.getItem('lastRoomId');
        if (rid && !/^\d+$/.test(rid)) { // UUID 기반만 허용
            setActiveRoomId(rid);
        }
    }, [query]);
    
    // 기존 숫자형 roomId 및 관련 데이터 클리어
    const clearLegacyData = () => {
        const lastRoomId = sessionStorage.getItem('lastRoomId');
        const urlRoomId = query.get('roomId');
        
        // URL이나 세션에 기존 숫자형 roomId가 있으면 클리어
        const legacyRoomId = urlRoomId || lastRoomId;
        if (legacyRoomId && /^\d+$/.test(legacyRoomId)) {
            console.log('🧹 채팅페이지에서 기존 숫자형 roomId 클리어:', legacyRoomId);
            sessionStorage.removeItem('lastRoomId');
            sessionStorage.removeItem('matchedUser');
            localStorage.removeItem('chatRoomId');
            
            toast.error('기존 채팅방은 더 이상 지원되지 않습니다. 새로운 매칭을 시작해주세요.', {
                duration: 4000,
                icon: '⚠️'
            });
            
            // 매칭 페이지로 리다이렉트
            setTimeout(() => {
                navigate('/matching');
            }, 2000);
        }
    };

    /* -------- 내 정보 -------- */
    useEffect(() => {
        (async () => {
            try {
                const { data } = await api.get('/api/auth/me');
                if (data?.authenticated && data?.user?.id) {
                    setMe({ id: data.user.id, name: data.user.name, email: data.user.email ?? null });
                } else {
                    setMe({ id: 1, name: '뮤직러버', email: null }); // 폴백
                }
            } catch {
                setMe({ id: 1, name: '뮤직러버', email: null });
            }
        })();
    }, []);

    /* -------- 방 목록 -------- */
    const loadRooms = async () => {
        try {
            const { data } = await api.get('/api/chat/rooms');
            const list: ChatRoom[] = Array.isArray(data)
                ? data.map((r: any) => {
                    const roomId = r.id ?? r.roomId;
                    let roomName = r.name;
                    
                    // 방 이름이 없으면 매칭된 사용자 정보로 생성
                    if (!roomName || roomName === 'undefined' || roomName.includes('undefined')) {
                        try {
                            const matched = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
                            if (matched?.name) {
                                roomName = `${matched.name}와의 채팅`;
                            } else {
                                roomName = typeof roomId === 'string' && roomId.length > 8 
                                    ? `채팅방 ${roomId.substring(0, 8)}...`
                                    : `방 ${roomId}`;
                            }
                        } catch (e) {
                            roomName = `방 ${roomId}`;
                        }
                    }
                    
                    return {
                        id: roomId,
                        name: roomName,
                        lastMessage: r.lastMessage ?? '',
                        unread: r.unread ?? 0,
                    };
                })
                : [];
            setRooms(list);
        } catch {
            // 폴백: UUID 기반 roomId만 허용
            const rid = query.get('roomId') ?? sessionStorage.getItem('lastRoomId');
            if (rid && !/^\d+$/.test(rid)) {
                const matched = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
                setRooms([
                    {
                        id: rid,
                        name: matched?.name ? `${matched.name}와의 채팅` : `채팅방 ${rid.substring(0, 8)}...`,
                        lastMessage: '',
                        unread: 0,
                    },
                ]);
            } else {
                setRooms([]);
            }
        }
    };

    /* -------- 메시지 캐싱 키 -------- */
    const getMessageCacheKey = (roomId: string | number): string => {
        return `chatMessages_${roomId}`;
    };

    // 사용자 쌍 기반 캐시 키 (로그아웃/재로그인에 안전)
    const getUserPairCacheKey = (userId1: number, userId2: number): string => {
        const sortedIds = [userId1, userId2].sort((a, b) => a - b);
        return `chatHistory_${sortedIds[0]}_${sortedIds[1]}`;
    };

    // 메시지 캐시 업데이트 헬퍼 함수 (roomId + 사용자 쌍 캐시 동시 업데이트)
    const updateMessageCache = (messages: ChatMessage[], roomId: string | number) => {
        try {
            // 1. 룸ID 기반 캐시 업데이트
            const cacheKey = getMessageCacheKey(roomId);
            localStorage.setItem(cacheKey, JSON.stringify(messages));
            
            // 2. 사용자 쌍 기반 캐시 업데이트 (로그아웃/재로그인에 안전)
            if (me) {
                const matchedUserData = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
                if (matchedUserData?.id) {
                    const pairCacheKey = getUserPairCacheKey(me.id, matchedUserData.id);
                    localStorage.setItem(pairCacheKey, JSON.stringify(messages));
                    console.log('💾 Chat: 사용자 쌍 캐시도 함께 업데이트됨');
                }
            }
            
            console.log('💾 Chat: 메시지 캐시 업데이트됨 (총 메시지:', messages.length, '개)');
        } catch (e) {
            console.warn('💾 Chat: 메시지 캐시 업데이트 실패:', e);
        }
    };

    /* -------- 메시지 로드 (캐싱 포함) -------- */
    const loadMessages = async (roomId: string | number) => {
        const rid = normalizeRoomId(roomId);
        const cacheKey = getMessageCacheKey(roomId);
        
        // 1. 먼저 룸ID 기반 캐시된 메시지 로드
        let cachedMessages: ChatMessage[] = [];
        try {
            const cached = localStorage.getItem(cacheKey);
            if (cached) {
                cachedMessages = JSON.parse(cached);
                setMessages(cachedMessages);
                console.log('📚 Chat: 룸ID 기반 캐시된 메시지 로드됨:', cachedMessages.length, '개');
            }
        } catch (e) {
            console.warn('💾 Chat: 룸ID 기반 메시지 캐시 로드 실패:', e);
        }

        // 2. 룸ID 기반 캐시가 없으면 사용자 쌍 기반 캐시 확인
        if (cachedMessages.length === 0 && me) {
            try {
                const matchedUserData = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
                if (matchedUserData?.id) {
                    const pairCacheKey = getUserPairCacheKey(me.id, matchedUserData.id);
                    const pairCached = localStorage.getItem(pairCacheKey);
                    if (pairCached) {
                        cachedMessages = JSON.parse(pairCached);
                        setMessages(cachedMessages);
                        console.log('📚 Chat: 사용자 쌍 기반 캐시된 메시지 로드됨:', cachedMessages.length, '개');
                    }
                }
            } catch (e) {
                console.warn('💾 Chat: 사용자 쌍 기반 메시지 캐시 로드 실패:', e);
            }
        }

        // 2. 서버에서 최신 메시지 가져오기
        try {
            console.log('🔄 Chat: 서버에서 메시지 로드 중...', rid);
            const { data } = await api.get(`/api/chat/rooms/${rid}/messages`, {
                params: { limit: 100, order: 'asc' },
            });
            const serverMessages: ChatMessage[] = Array.isArray(data)
                ? data.map((m: any, idx: number) => ({
                    id: m.id ?? idx,
                    roomId: rid,
                    senderId: m.senderId ?? m.sender?.id ?? 'unknown',
                    senderName: m.senderName ?? m.sender?.name ?? (String(m.senderId) === '0' ? '시스템' : '상대'),
                    content: String(m.content ?? ''),
                    type: m.type ?? 'TEXT',
                    createdAt: m.createdAt ?? new Date().toISOString(),
                }))
                : [];
            
            // 3. 서버 메시지가 있으면 캐시 업데이트
            if (serverMessages.length > 0) {
                setMessages(serverMessages);
                updateMessageCache(serverMessages, roomId);
                console.log('✅ Chat: 서버 메시지 로드 및 캐시 완료:', serverMessages.length, '개');
            } else if (cachedMessages.length === 0) {
                // 서버에도 캐시에도 메시지가 없으면 빈 배열
                setMessages([]);
                console.log('📭 Chat: 메시지 없음 (새 채팅방)');
            }
            // 서버에 메시지가 없지만 캐시에는 있으면 캐시 유지
        } catch (error) {
            console.warn('🚫 Chat: 서버 메시지 로드 실패, 캐시 사용:', error);
            // 서버 로드 실패 시 캐시된 메시지 유지 (이미 setMessages 호출됨)
            if (cachedMessages.length === 0) {
                setMessages([]); // 캐시도 없으면 빈 대화에서 시작
            }
        }
    };

    /* -------- 초기 로드 -------- */
    useEffect(() => {
        (async () => {
            await loadRooms();
            setLoading(false);
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    /* -------- 방이 정해지면 메시지 로드 -------- */
    useEffect(() => {
        if (!activeRoomId) return;
        setLoading(true);
        (async () => {
            await loadMessages(activeRoomId);
            setLoading(false);
            
            // 🎯 WebSocket 채팅방 구독 활성화! (연결 상태 확인 후)
            if (socket?.subscribeToChatRoom) {
                console.log('🏠 Chat: 채팅방 구독 시작 -', activeRoomId, 'WebSocket 연결 상태:', socket.isConnected);
                
                if (socket.isConnected) {
                    // 이미 연결되어 있으면 바로 구독
                    socket.subscribeToChatRoom(String(activeRoomId));
                } else {
                    // 연결되지 않았다면 연결 완료를 기다린 후 구독
                    console.log('🔄 Chat: WebSocket 연결 대기 중...');
                    const checkConnection = setInterval(() => {
                        if (socket.isConnected) {
                            console.log('✅ Chat: WebSocket 연결 완료, 채팅방 구독 실행');
                            socket.subscribeToChatRoom(String(activeRoomId));
                            clearInterval(checkConnection);
                        }
                    }, 100); // 100ms마다 연결 상태 체크
                    
                    // 10초 후 타임아웃
                    setTimeout(() => {
                        clearInterval(checkConnection);
                        console.warn('⚠️ Chat: WebSocket 연결 타임아웃');
                    }, 10000);
                }
            }
            
            setTimeout(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeRoomId, socket?.subscribeToChatRoom]);

    /* -------- 소켓 이벤트 구독 -------- */
    useEffect(() => {
        const onWinMsg = (ev: any) => {
            const m = ev?.detail;
            console.log('💬 Chat: 새 메시지 수신:', { message: m, currentRoom: activeRoomId });
            
            if (!m || String(normalizeRoomId(m.roomId)) !== String(normalizeRoomId(activeRoomId || ''))) {
                console.log('⚠️ Chat: 루임 ID 불일치로 메시지 무시');
                return;
            }
            
            // 중복 메시지 방지 (동일 ID 또는 내용+시간 기준)
            setMessages((prev) => {
                const messageExists = prev.some(existing => 
                    existing.id === (m.id ?? `${Date.now()}`) ||
                    (existing.content === String(m.content ?? '') && 
                     existing.senderId === (m.senderId ?? 'unknown') &&
                     Math.abs(new Date(existing.createdAt).getTime() - new Date(m.createdAt || new Date().toISOString()).getTime()) < 2000)
                );
                
                if (messageExists) {
                    console.log('🔁 Chat: 중복 메시지 방지');
                    return prev;
                }
                
                const newMsg = {
                    id: m.id ?? `${Date.now()}`,
                    roomId: normalizeRoomId(m.roomId),
                    senderId: m.senderId ?? 'unknown',
                    senderName: m.senderName ?? (String(m.senderId) === '0' ? '시스템' : '상대'),
                    content: String(m.content ?? ''),
                    type: m.type ?? 'TEXT',
                    createdAt: m.createdAt ?? new Date().toISOString(),
                };
                
                console.log('✅ Chat: 새 메시지 추가:', newMsg);
                const updatedMessages = [...prev, newMsg];
                
                // 캐시 업데이트 (roomId + 사용자 쌍 캐시 동시 업데이트)
                updateMessageCache(updatedMessages, activeRoomId || '');
                
                return updatedMessages;
            });
            
            setPeerTyping(false);
            setTimeout(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
        };

        const onWinTyping = (ev: any) => {
            const d = ev?.detail;
            if (String(normalizeRoomId(d?.roomId)) !== String(normalizeRoomId(activeRoomId || ''))) return;
            setPeerTyping(Boolean(d?.typing));
            if (d?.typing) setTimeout(() => setPeerTyping(false), 2500);
        };

        console.log('🔧 Chat: 이벤트 리스너 등록 - activeRoomId:', activeRoomId);
        
        // 메인 이벤트 리스너
        window.addEventListener('newChatMessage', onWinMsg as EventListener);
        window.addEventListener('chatTyping', onWinTyping as EventListener);

        return () => {
            console.log('🔧 Chat: 이벤트 리스너 제거');
            window.removeEventListener('newChatMessage', onWinMsg as EventListener);
            window.removeEventListener('chatTyping', onWinTyping as EventListener);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeRoomId, socket?.onMessage]);

    /* -------- 타이핑 표시(소켓) -------- */
    useEffect(() => {
        if (!activeRoomId || !isSocketReady || !socket?.sendTyping) return;
        if (!isTyping) return;
        const rid = String(activeRoomId); // UUID 그대로 사용
        socket.sendTyping(rid, true);
        const t = setTimeout(() => socket.sendTyping(rid, false), 1500);
        return () => clearTimeout(t);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isTyping, activeRoomId, isSocketReady]);

    const activeRoom = useMemo(
        () => rooms.find((r) => String(r.id) === String(activeRoomId)) || null,
        [rooms, activeRoomId]
    );

    // 채팅방 헤더 표시 이름 가져오기
    const getActiveRoomDisplayName = (): string => {
        if (activeRoom?.name) {
            return activeRoom.name;
        }
        
        if (activeRoomId) {
            // sessionStorage에서 매칭된 사용자 정보 확인
            try {
                const matchedUserData = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
                if (matchedUserData?.name) {
                    return `${matchedUserData.name}와의 채팅`;
                }
            } catch (e) {
                console.warn('매칭된 사용자 정보 파싱 실패:', e);
            }
            
            return `채팅방 ${activeRoomId}`;
        }
        
        return '채팅';
    };

    /* -------- 전송 -------- */
    const handleSend = async () => {
        const text = input.trim();
        if (!text || !activeRoomId || !me) return;
        
        // 채팅방 나간 후 메시지 전송 방지
        if (!activeRoomId) {
            toast.error('채팅방에 참여하고 있지 않습니다');
            return;
        }

        // 마지막 보안 검증
        const validation = validateReviewText(text);
        if (!validation.isValid) {
            toast.error(validation.error || '메시지에 허용되지 않는 내용이 포함되어 있습니다');
            return;
        }

        const sanitizedText = validation.sanitized;
        // UUID는 그대로 사용 (normalizeRoomId 제거)
        const rid = String(activeRoomId);

        try {
            setSending(true);
            setInput('');

            // 1) 소켓 우선 (UUID 그대로 전송)
            if (isSocketReady && socket?.sendChatMessage) {
                console.log('🚀 Chat: WebSocket으로 메시지 전송:', { rid, text: sanitizedText, userId: me.id });
                await socket.sendChatMessage(rid, sanitizedText);
                return;
            }

            // 2) REST fallback - API는 해시된 룸ID 사용
            const hashRoomId = normalizeRoomId(activeRoomId);
            console.log('📡 Chat: REST API로 메시지 전송 fallback:', { hashRoomId, text: sanitizedText, userId: me.id });
            const response = await api.post(`/api/chat/rooms/${hashRoomId}/messages`, null, {
                params: { senderId: me.id, content: sanitizedText },
            });
            
            // REST 전송 성공 시 UI에 직접 반영 (WebSocket이 안 될 때만)
            const serverMsg: ChatMessage = {
                id: response.data?.id || `${Date.now()}`,
                roomId: hashRoomId,
                senderId: me.id,
                senderName: me.name ?? '나',
                content: sanitizedText,
                type: 'TEXT',
                createdAt: response.data?.createdAt || new Date().toISOString(),
            };
            setMessages((prev) => {
                const updatedMessages = [...prev, serverMsg];
                
                // 캐시 업데이트 (roomId + 사용자 쌍 캐시 동시 업데이트)
                updateMessageCache(updatedMessages, activeRoomId);
                
                return updatedMessages;
            });
            setTimeout(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), 10);
        } catch {
            toast.error('메시지 전송 실패');
        } finally {
            setSending(false);
        }
    };

    /* -------- 파일 첨부(데모) -------- */
    const onAttach = () => {
        toast('파일 첨부는 곧 제공됩니다 📎');
    };

    /* -------- 채팅방 나가기 -------- */
    const handleLeaveRoom = async () => {
        if (!activeRoomId) return;
        
        const confirmed = window.confirm('채팅방을 나가시겠습니까?\n상대방도 더 이상 채팅을 할 수 없게 됩니다.');
        if (!confirmed) return;
        
        try {
            // 1. REST API를 통해 채팅방 나가기 처리 (먼저 서버에서 처리)
            await api.post(`/api/chat/rooms/${activeRoomId}/leave`);
            
            // 2. 상태 초기화 (세션은 유지)
            const currentActiveRoomId = activeRoomId;
            setActiveRoomId(null);
            setMessages([]);
            setRooms([]);
            
            // 3. 매칭 관련 데이터만 제거 (인증 세션은 보존)
            sessionStorage.removeItem('lastRoomId');
            sessionStorage.removeItem('matchedUser');
            localStorage.removeItem('chatRoomId');
            
            toast.success('채팅방에서 나가기 완료');
            
            // 4. 즉시 대시보드로 이동
            navigate('/dashboard', { replace: true });
            
        } catch (error: any) {
            console.error('채팅방 나가기 실패:', error);
            if (error.response?.status === 403) {
                toast.error('채팅방에 참여하고 있지 않습니다');
            } else if (error.response?.status === 401) {
                toast.error('로그인이 필요합니다');
            } else {
                toast.error('채팅방 나가기에 실패했습니다');
            }
        }
    };

    /* -------- 좌측: 방 클릭 -------- */
    const openRoom = (rid: string | number) => {
        setActiveRoomId(rid);
        sessionStorage.setItem('lastRoomId', String(rid));
        navigate(`/chat?roomId=${rid}`, { replace: true });
    };

    if (loading) {
        return (
            <div className="min-h-[60vh] grid place-items-center">
                <Loader2 className="w-6 h-6 animate-spin text-white" />
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* 좌측: 방 목록 */}
            <aside className="md:col-span-1 glass-card p-4">
                <div className="flex items-center gap-2 mb-3">
                    <ArrowLeft className="w-4 h-4 text-gray-400" />
                    <h2 className="font-semibold text-white">채팅방</h2>
                </div>
                <div className="space-y-2 max-h-[70vh] overflow-auto pr-1">
                    {rooms.map((r) => (
                        <button
                            key={r.id}
                            onClick={() => openRoom(r.id)}
                            className={`w-full text-left p-3 rounded-lg transition-all ${
                                String(r.id) === String(activeRoomId)
                                    ? 'bg-blue-600/80 text-white'
                                    : 'hover:bg-white/10 text-white/90'
                            }`}
                        >
                            <div className="flex items-center gap-3">
                                <div className="w-8 h-8 rounded-full bg-gradient-to-r from-purple-500 to-blue-500 grid place-items-center">
                                    <User className="w-4 h-4 text-white" />
                                </div>
                                <div className="flex-1">
                                    <div className="text-sm font-medium truncate">
                                        {r.name && r.name !== 'undefined' && !r.name.includes('undefined') 
                                            ? r.name 
                                            : `방 ${r.id}`}
                                    </div>
                                    {r.lastMessage && r.lastMessage !== 'undefined' ? (
                                        <div className="text-xs opacity-70 truncate">{r.lastMessage}</div>
                                    ) : null}
                                </div>
                                {r.unread ? (
                                    <span className="text-[10px] bg-red-500/90 text-white rounded-full px-2 py-0.5">
                    {r.unread}
                  </span>
                                ) : null}
                            </div>
                        </button>
                    ))}
                    {!rooms.length && <div className="text-sm text-gray-400">참여 중인 채팅방이 없습니다.</div>}
                </div>
            </aside>

            {/* 우측: 대화창 */}
            <section className="md:col-span-2 glass-card p-0 overflow-hidden">
                {/* 헤더 */}
                <div className="flex items-center gap-3 px-4 py-3 border-b border-white/10">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-r from-pink-500 to-violet-500 grid place-items-center">
                        <Music className="w-5 h-5 text-white" />
                    </div>
                    <div className="flex-1 min-w-0">
                        <div className="font-semibold truncate text-white" 
                             style={{ 
                                 textShadow: '0 2px 4px rgba(0, 0, 0, 0.9), 0 1px 2px rgba(0, 0, 0, 0.7)'
                             }}>
                            {getActiveRoomDisplayName()}
                        </div>
                        <div className="text-xs text-green-400 drop-shadow-sm mt-1">
                            {peerTyping ? '상대가 입력 중…' : isSocketReady ? '실시간 연결됨' : '오프라인 모드'}
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="text-xs text-white/70 flex items-center gap-1">
                            읽음 동기화
                            <CheckCheck className="w-4 h-4" />
                        </div>
                        {activeRoomId && (
                            <button
                                onClick={handleLeaveRoom}
                                className="p-2 rounded-lg bg-red-500/20 hover:bg-red-500/30 text-red-400 hover:text-red-300 transition-colors"
                                title="채팅방 나가기"
                            >
                                <LogOut className="w-4 h-4" />
                            </button>
                        )}
                    </div>
                </div>

                {/* 메시지 리스트 */}
                <div className="h-[60vh] overflow-y-auto px-4 py-4 space-y-2">
                    {!activeRoomId && (
                        <div className="text-center text-sm text-gray-400 mt-10">
                            왼쪽에서 채팅방을 선택하거나 매칭 후 <b>채팅 시작하기</b> 버튼을 눌러주세요.
                        </div>
                    )}

                    {activeRoomId &&
                        messages.map((m) => {
                            const mine = me ? String(m.senderId) === String(me.id) : false;
                            return <ChatMessageItem key={m.id} message={m} mine={mine} fetchUserBadge={fetchUserBadge} />;
                        })}
                    <div ref={endRef} />
                </div>

                {/* 입력창 */}
                <div className="px-4 py-3 border-t border-white/10">
                    <div className="flex items-center gap-2">
                        <button onClick={onAttach} className="p-2 rounded-lg hover:bg-white/10">
                            <Paperclip className="w-5 h-5 text-white/80" />
                        </button>
                        <button onClick={() => toast('이모지는 곧 제공됩니다 😄')} className="p-2 rounded-lg hover:bg-white/10">
                            <Smile className="w-5 h-5 text-white/80" />
                        </button>
                        <input
                            ref={inputRef}
                            value={input}
                            onChange={(e) => {
                                const rawValue = e.target.value;
                                const validation = validateReviewText(rawValue);
                                if (!validation.isValid) {
                                    toast.error(validation.error || '메시지에 허용되지 않는 내용이 포함되어 있습니다');
                                    return;
                                }
                                setInput(validation.sanitized);
                                setIsTyping(true);
                            }}
                            onKeyDown={(e) => {
                                // 스페이스바는 항상 허용
                                if (e.key === ' ') {
                                    // 스페이스바 입력 허용 (기본 동작 유지)
                                    return;
                                }
                                if (e.key === 'Enter' && !e.shiftKey) {
                                    e.preventDefault();
                                    handleSend();
                                }
                            }}
                            placeholder={activeRoomId ? '메시지를 입력하세요…' : '채팅방을 먼저 선택하세요'}
                            className="flex-1 bg-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none placeholder:text-white/50"
                            disabled={!activeRoomId || sending}
                        />
                        {/* 음악 공유 버튼 */}
                        <button
                            onClick={async () => {
                                const trackInfo = prompt("음악 정보를 입력하세요 (형식: 아티스트 - 곡제목)");
                                if (trackInfo && trackInfo.includes(" - ")) {
                                    const [artist, track] = trackInfo.split(" - ");
                                    console.log('🎵 음악 공유 시도:', { artist: artist.trim(), track: track.trim(), roomId: activeRoomId });
                                    
                                    if (socket?.shareMusic) {
                                        try {
                                            await socket.shareMusic(activeRoomId || '', {
                                                trackName: track.trim(),
                                                artistName: artist.trim(),
                                                spotifyUrl: ''
                                            });
                                            console.log('✅ 음악 공유 완료');
                                            toast.success(`🎵 ${artist.trim()} - ${track.trim()} 음악을 공유했습니다!`);
                                        } catch (error) {
                                            console.error('❌ 음악 공유 실패:', error);
                                            toast.error('음악 공유에 실패했습니다');
                                        }
                                    } else {
                                        console.warn('⚠️ WebSocket 연결되지 않음');
                                        toast.error('실시간 연결이 필요합니다');
                                    }
                                } else {
                                    toast.error('올바른 형식으로 입력해주세요 (예: IU - Blueming)');
                                }
                            }}
                            disabled={!activeRoomId || !socket?.isConnected}
                            className="px-4 py-2 bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 hover:text-purple-200 rounded-lg transition-colors disabled:opacity-50 border border-purple-500/30"
                            title="음악 공유"
                        >
                            <Music className="w-5 h-5" />
                        </button>
                        
                        <button
                            onClick={handleSend}
                            disabled={!activeRoomId || sending || !input.trim()}
                            className="btn-primary px-4 py-2 disabled:opacity-50"
                        >
                            {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                        </button>
                    </div>

                    {/* 퀵 리플라이 */}
                    <div className="flex flex-wrap gap-2 mt-3">
                        {['플리 공유할래요?', '요즘 뭐 들어요?', '공연 보러 가요!', '최애는 누구?'].map((t) => (
                            <button
                                key={t}
                                onClick={() => setInput((prev) => (prev ? prev + ' ' + t : t))}
                                className="px-2 py-1 text-xs rounded-full bg-white/10 text-white hover:bg-white/20"
                            >
                                {t}
                            </button>
                        ))}
                    </div>
                </div>
            </section>
        </div>
    );
};

export default Chat;
