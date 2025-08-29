// frontend/src/pages/Matching.tsx
import React, { useState, useEffect } from 'react';
import { Users, Heart, Music, Search, Clock, X, Check, Zap } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { useSocket } from '../context/SocketContext';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

type MatchStatus = 'IDLE' | 'WAITING' | 'MATCHED';

const Matching: React.FC = () => {
    const [matchingStatus, setMatchingStatus] = useState<MatchStatus>('IDLE');
    const [matchedUser, setMatchedUser] = useState<any>(null);
    const [waitingTime, setWaitingTime] = useState(0);
    const [queuePosition, setQueuePosition] = useState(0);
    const [isMatching, setIsMatching] = useState(false);

    const { isConnected, requestMatching } = useSocket();
    const navigate = useNavigate();

    // 로그인 사용자 ID (있으면 /api/auth/me 사용, 없으면 1)
    const [userId, setUserId] = useState<number>(1);

    useEffect(() => {
        (async () => {
            try {
                const { data } = await api.get('/api/auth/me');
                if (data?.authenticated && data?.user?.id) {
                    setUserId(Number(data.user.id));
                }
            } catch {
                // fallback: keep 1
            }
            checkMatchingStatus();
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        let interval: ReturnType<typeof setInterval> | undefined;
        if (matchingStatus === 'WAITING') {
            interval = setInterval(() => setWaitingTime((prev) => prev + 1), 1000);
        }
        return () => {
            if (interval) clearInterval(interval);
        };
    }, [matchingStatus]);

    // WebSocket 커스텀 이벤트 리스너
    useEffect(() => {
        const handleMatchingSuccess = (event: any) => {
            const roomId = event?.detail?.roomId;
            const mUser = event?.detail?.matchedUser || { id: 2, name: '음악친구', chatRoomId: roomId };
            setMatchingStatus('MATCHED');
            setMatchedUser(mUser);

            // ✅ 채팅 이동 대비: 세션에 저장
            if (roomId) {
                sessionStorage.setItem('lastRoomId', String(roomId));
            }
            sessionStorage.setItem('matchedUser', JSON.stringify({ id: mUser.id, name: mUser.name }));
        };

        const handleMatchingFailed = () => {
            setMatchingStatus('IDLE');
        };

        window.addEventListener('matchingSuccess', handleMatchingSuccess as EventListener);
        window.addEventListener('matchingFailed', handleMatchingFailed as EventListener);

        return () => {
            window.removeEventListener('matchingSuccess', handleMatchingSuccess as EventListener);
            window.removeEventListener('matchingFailed', handleMatchingFailed as EventListener);
        };
    }, []);

    const checkMatchingStatus = async () => {
        try {
            const { data } = await api.get(`/api/realtime-matching/status/${userId}`);
            const status: MatchStatus = data?.status || 'IDLE';

            setMatchingStatus(status);
            if (status === 'MATCHED') {
                const roomId = data?.roomId;
                const mUser = {
                    id: data?.matchedWith ?? 2,
                    name: '음악친구',
                    chatRoomId: roomId,
                };
                setMatchedUser(mUser);

                // ✅ 세션 저장
                if (roomId) {
                    sessionStorage.setItem('lastRoomId', String(roomId));
                }
                sessionStorage.setItem('matchedUser', JSON.stringify({ id: mUser.id, name: mUser.name }));
            } else if (status === 'WAITING') {
                setQueuePosition(data?.queuePosition || 0);
            }
        } catch (error) {
            console.error('매칭 상태 확인 오류:', error);
        }
    };

    const startMatching = async () => {
        try {
            setIsMatching(true);

            // WebSocket 실시간 매칭
            if (isConnected && requestMatching) {
                requestMatching();
                setMatchingStatus('WAITING');
                setWaitingTime(0);
                setQueuePosition(1);
                toast.success('실시간 매칭 요청 전송!');
            } else {
                // 폴백: HTTP
                const { data } = await api.post(`/api/realtime-matching/request/${userId}`);
                if (data?.success) {
                    setMatchingStatus('WAITING');
                    setWaitingTime(0);
                    setQueuePosition(data?.queuePosition || 1);
                    toast.success('매칭 요청이 시작되었습니다!');
                } else {
                    toast.error(data?.message || '매칭 요청에 실패했습니다');
                }
            }
        } catch (error) {
            console.error('매칭 시작 오류:', error);
            toast.error('매칭 요청 중 오류가 발생했습니다');
        } finally {
            setIsMatching(false);
        }
    };

    const cancelMatching = async () => {
        try {
            await api.delete(`/api/realtime-matching/cancel/${userId}`);
            setMatchingStatus('IDLE');
            setWaitingTime(0);
            setQueuePosition(0);
            toast.success('매칭이 취소되었습니다');
        } catch (error) {
            console.error('매칭 취소 오류:', error);
            toast.error('매칭 취소 중 오류가 발생했습니다');
        }
    };

    const endMatching = async () => {
        try {
            await api.delete(`/api/realtime-matching/end/${userId}`);
            setMatchingStatus('IDLE');
            setMatchedUser(null);
            toast.success('매칭이 종료되었습니다');
        } catch (error) {
            console.error('매칭 종료 오류:', error);
            toast.error('매칭 종료 중 오류가 발생했습니다');
        }
    };

    const createDemoMatch = async () => {
        try {
            const { data } = await api.post(
                '/api/realtime-matching/demo/quick-match',
                null,
                { params: { user1Id: userId, user2Id: 2 } }
            );
            if (data?.success) {
                const roomId = data?.demoMatch?.room;
                const mUser = { id: 2, name: '데모 음악친구', chatRoomId: roomId };
                setMatchingStatus('MATCHED');
                setMatchedUser(mUser);

                // ✅ 세션 저장
                if (roomId) {
                    sessionStorage.setItem('lastRoomId', String(roomId));
                }
                sessionStorage.setItem('matchedUser', JSON.stringify({ id: mUser.id, name: mUser.name }));

                toast.success('🎉 데모 매칭 성공!');
            } else {
                toast.error(data?.message || '데모 매칭 실패');
            }
        } catch (error) {
            console.error('데모 매칭 오류:', error);
            toast.error('데모 매칭 생성 중 오류가 발생했습니다');
        }
    };

    const goChat = () => {
        const roomId = matchedUser?.chatRoomId || sessionStorage.getItem('lastRoomId');
        if (roomId) {
            // 안전을 위해 다시 저장
            sessionStorage.setItem('lastRoomId', String(roomId));
            if (matchedUser?.id && matchedUser?.name) {
                sessionStorage.setItem('matchedUser', JSON.stringify({ id: matchedUser.id, name: matchedUser.name }));
            }
            navigate(`/chat?roomId=${roomId}`);
        } else {
            navigate('/chat'); // 방 정보 없으면 채팅으로만 이동
        }
    };

    const formatTime = (seconds: number) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    return (
        <div className="max-w-4xl mx-auto space-y-8 animate-fade-in">
            {/* 헤더 */}
            <div className="text-center space-y-4">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-blue-400 to-purple-600 bg-clip-text text-transparent">
                    🎵 음악 매칭
                </h1>
                <p className="text-lg text-gray-600 dark:text-gray-300">
                    비슷한 음악 취향을 가진 사람들과 실시간으로 연결되세요
                </p>
            </div>

            {/* 매칭 상태별 UI */}
            {matchingStatus === 'IDLE' && (
                <div className="space-y-6">
                    {/* 매칭 시작 카드 */}
                    <div className="glass-card p-8 text-center">
                        <div className="w-24 h-24 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center mx-auto mb-6">
                            <Search className="h-12 w-12 text-white" />
                        </div>
                        <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-4">새로운 음악 친구 찾기</h2>
                        <p className="text-gray-600 dark:text-gray-400 mb-8 max-w-2xl mx-auto">
                            AI가 분석한 당신의 음악 취향을 바탕으로 완벽한 음악 파트너를 찾아드립니다.
                            비슷한 장르, 아티스트, 감성을 공유하는 사람들과 만나보세요.
                        </p>

                        <div className="space-y-4">
                            <button className="btn-primary text-lg px-8 py-4" onClick={startMatching} disabled={isMatching}>
                                {isMatching ? (
                                    <>
                                        <div className="loading-spinner w-5 h-5 mr-2"></div>
                                        매칭 시작 중...
                                    </>
                                ) : (
                                    <>
                                        <Heart className="h-5 w-5 mr-2" />
                                        매칭 시작하기
                                    </>
                                )}
                            </button>

                            <button className="btn-secondary ml-4" onClick={createDemoMatch}>
                                <Zap className="h-4 w-4 mr-2" />
                                데모 매칭 (테스트)
                            </button>
                        </div>
                    </div>

                    {/* 매칭 방식 설명 */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="glass-card p-6 text-center">
                            <Music className="h-12 w-12 text-blue-500 mx-auto mb-4" />
                            <h3 className="font-bold text-gray-800 dark:text-white mb-2">음악 취향 분석</h3>
                            <p className="text-gray-600 dark:text-gray-400 text-sm">선호하는 장르, 아티스트, 분위기를 AI가 정밀 분석</p>
                        </div>
                        <div className="glass-card p-6 text-center">
                            <Users className="h-12 w-12 text-purple-500 mx-auto mb-4" />
                            <h3 className="font-bold text-gray-800 dark:text-white mb-2">스마트 매칭</h3>
                            <p className="text-gray-600 dark:text-gray-400 text-sm">유사도 70% 이상의 높은 호환성을 가진 사용자 매칭</p>
                        </div>
                        <div className="glass-card p-6 text-center">
                            <Heart className="h-12 w-12 text-pink-500 mx-auto mb-4" />
                            <h3 className="font-bold text-gray-800 dark:text-white mb-2">실시간 연결</h3>
                            <p className="text-gray-600 dark:text-gray-400 text-sm">매칭 즉시 1:1 채팅방 생성 및 음악 공유 가능</p>
                        </div>
                    </div>
                </div>
            )}

            {matchingStatus === 'WAITING' && (
                <div className="space-y-6">
                    {/* 매칭 대기 중 */}
                    <div className="glass-card p-8 text-center">
                        <div className="w-32 h-32 mx-auto mb-6 relative">
                            <div className="w-full h-full border-8 border-blue-200 dark:border-blue-800 rounded-full animate-pulse"></div>
                            <div className="absolute inset-4 border-8 border-purple-500 rounded-full animate-spin border-t-transparent"></div>
                            <Search className="absolute inset-0 m-auto h-12 w-12 text-blue-500 animate-bounce" />
                        </div>

                        <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-4">🔍 완벽한 음악 파트너를 찾는 중...</h2>

                        <div className="space-y-4">
                            <div className="flex items-center justify-center space-x-6 text-lg">
                                <div className="flex items-center text-blue-500">
                                    <Clock className="h-5 w-5 mr-2" />
                                    {formatTime(waitingTime)}
                                </div>
                                <div className="flex items-center text-purple-500">
                                    <Users className="h-5 w-5 mr-2" />
                                    대기열 {queuePosition}번째
                                </div>
                            </div>

                            <p className="text-gray-600 dark:text-gray-400">비슷한 음악 취향을 가진 사용자를 찾고 있습니다...</p>

                            <button className="btn-secondary" onClick={cancelMatching}>
                                <X className="h-4 w-4 mr-2" />
                                매칭 취소
                            </button>
                        </div>
                    </div>

                    {/* 매칭 진행 상황 */}
                    <div className="glass-card p-6">
                        <h3 className="font-bold text-gray-800 dark:text-white mb-4">매칭 진행 상황</h3>
                        <div className="space-y-3">
                            <div className="flex items-center text-green-500">
                                <Check className="h-5 w-5 mr-3" />
                                음악 취향 분석 완료
                            </div>
                            <div className="flex items-center text-blue-500">
                                <div className="loading-spinner w-4 h-4 mr-3"></div>
                                호환 가능한 사용자 검색 중
                            </div>
                            <div className="flex items-center text-gray-400">
                                <Clock className="h-5 w-5 mr-3" />
                                매칭 완료 대기 중
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {matchingStatus === 'MATCHED' && matchedUser && (
                <div className="space-y-6">
                    {/* 매칭 성공 */}
                    <div className="glass-card p-8 text-center">
                        <div className="w-24 h-24 bg-gradient-to-r from-green-400 to-blue-500 rounded-full flex items-center justify-center mx-auto mb-6 animate-bounce">
                            <Heart className="h-12 w-12 text-white" />
                        </div>

                        <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-4">🎉 매칭 성공!</h2>

                        <div className="bg-white/5 dark:bg-gray-800/30 rounded-2xl p-6 mb-6">
                            <div className="flex items-center justify-center space-x-4">
                                <div className="w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
                                    <Music className="h-8 w-8 text-white" />
                                </div>
                                <div className="text-center">
                                    <h3 className="font-bold text-gray-800 dark:text-white text-lg">{matchedUser.name}</h3>
                                    <p className="text-green-500 font-medium">85% 음악 취향 일치</p>
                                    <p className="text-gray-600 dark:text-gray-400 text-sm">공통 관심사: K-POP, 인디, R&B</p>
                                </div>
                            </div>
                        </div>

                        <div className="space-y-4">
                            <button className="btn-primary text-lg px-8 py-4" onClick={goChat}>
                                <Heart className="h-5 w-5 mr-2" />
                                채팅 시작하기
                            </button>

                            <button className="btn-secondary ml-4" onClick={endMatching}>
                                <X className="h-4 w-4 mr-2" />
                                매칭 종료
                            </button>
                        </div>
                    </div>

                    {/* 매칭 정보 */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="glass-card p-6">
                            <h3 className="font-bold text-gray-800 dark:text-white mb-4">공통 관심사</h3>
                            <div className="space-y-2">
                                <div className="flex items-center">
                                    <Music className="h-4 w-4 text-blue-500 mr-2" />
                                    <span className="text-gray-600 dark:text-gray-400">K-POP</span>
                                </div>
                                <div className="flex items-center">
                                    <Music className="h-4 w-4 text-purple-500 mr-2" />
                                    <span className="text-gray-600 dark:text-gray-400">인디 음악</span>
                                </div>
                                <div className="flex items-center">
                                    <Music className="h-4 w-4 text-pink-500 mr-2" />
                                    <span className="text-gray-600 dark:text-gray-400">R&B/Soul</span>
                                </div>
                            </div>
                        </div>

                        <div className="glass-card p-6">
                            <h3 className="font-bold text-gray-800 dark:text-white mb-4">추천 활동</h3>
                            <div className="space-y-2 text-gray-600 dark:text-gray-400">
                                <p>🎵 서로의 플레이리스트 공유하기</p>
                                <p>🎤 좋아하는 아티스트 이야기하기</p>
                                <p>🎶 새로운 음악 추천해주기</p>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* 시스템 상태 (데모) */}
            <div className="glass-card p-6">
                <h3 className="font-bold text-gray-800 dark:text-white mb-4">매칭 시스템 현황</h3>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
                    <div>
                        <p className="text-2xl font-bold text-blue-500">25</p>
                        <p className="text-gray-600 dark:text-gray-400 text-sm">대기 중</p>
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-green-500">12</p>
                        <p className="text-gray-600 dark:text-gray-400 text-sm">매칭 완료</p>
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-purple-500">87</p>
                        <p className="text-gray-600 dark:text-gray-400 text-sm">온라인</p>
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-pink-500">95%</p>
                        <p className="text-gray-600 dark:text-gray-400 text-sm">만족도</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Matching;
