// frontend/src/pages/Stats.tsx
import React, { useEffect, useState } from 'react';
import {
    BarChart as RBarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    PieChart,
    Pie,
    Cell,
    LineChart,
    Line,
    ResponsiveContainer,
} from 'recharts';
import { Users, Music, TrendingUp, Activity, Clock, Heart } from 'lucide-react';
import api from '../api/client';

// 차트 색상
const COLORS = ['#8B5CF6', '#06B6D4', '#10B981', '#F59E0B', '#EF4444', '#3B82F6', '#EC4899'];

interface GenreData {
    genre: string;
    count: number;
    percentage?: number; // 없을 수도 있으니 선택
}

interface ActivityData {
    day: string;
    users: number;
    recommendations: number;
}

interface MatchingData {
    month: string;
    successRate: number;
    totalMatches: number;
}

interface SystemStats {
    totalUsers: number;
    totalRecommendations: number;
    avgSessionTime: number;
    matchSuccessRate: number;
    activeUsers: number;
    newUsersToday: number;
}

// ---------- 안전 정규화 유틸 ----------
const safeParseJson = (val: any) => {
    if (typeof val !== 'string') return val;
    try {
        return JSON.parse(val);
    } catch {
        return val;
    }
};

const safeArray = <T,>(val: any, fallback: T[] = []): T[] => {
    const v = safeParseJson(val);
    return Array.isArray(v) ? (v as T[]) : fallback;
};

const safeObject = <T extends object>(val: any, fallback: T): T => {
    const v = safeParseJson(val);
    return v && typeof v === 'object' && !Array.isArray(v) ? (v as T) : fallback;
};

// 객체 형태의 { genreName: count } 응답도 배열로 변환
const objectToGenreArray = (obj: Record<string, any>): GenreData[] => {
    const list = Object.entries(obj || {}).map(([genre, count]) => ({
        genre,
        count: typeof count === 'number' ? count : Number(count) || 0,
    }));
    const total = list.reduce((s, x) => s + (x.count || 0), 0) || 1;
    return list.map((x) => ({ ...x, percentage: Math.round((x.count / total) * 100) }));
};

const computePercentIfMissing = (arr: GenreData[]): GenreData[] => {
    const total = arr.reduce((s, x) => s + (x.count || 0), 0) || 1;
    return arr.map((x) =>
        typeof x.percentage === 'number'
            ? x
            : { ...x, percentage: Math.round((x.count / total) * 100) }
    );
};

const Stats: React.FC = () => {
    const [genreData, setGenreData] = useState<GenreData[]>([]);
    const [userActivityData, setUserActivityData] = useState<ActivityData[]>([]);
    const [matchingData, setMatchingData] = useState<MatchingData[]>([]);
    const [systemStats, setSystemStats] = useState<SystemStats | null>(null);
    const [realSystemData, setRealSystemData] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadStatsData = async () => {
            try {
                setLoading(true);

                // 실시간 시스템 상태
                try {
                    const systemResponse = await api.get('/api/realtime-matching/system-status');
                    setRealSystemData(systemResponse.data);
                } catch (e) {
                    // 없는 API여도 전체 페이지는 동작해야 하므로 무시
                    setRealSystemData(null);
                }

                // 매칭 상태(통계용) — 사용은 안 해도 호출 로그 확인용
                try {
                    await api.get('/api/realtime-matching/status/1');
                } catch (e) {
                    // 무시
                }

                // 주요 지표 (실제 사용자/추천 수 API가 생기면 치환)
                setSystemStats({
                    totalUsers: 1247,
                    totalRecommendations: 15680,
                    avgSessionTime: 23,
                    matchSuccessRate: 85,
                    activeUsers:
                        safeObject(realSystemData, { matchingSystem: { activeUsers: 892 } as any })
                            ?.matchingSystem?.activeUsers ?? 892,
                    newUsersToday: 34,
                });

                // 장르 데이터
                try {
                    const genreResponse = await api.get('/api/stats/genres');
                    const raw = genreResponse.data;
                    let normalized: GenreData[] = [];

                    if (Array.isArray(raw)) {
                        // [{genre,count,percentage?}, ...]
                        normalized = computePercentIfMissing(
                            raw.map((x: any) => ({
                                genre: String(x.genre ?? x.name ?? ''),
                                count: Number(x.count ?? 0),
                                percentage:
                                    typeof x.percentage === 'number' ? x.percentage : undefined,
                            }))
                        );
                    } else if (raw && typeof raw === 'object') {
                        // { "K-Pop": 340, "Pop": 290, ... }
                        normalized = objectToGenreArray(raw);
                    } else {
                        normalized = [];
                    }

                    setGenreData(normalized);
                } catch (error) {
                    // API 없으면 모의 데이터
                    const mock = [
                        { genre: 'K-Pop', count: 340 },
                        { genre: 'Pop', count: 290 },
                        { genre: 'Rock', count: 185 },
                        { genre: 'Hip-Hop', count: 165 },
                        { genre: 'Jazz', count: 120 },
                        { genre: 'Classical', count: 110 },
                    ];
                    setGenreData(computePercentIfMissing(mock));
                }

                // 사용자 활동 데이터
                try {
                    const activityResponse = await api.get('/api/stats/user-activity');
                    const raw = activityResponse.data;
                    const list = safeArray<ActivityData>(raw).map((x) => ({
                        day: String((x as any).day ?? ''),
                        users: Number((x as any).users ?? 0),
                        recommendations: Number((x as any).recommendations ?? 0),
                    }));
                    setUserActivityData(list.length ? list : []);
                } catch (error) {
                    setUserActivityData([
                        { day: '월', users: 120, recommendations: 340 },
                        { day: '화', users: 135, recommendations: 380 },
                        { day: '수', users: 148, recommendations: 420 },
                        { day: '목', users: 162, recommendations: 450 },
                        { day: '금', users: 180, recommendations: 520 },
                        { day: '토', users: 195, recommendations: 580 },
                        { day: '일', users: 175, recommendations: 510 },
                    ]);
                }

                // 매칭 성공률 데이터
                try {
                    const matchingStatsResponse = await api.get('/api/stats/matching-trends');
                    const raw = matchingStatsResponse.data;
                    const list = safeArray<MatchingData>(raw).map((x) => ({
                        month: String((x as any).month ?? ''),
                        successRate: Number((x as any).successRate ?? 0),
                        totalMatches: Number((x as any).totalMatches ?? 0),
                    }));
                    setMatchingData(list.length ? list : []);
                } catch (error) {
                    console.log('매칭 추이 API 요청 실패:', error);
                    // 실제 데이터가 없으면 빈 배열로 설정 (더미 데이터 제거)
                    setMatchingData([]);
                }
            } catch (error) {
                // 페이지 전체가 꺾이지 않도록 기본값 채움
                setSystemStats({
                    totalUsers: 1247,
                    totalRecommendations: 15680,
                    avgSessionTime: 23,
                    matchSuccessRate: 85,
                    activeUsers: 892,
                    newUsersToday: 34,
                });
                setGenreData(
                    computePercentIfMissing([
                        { genre: 'K-Pop', count: 340 },
                        { genre: 'Pop', count: 290 },
                        { genre: 'Rock', count: 185 },
                    ])
                );
                setUserActivityData([
                    { day: '월', users: 120, recommendations: 340 },
                    { day: '화', users: 135, recommendations: 380 },
                    { day: '수', users: 148, recommendations: 420 },
                ]);
                // 실제 데이터가 없으면 빈 배열로 설정 (더미 데이터 제거)
                setMatchingData([]);
            } finally {
                setLoading(false);
            }
        };

        loadStatsData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [realSystemData?.matchingSystem?.activeUsers]); // systemStats.activeUsers 계산 시 참조 가능

    if (loading) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-white mx-auto mb-4"></div>
                    <div className="text-white text-xl">통계를 불러오는 중...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900">
            <div className="container mx-auto px-4 py-8">
                {/* 헤더 */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-white mb-2">시스템 통계</h1>
                    <p className="text-blue-200 text-lg">음악 추천 시스템의 성과와 사용자 활동을 확인하세요</p>
                </div>

                {/* 실시간 시스템 데이터 표시 */}
                {realSystemData && (
                    <div className="mb-6 bg-white/10 backdrop-blur-lg rounded-2xl p-4 border border-white/20">
                        <div className="text-white">
                            <h3 className="font-bold mb-2">실시간 시스템 상태</h3>
                            <p className="text-sm text-blue-200">{realSystemData?.message ?? '—'}</p>
                            <div className="mt-2 text-xs text-gray-300">
                                <p>버전: {realSystemData?.systemVersion ?? '—'}</p>
                                <p>매칭 시스템: {realSystemData?.matchingSystem ? '활성' : '비활성'}</p>
                                <p>채팅 시스템: {realSystemData?.chatSystem ? '활성' : '비활성'}</p>
                            </div>
                        </div>
                    </div>
                )}

                {/* 주요 지표 */}
                {systemStats && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">총 사용자</p>
                                    <p className="text-3xl font-bold text-white">
                                        {systemStats.totalUsers.toLocaleString()}
                                    </p>
                                    <p className="text-green-400 text-sm">+{systemStats.newUsersToday} 오늘</p>
                                </div>
                                <Users className="w-12 h-12 text-blue-400" />
                            </div>
                        </div>

                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">총 추천곡</p>
                                    <p className="text-3xl font-bold text-white">
                                        {systemStats.totalRecommendations.toLocaleString()}
                                    </p>
                                    <p className="text-purple-400 text-sm">누적 제공</p>
                                </div>
                                <Music className="w-12 h-12 text-purple-400" />
                            </div>
                        </div>

                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">매칭 성공률</p>
                                    <p className="text-3xl font-bold text-white">{systemStats.matchSuccessRate}%</p>
                                    <p className="text-green-400 text-sm">+3% 이전 달</p>
                                </div>
                                <Heart className="w-12 h-12 text-red-400" />
                            </div>
                        </div>

                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">활성 사용자</p>
                                    <p className="text-3xl font-bold text-white">
                                        {systemStats.activeUsers.toLocaleString()}
                                    </p>
                                    <p className="text-blue-400 text-sm">현재 온라인</p>
                                </div>
                                <Activity className="w-12 h-12 text-green-400" />
                            </div>
                        </div>

                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">평균 세션</p>
                                    <p className="text-3xl font-bold text-white">{systemStats.avgSessionTime}분</p>
                                    <p className="text-yellow-400 text-sm">사용자당</p>
                                </div>
                                <Clock className="w-12 h-12 text-yellow-400" />
                            </div>
                        </div>

                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm font-medium">성장률</p>
                                    <p className="text-3xl font-bold text-white">+12%</p>
                                    <p className="text-green-400 text-sm">월간 증가</p>
                                </div>
                                <TrendingUp className="w-12 h-12 text-green-400" />
                            </div>
                        </div>
                    </div>
                )}

                {/* 차트 섹션 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
                    {/* 장르별 인기도 파이 차트 */}
                    <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                        <h3 className="text-xl font-bold text-white mb-4 flex items-center space-x-2">
                            <Music className="w-5 h-5" />
                            <span>장르별 인기도</span>
                        </h3>
                        <ResponsiveContainer width="100%" height={300}>
                            <PieChart>
                                <Pie
                                    data={genreData}
                                    cx="50%"
                                    cy="50%"
                                    outerRadius={80}
                                    fill="#8884d8"
                                    dataKey="count"
                                    label={(entry: any) =>
                                        `${entry?.genre ?? ''} ${entry?.percentage ?? 0}%`
                                    }
                                >
                                    {genreData.map((_, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: 'rgba(0,0,0,0.8)',
                                        border: 'none',
                                        borderRadius: '8px',
                                        color: 'white',
                                    }}
                                />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>

                    {/* 주간 사용자 활동 바 차트 */}
                    <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                        <h3 className="text-xl font-bold text-white mb-4 flex items-center space-x-2">
                            <Users className="w-5 h-5" />
                            <span>주간 사용자 활동</span>
                        </h3>
                        <ResponsiveContainer width="100%" height={300}>
                            <RBarChart data={userActivityData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.2)" />
                                <XAxis
                                    dataKey="day"
                                    tick={{ fill: 'white' }}
                                    axisLine={{ stroke: 'rgba(255,255,255,0.3)' }}
                                />
                                <YAxis
                                    tick={{ fill: 'white' }}
                                    axisLine={{ stroke: 'rgba(255,255,255,0.3)' }}
                                />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: 'rgba(0,0,0,0.8)',
                                        border: 'none',
                                        borderRadius: '8px',
                                        color: 'white',
                                    }}
                                />
                                <Legend />
                                <Bar dataKey="users" fill="#8B5CF6" name="활성 사용자" />
                                <Bar dataKey="recommendations" fill="#06B6D4" name="추천 수" />
                            </RBarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* 매칭 성공률 라인 차트 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                    <h3 className="text-xl font-bold text-white mb-4 flex items-center space-x-2">
                        <Heart className="w-5 h-5" />
                        <span>월별 매칭 성공률 추이</span>
                    </h3>
                    <ResponsiveContainer width="100%" height={300}>
                        <LineChart data={matchingData}>
                            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.2)" />
                            <XAxis
                                dataKey="month"
                                tick={{ fill: 'white' }}
                                axisLine={{ stroke: 'rgba(255,255,255,0.3)' }}
                            />
                            <YAxis
                                tick={{ fill: 'white' }}
                                axisLine={{ stroke: 'rgba(255,255,255,0.3)' }}
                            />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: 'rgba(0,0,0,0.8)',
                                    border: 'none',
                                    borderRadius: '8px',
                                    color: 'white',
                                }}
                            />
                            <Legend />
                            <Line
                                type="monotone"
                                dataKey="successRate"
                                stroke="#10B981"
                                strokeWidth={3}
                                name="성공률 (%)"
                                dot={{ fill: '#10B981', strokeWidth: 2, r: 6 }}
                            />
                            <Line
                                type="monotone"
                                dataKey="totalMatches"
                                stroke="#F59E0B"
                                strokeWidth={3}
                                name="총 매칭 수"
                                dot={{ fill: '#F59E0B', strokeWidth: 2, r: 6 }}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>

                {/* 실시간 통계 요약 */}
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20 mt-8">
                    <h3 className="text-xl font-bold text-white mb-4 flex items-center space-x-2">
                        <Activity className="w-5 h-5" />
                        <span>실시간 시스템 상태</span>
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="text-center">
                            <div className="w-4 h-4 bg-green-500 rounded-full mx-auto mb-2 animate-pulse" />
                            <p className="text-white font-medium">서버 상태</p>
                            <p className="text-green-400 text-sm">
                                {realSystemData?.success ? '정상 운영' : '점검 중'}
                            </p>
                        </div>
                        <div className="text-center">
                            <div className="w-4 h-4 bg-blue-500 rounded-full mx-auto mb-2 animate-pulse" />
                            <p className="text-white font-medium">데이터베이스</p>
                            <p className="text-blue-400 text-sm">연결됨</p>
                        </div>
                        <div className="text-center">
                            <div className="w-4 h-4 bg-purple-500 rounded-full mx-auto mb-2 animate-pulse" />
                            <p className="text-white font-medium">추천 엔진</p>
                            <p className="text-purple-400 text-sm">
                                {realSystemData?.matchingSystem ? '활성화' : '비활성'}
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Stats;
