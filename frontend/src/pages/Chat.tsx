import React, { useState, useEffect, useRef } from 'react';
import { Send, Music, Heart, Smile, Paperclip, MoreVertical } from 'lucide-react';
import axios from 'axios';
import { toast } from 'react-hot-toast';
import { useSocket } from '../context/SocketContext';

interface Message {
    id: string;
    sender: string;
    content: string;
    type: 'text' | 'music' | 'system';
    timestamp: string;
    isOwn?: boolean;
}

const Chat = () => {
    const [messages, setMessages] = useState<Message[]>([]);
    const [newMessage, setNewMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);
    const [roomInfo, setRoomInfo] = useState<any>(null);
    const [isTyping, setIsTyping] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const { socket } = useSocket();

    useEffect(() => {
        loadChatRoom();

        // 시뮬레이션 메시지 추가
        const simulatedMessages: Message[] = [
            {
                id: '1',
                sender: 'system',
                content: '🎵 음악 매칭 채팅방에 오신 것을 환영합니다!',
                type: 'system',
                timestamp: new Date().toISOString()
            },
            {
                id: '2',
                sender: '음악친구',
                content: '안녕하세요! 반가워요 😊',
                type: 'text',
                timestamp: new Date().toISOString(),
                isOwn: false
            },
            {
                id: '3',
                sender: '뮤직러버',
                content: '안녕하세요! 어떤 음악 좋아하세요?',
                type: 'text',
                timestamp: new Date().toISOString(),
                isOwn: true
            }
        ];
        setMessages(simulatedMessages);
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    // WebSocket 이벤트 리스너
    useEffect(() => {
        if (socket) {
            socket.on('newMessage', (message: Message) => {
                setMessages(prev => [...prev, { ...message, isOwn: false }]);
                setIsTyping(false);
            });

            socket.on('userTyping', (data: any) => {
                setIsTyping(data.isTyping);
            });

            socket.on('connect', () => {
                setIsConnected(true);
            });

            socket.on('disconnect', () => {
                setIsConnected(false);
            });

            return () => {
                socket.off('newMessage');
                socket.off('userTyping');
                socket.off('connect');
                socket.off('disconnect');
            };
        }
    }, [socket]);

    const loadChatRoom = async () => {
        try {
            const response = await axios.get('/api/realtime-matching/chat/room/1');
            if (response.data.hasActiveChatRoom) {
                setRoomInfo(response.data.roomInformation);
            }
        } catch (error) {
            console.error('채팅방 정보 로드 오류:', error);
        }
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const sendMessage = async () => {
        if (!newMessage.trim()) return;

        const message: Message = {
            id: Date.now().toString(),
            sender: '뮤직러버',
            content: newMessage,
            type: 'text',
            timestamp: new Date().toISOString(),
            isOwn: true
        };

        setMessages(prev => [...prev, message]);
        setNewMessage('');

        // WebSocket으로 메시지 전송
        if (socket) {
            socket.emit('sendMessage', {
                roomId: 'room_1_2',
                content: newMessage,
                type: 'text'
            });
        }

        // 시뮬레이션: 자동 응답
        setTimeout(() => {
            const responses = [
                '그 음악 정말 좋죠! 저도 좋아해요 🎵',
                '오 새로운 장르네요! 추천해주셔서 감사해요',
                '비슷한 취향이네요 😊',
                '그 아티스트 컨서트 가보셨나요?',
                '플레이리스트 공유해주실 수 있나요?'
            ];

            const randomResponse = responses[Math.floor(Math.random() * responses.length)];
            const responseMessage: Message = {
                id: (Date.now() + 1).toString(),
                sender: '음악친구',
                content: randomResponse,
                type: 'text',
                timestamp: new Date().toISOString(),
                isOwn: false
            };

            setMessages(prev => [...prev, responseMessage]);
        }, 1000 + Math.random() * 2000);

        toast.success('메시지가 전송되었습니다!');
    };

    const shareMusic = () => {
        const musicMessage: Message = {
            id: Date.now().toString(),
            sender: '뮤직러버',
            content: '🎵 IU - Love poem (Spotify)',
            type: 'music',
            timestamp: new Date().toISOString(),
            isOwn: true
        };

        setMessages(prev => [...prev, musicMessage]);
        toast.success('음악이 공유되었습니다! 🎵');
    };

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    };

    const formatTime = (timestamp: string) => {
        return new Date(timestamp).toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div className="max-w-4xl mx-auto h-[80vh] flex flex-col animate-fade-in">
            {/* 채팅방 헤더 */}
            <div className="glass-card p-4 rounded-b-none border-b border-white/10">
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                        <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
                            <Music className="h-6 w-6 text-white" />
                        </div>
                        <div>
                            <h2 className="font-bold text-gray-800 dark:text-white">
                                음악 매칭 채팅방
                            </h2>
                            <div className="flex items-center space-x-2 text-sm">
                                <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
                                <span className="text-gray-600 dark:text-gray-400">
                  {isConnected ? '연결됨' : '연결 끊김'}
                </span>
                                {isTyping && (
                                    <span className="text-blue-500 animate-pulse">
                    상대방이 입력 중...
                  </span>
                                )}
                            </div>
                        </div>
                    </div>

                    <button className="p-2 hover:bg-white/10 rounded-lg transition-colors">
                        <MoreVertical className="h-5 w-5 text-gray-600 dark:text-gray-400" />
                    </button>
                </div>
            </div>

            {/* 메시지 영역 */}
            <div className="flex-1 glass-card rounded-t-none rounded-b-none overflow-y-auto p-4 space-y-4">
                {messages.map((message) => (
                    <div
                        key={message.id}
                        className={`flex ${message.type === 'system' ? 'justify-center' : message.isOwn ? 'justify-end' : 'justify-start'}`}
                    >
                        {message.type === 'system' ? (
                            <div className="bg-blue-500/20 text-blue-600 dark:text-blue-400 px-4 py-2 rounded-full text-sm">
                                {message.content}
                            </div>
                        ) : (
                            <div
                                className={`max-w-xs lg:max-w-md px-4 py-3 rounded-2xl message-slide-in ${
                                    message.isOwn
                                        ? 'bg-gradient-to-r from-blue-500 to-purple-600 text-white'
                                        : 'bg-white/10 dark:bg-gray-800/50 text-gray-800 dark:text-white'
                                }`}
                            >
                                {!message.isOwn && (
                                    <p className="text-xs opacity-70 mb-1">{message.sender}</p>
                                )}

                                {message.type === 'music' ? (
                                    <div className="flex items-center space-x-2">
                                        <Music className="h-4 w-4" />
                                        <span>{message.content}</span>
                                    </div>
                                ) : (
                                    <p>{message.content}</p>
                                )}

                                <p className={`text-xs mt-1 ${message.isOwn ? 'opacity-70' : 'opacity-50'}`}>
                                    {formatTime(message.timestamp)}
                                </p>
                            </div>
                        )}
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            {/* 메시지 입력 영역 */}
            <div className="glass-card p-4 rounded-t-none">
                <div className="flex items-center space-x-3">
                    <button
                        onClick={shareMusic}
                        className="p-2 hover:bg-white/10 rounded-lg transition-colors group"
                        title="음악 공유"
                    >
                        <Music className="h-5 w-5 text-gray-600 dark:text-gray-400 group-hover:text-blue-500" />
                    </button>

                    <button className="p-2 hover:bg-white/10 rounded-lg transition-colors group">
                        <Paperclip className="h-5 w-5 text-gray-600 dark:text-gray-400 group-hover:text-purple-500" />
                    </button>

                    <div className="flex-1 relative">
                        <input
                            type="text"
                            value={newMessage}
                            onChange={(e) => setNewMessage(e.target.value)}
                            onKeyPress={handleKeyPress}
                            placeholder="메시지를 입력하세요..."
                            className="input-field w-full pr-12"
                            disabled={!isConnected}
                        />
                        <button className="absolute right-3 top-1/2 transform -translate-y-1/2 p-1 hover:bg-white/10 rounded transition-colors">
                            <Smile className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                        </button>
                    </div>

                    <button
                        onClick={sendMessage}
                        disabled={!newMessage.trim() || !isConnected}
                        className="btn-primary px-4 py-3 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <Send className="h-4 w-4" />
                    </button>
                </div>

                <div className="mt-2 text-xs text-gray-500 dark:text-gray-400 text-center">
                    Enter로 전송 • Shift+Enter로 줄바꿈
                </div>
            </div>

            {/* 채팅방 없는 경우 */}
            {!roomInfo && messages.length === 0 && (
                <div className="flex-1 flex items-center justify-center">
                    <div className="text-center space-y-4">
                        <div className="w-24 h-24 bg-gradient-to-r from-gray-400 to-gray-600 rounded-full flex items-center justify-center mx-auto">
                            <Music className="h-12 w-12 text-white" />
                        </div>
                        <h3 className="text-xl font-bold text-gray-800 dark:text-white">
                            활성화된 채팅방이 없습니다
                        </h3>
                        <p className="text-gray-600 dark:text-gray-400">
                            먼저 매칭을 완료해주세요
                        </p>
                        <button
                            className="btn-primary"
                            onClick={() => window.location.href = '/matching'}
                        >
                            매칭 하러가기
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Chat;