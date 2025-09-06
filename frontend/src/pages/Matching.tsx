// frontend/src/pages/Matching.tsx
import React, { useState, useEffect } from 'react';
import { Users, Heart, Music, Search, Clock, X, Check, Zap, RefreshCw, AlertCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { useSocket } from '../context/SocketContext';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

type MatchStatus = 'IDLE' | 'WAITING' | 'MATCHED';

type RepresentativeBadge = {
    id: number;
    badgeType: string;
    badgeName: string;
    description: string;
    iconUrl?: string;
    rarity: string;
    badgeColor: string;
};

const Matching: React.FC = () => {
    const [matchingStatus, setMatchingStatus] = useState<MatchStatus>('IDLE');
    const [matchedUser, setMatchedUser] = useState<any>(null);
    const [waitingTime, setWaitingTime] = useState(0);
    const [queuePosition, setQueuePosition] = useState(0);
    const [isMatching, setIsMatching] = useState(false);

    // 매칭 시스템 현황 상태
    const [systemStats, setSystemStats] = useState({
        totalWaiting: 0,
        totalMatched: 0,
        onlineUsers: 0
    });
    const [nextUpdateCountdown, setNextUpdateCountdown] = useState<number>(30);
    const [lastUpdateTime, setLastUpdateTime] = useState<string>('');
    const [statsUpdateError, setStatsUpdateError] = useState<boolean>(false);

    const { isConnected, requestMatching } = useSocket();
    const navigate = useNavigate();

    // 로그인 사용자 ID (있으면 /api/auth/me 사용, 없으면 1)
    const [userId, setUserId] = useState<number>(1);
    
    // 매칭된 사용자의 대표 배지
    const [matchedUserBadge, setMatchedUserBadge] = useState<RepresentativeBadge | null>(null);

    useEffect(() => {
        (async () => {
            try {
                // OAuth 인증 확인
                const { data } = await api.get('/api/auth/me');
                console.log('🔍 OAuth 인증 응답:', data);
                if (data?.authenticated && data?.user?.id) {
                    console.log('✅ OAuth 인증 성공, userId 설정:', data.user.id);
                    setUserId(Number(data.user.id));
                } else {
                    // 로컬 인증 확인
                    try {
                        const localAuthResponse = await api.get('/api/auth/local/me');
                        console.log('🔍 로컬 인증 응답:', localAuthResponse.data);
                        if (localAuthResponse.data?.success && localAuthResponse.data?.user?.id) {
                            console.log('✅ 로컬 인증 성공, userId 설정:', localAuthResponse.data.user.id);
                            setUserId(Number(localAuthResponse.data.user.id));
                        } else {
                            console.log('❌ 로컬 인증 실패, userId=1 유지');
                        }
                    } catch (localError) {
                        console.log('❌ 로컬 인증 오류:', localError);
                    }
                }
            } catch (oauthError) {
                console.log('❌ OAuth 인증 오류:', oauthError);
            }
            
            // 🧹 기존 데이터 클리어 - 새로운 UUID 기반 시스템으로 완전 전환
            clearLegacyData();
            
            checkMatchingStatus();
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);
    
    // 기존 숫자형 roomId 및 관련 데이터 클리어
    const clearLegacyData = () => {
        const lastRoomId = sessionStorage.getItem('lastRoomId');
        
        // 기존 숫자형 roomId인 경우 클리어 (중복 메시지 방지를 위해 로그만)
        if (lastRoomId && /^\d+$/.test(lastRoomId)) {
            console.log('🧹 기존 숫자형 roomId 클리어:', lastRoomId);
            sessionStorage.removeItem('lastRoomId');
            sessionStorage.removeItem('matchedUser');
            localStorage.removeItem('chatRoomId');
            // 토스트 메시지 제거 - 중복 메시지 방지
        }
    };

    useEffect(() => {
        let interval: ReturnType<typeof setInterval> | undefined;
        if (matchingStatus === 'WAITING') {
            interval = setInterval(() => setWaitingTime((prev) => prev + 1), 1000);
        }
        return () => {
            if (interval) clearInterval(interval);
        };
    }, [matchingStatus]);

    // WebSocket 커스텀 이벤트 리스너 (강화된 중복 방지)
    useEffect(() => {
        let processedEvents = new Set<string>(); // 처리된 이벤트 추적
        
        const handleMatchingSuccess = (event: any) => {
            const data = event?.detail;
            const roomId = data?.roomId || data?.matchedUser?.roomId;
            const eventKey = `${roomId}_${data?.matchedUser?.id || 'unknown'}`;
            
            // 이미 처리된 이벤트인지 확인
            if (processedEvents.has(eventKey)) {
                console.log('🔄 중복된 매칭 성공 이벤트 무시:', eventKey);
                return;
            }
            
            processedEvents.add(eventKey);
            console.log('🎉 새로운 매칭 성공 이벤트 처리:', eventKey, event.detail);
            
            const matchedUserData = data?.matchedUser || { id: 2, name: '음악친구' };
            const mUser = {
                id: matchedUserData.id,
                name: matchedUserData.name,
                chatRoomId: roomId
            };
            
            setMatchingStatus('MATCHED');
            setMatchedUser(mUser);

            // ✅ 채팅 이동 대비: 세션에 저장
            if (roomId) {
                sessionStorage.setItem('lastRoomId', String(roomId));
            }
            sessionStorage.setItem('matchedUser', JSON.stringify({ id: mUser.id, name: mUser.name }));
            
            // 매칭 성공 토스트는 SocketContext에서 이미 처리됨 - 중복 방지
            console.log('✅ 매칭 상태 업데이트 완료 (토스트는 SocketContext에서 처리)');
        };

        const handleMatchingFailed = () => {
            console.log('❌ 매칭 실패 처리');
            setMatchingStatus('IDLE');
        };

        // 이벤트 리스너 등록
        window.addEventListener('matchingSuccess', handleMatchingSuccess as EventListener);
        window.addEventListener('matchingFailed', handleMatchingFailed as EventListener);

        return () => {
            window.removeEventListener('matchingSuccess', handleMatchingSuccess as EventListener);
            window.removeEventListener('matchingFailed', handleMatchingFailed as EventListener);
        };
    }, []);

    const checkMatchingStatus = async () => {
        // 🛡️ 안전 장치: 이미 매칭 중이거나 처리 중이면 중복 호출 방지
        if (isMatching || matchingStatus === 'WAITING') {
            console.log(`🔒 매칭 상태 확인 건너뜀 - 현재 상태: ${matchingStatus}, 처리 중: ${isMatching}`);
            return;
        }

        try {
            console.log(`📋 안전한 매칭 상태 확인 시작 - User ${userId}`);
            
            // 세션 스토리지에서 기존 매칭 정보 먼저 확인
            const cachedRoomId = sessionStorage.getItem('lastRoomId');
            const cachedMatchedUser = sessionStorage.getItem('matchedUser');
            
            if (cachedRoomId && cachedMatchedUser && cachedRoomId !== 'null') {
                try {
                    const parsedUser = JSON.parse(cachedMatchedUser);
                    console.log('💾 캐시된 매칭 정보 복원:', { roomId: cachedRoomId, user: parsedUser });
                    
                    setMatchingStatus('MATCHED');
                    setMatchedUser({
                        id: parsedUser.id,
                        name: parsedUser.name,
                        chatRoomId: cachedRoomId
                    });
                    console.log('✅ 캐시 기반 매칭 상태 복원 완료 (API 호출 없음)');
                    return; // API 호출 완전 회피
                } catch (parseError) {
                    console.warn('캐시 파싱 실패, API 호출로 폴백:', parseError);
                }
            }

            // 캐시가 없을 때만 조심스럽게 API 호출
            console.log('🌐 캐시 없음, 신중한 API 호출 시작...');
            
            const { data } = await api.get(`/api/realtime-matching/status/${userId}`, {
                timeout: 3000, // 3초 타임아웃
                headers: {
                    'X-Request-Type': 'STATUS_CHECK_ONLY' // 백엔드에 의도 명시
                }
            });
            
            const status: MatchStatus = data?.status || 'IDLE';
            console.log(`📊 API 응답 - User ${userId}: ${status}`, data);

            // 상태별 처리
            setMatchingStatus(status);
            if (status === 'MATCHED') {
                const roomId = data?.roomId;
                const mUser = {
                    id: data?.matchedWith ?? 2,
                    name: '음악친구',
                    chatRoomId: roomId,
                };
                setMatchedUser(mUser);
                
                console.log(`✅ 서버 기존 매칭 발견 - User ${userId} <-> User ${data?.matchedWith}, Room: ${roomId}`);

                // 세션에 저장하여 다음번엔 API 호출 회피
                if (roomId) {
                    sessionStorage.setItem('lastRoomId', String(roomId));
                }
                sessionStorage.setItem('matchedUser', JSON.stringify({ id: mUser.id, name: mUser.name }));
            } else if (status === 'WAITING') {
                setQueuePosition(data?.queuePosition || 0);
                console.log(`⏳ 서버 대기 상태 발견 - User ${userId}, Queue Position: ${data?.queuePosition}`);
            } else {
                console.log(`💤 서버 유휴 상태 확인 - User ${userId}`);
            }
        } catch (error) {
            console.error('매칭 상태 확인 오류:', error);
            // 에러 시 안전한 기본 상태
            setMatchingStatus('IDLE');
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

    // 매칭 시스템 현황 업데이트 (실시간 데이터)
    const updateSystemStats = async () => {
        try {
            const response = await api.get('/api/realtime-matching/system-status');
            if (response.data) {
                const stats = response.data;
                const totalWaiting = stats.matchingSystem?.statistics?.totalWaiting ?? 0;
                const totalMatched = stats.matchingSystem?.statistics?.totalMatched ?? 0;
                
                // 온라인 사용자 수 = 매칭 대기 중인 사용자 + 매칭 완료된 사용자들 (매칭 완료 1쌍 = 2명)
                const onlineUsers = totalWaiting + (totalMatched * 2);
                
                setSystemStats({
                    totalWaiting: totalWaiting,
                    totalMatched: totalMatched,
                    onlineUsers: onlineUsers
                });
                setLastUpdateTime(new Date().toLocaleTimeString());
                setStatsUpdateError(false);
                console.log('🔄 매칭 시스템 현황 업데이트:', new Date().toLocaleTimeString());
                console.log(`📊 실제 온라인 계산: 대기(${totalWaiting}) + 매칭완료(${totalMatched} × 2) = ${onlineUsers}명`);
            }
        } catch (error) {
            console.warn('매칭 시스템 현황 업데이트 실패:', error);
            setStatsUpdateError(true);
        }
    };

    // 매칭 시스템 현황 자동 갱신 (30초마다) + 카운트다운
    useEffect(() => {
        // 초기 데이터 로드
        updateSystemStats();
        setNextUpdateCountdown(30);
        setLastUpdateTime(new Date().toLocaleTimeString());
        
        // 30초마다 시스템 현황 업데이트
        const updateInterval = setInterval(() => {
            updateSystemStats();
            setNextUpdateCountdown(30); // 카운트다운 리셋
        }, 30000);
        
        // 1초마다 카운트다운 감소
        const countdownInterval = setInterval(() => {
            setNextUpdateCountdown(prev => {
                if (prev <= 1) {
                    return 30; // 리셋
                }
                return prev - 1;
            });
        }, 1000);
        
        return () => {
            clearInterval(updateInterval);
            clearInterval(countdownInterval);
        };
    }, []);

    // 매칭된 사용자가 변경될 때 배지 및 사용자 정보 로드
    useEffect(() => {
        if (matchedUser?.id && matchedUser.id !== userId) {
            fetchMatchedUserInfo(matchedUser.id);
        }
    }, [matchedUser?.id, userId]);

    // 매칭된 사용자의 실제 정보 조회 (이름 + 배지)
    const fetchMatchedUserInfo = async (matchedUserId: number) => {
        try {
            // 1. 사용자 기본 정보 조회
            const userResponse = await api.get(`/api/users/${matchedUserId}`);
            const userData = userResponse.data;
            
            // 2. 대표 배지 조회
            const badgeResponse = await api.get(`/api/badges/user/${matchedUserId}/representative`);
            const badgeData = badgeResponse.data;
            
            const badge = badgeData ? {
                id: badgeData.id,
                badgeType: badgeData.badgeType,
                badgeName: badgeData.badgeName,
                description: badgeData.description,
                iconUrl: badgeData.iconUrl,
                rarity: badgeData.rarity || 'COMMON',
                badgeColor: badgeData.badgeColor || '#6B7280'
            } : null;
            
            setMatchedUserBadge(badge);
            
            // 3. 매칭된 사용자 정보 업데이트 (실제 이름으로)
            const updatedMatchedUser = {
                ...matchedUser,
                name: userData.name || '음악친구',
                realName: userData.name // 실제 이름 보존
            };
            setMatchedUser(updatedMatchedUser);
            
            // 4. 세션 스토리지에 실제 정보 저장 (채팅에서 사용)
            const userInfoForChat = {
                id: matchedUserId,
                name: userData.name || '음악친구',
                badge: badge
            };
            sessionStorage.setItem('matchedUser', JSON.stringify(userInfoForChat));
            
        } catch (error) {
            console.error(`매칭된 사용자 ${matchedUserId}의 정보 조회 실패:`, error);
            setMatchedUserBadge(null);
        }
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
                                    <div className="flex items-center justify-center gap-2 mb-1">
                                        <h3 className="font-bold text-gray-800 dark:text-white text-lg">{matchedUser.name}</h3>
                                        {matchedUserBadge && (
                                            <div className={`flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium relative ${
                                                matchedUserBadge.rarity === 'LEGENDARY' 
                                                    ? 'legendary-badge-glow legendary-matching-border shadow-md' 
                                                    : ''
                                            }`} 
                                                 style={{ 
                                                   backgroundColor: `${matchedUserBadge.badgeColor}20`, 
                                                   color: matchedUserBadge.badgeColor,
                                                   boxShadow: matchedUserBadge.rarity === 'LEGENDARY' 
                                                     ? `0 0 8px ${matchedUserBadge.badgeColor}40, 0 0 12px ${matchedUserBadge.badgeColor}25`
                                                     : 'none',
                                                   border: matchedUserBadge.rarity === 'LEGENDARY'
                                                     ? `1px solid ${matchedUserBadge.badgeColor}60`
                                                     : 'none'
                                                 }}>
                                                {matchedUserBadge.rarity === 'LEGENDARY' && (
                                                    <>
                                                        <div className="absolute -inset-0.5 rounded-full animate-ping opacity-15"
                                                             style={{ backgroundColor: matchedUserBadge.badgeColor }}></div>
                                                        <div className="absolute top-0 left-0 w-full h-full rounded-full"
                                                             style={{ 
                                                               background: `linear-gradient(45deg, transparent, ${matchedUserBadge.badgeColor}30, transparent, ${matchedUserBadge.badgeColor}20, transparent)`,
                                                               animation: 'legendary-wave 2.5s ease-in-out infinite'
                                                             }}>
                                                        </div>
                                                    </>
                                                )}
                                                {matchedUserBadge.iconUrl && (
                                                    <img src={matchedUserBadge.iconUrl} alt="" 
                                                         className={`w-4 h-4 rounded-full relative z-10 ${
                                                           matchedUserBadge.rarity === 'LEGENDARY' ? 'legendary-icon-float' : ''
                                                         }`} />
                                                )}
                                                <span className={`relative z-10 font-bold ${
                                                    matchedUserBadge.rarity === 'LEGENDARY' ? 'legendary-matching-text' : ''
                                                }`}>{matchedUserBadge.badgeName}</span>
                                                {matchedUserBadge.rarity === 'LEGENDARY' && (
                                                    <span className="legendary-sparkle text-yellow-300 text-xs">✨</span>
                                                )}
                                            </div>
                                        )}
                                    </div>
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

            {/* 매칭 시스템 현황 (실시간 데이터) */}
            <div className="glass-card p-6">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="font-bold text-gray-800 dark:text-white">매칭 시스템 현황</h3>
                    <div className="flex items-center space-x-2 text-sm">
                        {statsUpdateError ? (
                            <div className="flex items-center space-x-1 text-red-400">
                                <AlertCircle className="w-4 h-4" />
                                <span>업데이트 실패</span>
                            </div>
                        ) : (
                            <div className="flex items-center space-x-2 text-blue-200">
                                <RefreshCw className="w-4 h-4" />
                                <span>{nextUpdateCountdown}초 후 새로운 데이터로 갱신됩니다</span>
                            </div>
                        )}
                        {lastUpdateTime && (
                            <span className="text-gray-400 text-xs">
                                (마지막 업데이트: {lastUpdateTime})
                            </span>
                        )}
                    </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-center">
                    <div>
                        <p className="text-2xl font-bold text-blue-500">{systemStats.totalWaiting}</p>
                        <p className="text-gray-600 dark:text-gray-400 text-sm">대기 중</p>
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-green-500">{systemStats.totalMatched}</p>
                        <p className="text-gray-600 dark:text-gray-400 text-sm">매칭 완료</p>
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-purple-500">{systemStats.onlineUsers}</p>
                        <p className="text-gray-600 dark:text-gray-400 text-sm">온라인</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Matching;
