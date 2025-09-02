// frontend/src/pages/Chat.tsx
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Send, Paperclip, Smile, User, Loader2, Music, ArrowLeft, CheckCheck } from 'lucide-react';
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

/* ===================== 서브 컴포넌트 ===================== */
const ChatMessageItem: React.FC<{
    message: ChatMessage;
    mine: boolean;
    fetchUserBadge: (userId: number) => Promise<RepresentativeBadge | null>;
}> = ({ message, mine, fetchUserBadge }) => {
    const [badge, setBadge] = useState<RepresentativeBadge | null>(null);
    const [badgeLoading, setBadgeLoading] = useState(false);

    useEffect(() => {
        if (!mine && message.senderId && String(message.senderId) !== '0') {
            setBadgeLoading(true);
            fetchUserBadge(Number(message.senderId))
                .then(setBadge)
                .finally(() => setBadgeLoading(false));
        }
    }, [message.senderId, mine, fetchUserBadge]);

    return (
        <div className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${
                mine ? 'bg-blue-600 text-white' : 'bg-white/10 text-white'
            }`}>
                {!mine && (
                    <div className="flex items-center gap-2 mb-1">
                        <div className="text-[10px] opacity-70">
                            {message.senderName ?? (String(message.senderId) === '0' ? '시스템' : '상대')}
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

        try {
            const { data } = await api.get(`/api/users/${userId}/representative-badge`);
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

    /* -------- URL/세션에서 roomId 복원 -------- */
    useEffect(() => {
        const rid = query.get('roomId') ?? sessionStorage.getItem('lastRoomId');
        if (rid) setActiveRoomId(rid);
    }, [query]);

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
                ? data.map((r: any) => ({
                    id: r.id ?? r.roomId,
                    name: r.name ?? `방 ${r.id ?? r.roomId}`,
                    lastMessage: r.lastMessage ?? '',
                    unread: r.unread ?? 0,
                }))
                : [];
            setRooms(list);
        } catch {
            // 폴백: 현재 roomId 하나만 노출
            const rid = query.get('roomId') ?? sessionStorage.getItem('lastRoomId') ?? 'room_2_2';
            const matched = JSON.parse(sessionStorage.getItem('matchedUser') || 'null');
            setRooms([
                {
                    id: rid,
                    name: matched?.name ? `${matched.name}와의 채팅` : `채팅방 ${rid}`,
                    lastMessage: '',
                    unread: 0,
                },
            ]);
        }
    };

    /* -------- 메시지 로드 -------- */
    const loadMessages = async (roomId: string | number) => {
        const rid = normalizeRoomId(roomId);
        try {
            const { data } = await api.get(`/api/chat/rooms/${rid}/messages`, {
                params: { limit: 100, order: 'asc' },
            });
            const list: ChatMessage[] = Array.isArray(data)
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
            setMessages(list);
        } catch {
            setMessages([]); // API 없으면 빈 대화에서 시작
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
            if (!m || String(normalizeRoomId(m.roomId)) !== String(normalizeRoomId(activeRoomId || ''))) return;
            setMessages((prev) => [
                ...prev,
                {
                    id: m.id ?? `${Date.now()}`,
                    roomId: normalizeRoomId(m.roomId),
                    senderId: m.senderId ?? 'unknown',
                    senderName: m.senderName ?? (String(m.senderId) === '0' ? '시스템' : '상대'),
                    content: String(m.content ?? ''),
                    type: m.type ?? 'TEXT',
                    createdAt: m.createdAt ?? new Date().toISOString(),
                },
            ]);
            setPeerTyping(false);
            setTimeout(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), 10);
        };

        const onWinTyping = (ev: any) => {
            const d = ev?.detail;
            if (String(normalizeRoomId(d?.roomId)) !== String(normalizeRoomId(activeRoomId || ''))) return;
            setPeerTyping(Boolean(d?.typing));
            if (d?.typing) setTimeout(() => setPeerTyping(false), 2500);
        };

        console.log('🔧 Chat: 이벤트 리스너 등록 - activeRoomId:', activeRoomId);
        
        window.addEventListener('newChatMessage', onWinMsg as EventListener);
        window.addEventListener('chatTyping', onWinTyping as EventListener);
        
        // 디버깅을 위해 chatMessage도 등록 (혹시나)
        window.addEventListener('chatMessage', onWinMsg as EventListener);

        // SocketContext 쪽 리스너도 있으면 사용
        let offSocketMsg: any;
        if (socket?.onMessage) {
            offSocketMsg = socket.onMessage(onWinMsg);
        }

        return () => {
            console.log('🔧 Chat: 이벤트 리스너 제거');
            window.removeEventListener('newChatMessage', onWinMsg as EventListener);
            window.removeEventListener('chatMessage', onWinMsg as EventListener);
            window.removeEventListener('chatTyping', onWinTyping as EventListener);
            if (offSocketMsg) offSocketMsg();
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeRoomId, socket?.onMessage]);

    /* -------- 타이핑 표시(소켓) -------- */
    useEffect(() => {
        if (!activeRoomId || !isSocketReady || !socket?.sendTyping) return;
        if (!isTyping) return;
        const rid = normalizeRoomId(activeRoomId);
        socket.sendTyping(rid, true);
        const t = setTimeout(() => socket.sendTyping(rid, false), 1500);
        return () => clearTimeout(t);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isTyping, activeRoomId, isSocketReady]);

    const activeRoom = useMemo(
        () => rooms.find((r) => String(r.id) === String(activeRoomId)) || null,
        [rooms, activeRoomId]
    );

    /* -------- 전송 -------- */
    const handleSend = async () => {
        const text = input.trim();
        if (!text || !activeRoomId || !me) return;

        // 마지막 보안 검증
        const validation = validateReviewText(text);
        if (!validation.isValid) {
            toast.error(validation.error || '메시지에 허용되지 않는 내용이 포함되어 있습니다');
            return;
        }

        const sanitizedText = validation.sanitized;
        const rid = normalizeRoomId(activeRoomId);

        // UI에 먼저 낙관적 반영 (정리된 텍스트 사용)
        const localMsg: ChatMessage = {
            id: `${Date.now()}`,
            roomId: rid,
            senderId: me.id,
            senderName: me.name ?? '나',
            content: sanitizedText,
            type: 'TEXT',
            createdAt: new Date().toISOString(),
        };

        try {
            setSending(true);
            setMessages((prev) => [...prev, localMsg]);
            setInput('');
            setTimeout(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), 10);

            // 1) 소켓 우선 (정리된 텍스트 전송)
            if (isSocketReady && socket?.sendChatMessage) {
                await socket.sendChatMessage(rid, sanitizedText);
                return;
            }

            // 2) REST fallback (백엔드가 @RequestParam senderId, content 받음)
            await api.post(`/api/chat/rooms/${rid}/messages`, null, {
                params: { senderId: me.id, content: sanitizedText },
            });
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
                    <h2 className="font-semibold">채팅방</h2>
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
                                    <div className="text-sm font-medium truncate">{r.name ?? `방 ${r.id}`}</div>
                                    {r.lastMessage ? (
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
                        <div className="font-semibold truncate">
                            {activeRoom?.name ?? (activeRoomId ? `채팅방 ${activeRoomId}` : '채팅')}
                        </div>
                        <div className="text-xs text-green-400">
                            {peerTyping ? '상대가 입력 중…' : isSocketReady ? '실시간 연결됨' : '오프라인 모드'}
                        </div>
                    </div>
                    <div className="text-xs text-white/70 flex items-center gap-1">
                        읽음 동기화
                        <CheckCheck className="w-4 h-4" />
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
                                if (e.key === 'Enter' && !e.shiftKey) {
                                    e.preventDefault();
                                    handleSend();
                                }
                            }}
                            placeholder={activeRoomId ? '메시지를 입력하세요…' : '채팅방을 먼저 선택하세요'}
                            className="flex-1 bg-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none placeholder:text-white/50"
                            disabled={!activeRoomId || sending}
                        />
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
