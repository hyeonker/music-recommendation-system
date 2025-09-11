import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, CheckCircle, XCircle, Clock, MoreHorizontal, Users, Ban, UserCheck, Trash2, Award, RotateCcw, Bell, Send, MessageSquare, AlertCircle, Settings, Calendar, Gift, BarChart, Music, TrendingUp, Activity, Heart } from 'lucide-react';
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

interface ReviewReport {
  id: number;
  reviewId: number;
  reporterUserId: number;
  reason: string;
  description?: string;
  status: 'PENDING' | 'REVIEWING' | 'RESOLVED' | 'DISMISSED';
  createdAt: string;
  resolvedAt?: string;
  resolvedByUserId?: number;
  review?: {
    id: number;
    reviewText: string;
    rating: number;
    user: {
      id: number;
      name: string;
      nickname?: string;
    };
    musicItem: {
      id: number;
      name: string;
      artistName: string;
      albumName?: string;
    };
  };
}

interface ReportStats {
  totalReports: number;
  pendingReports: number;
  resolvedReports: number;
  dismissedReports: number;
}

interface User {
  id: number;
  name: string;
  email: string;
  provider: string;
  providerId: string;
  profileImageUrl?: string;
  createdAt: string;
  updatedAt: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
}

const AdminPanel: React.FC = () => {
  const [reports, setReports] = useState<ReviewReport[]>([]);
  const [stats, setStats] = useState<ReportStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isAdmin, setIsAdmin] = useState(false);
  const [activeTab, setActiveTab] = useState<'reports' | 'users' | 'badges' | 'notifications' | 'chatreports' | 'statistics'>('reports');
  const [users, setUsers] = useState<User[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [batchBadgeModal, setBatchBadgeModal] = useState(false);
  
  // 배지 관리 상태
  const [expiringBadges, setExpiringBadges] = useState<any[]>([]);
  const [badgesLoading, setBadgesLoading] = useState(false);
  const [expireResult, setExpireResult] = useState<any>(null);
  
  // 시스템 알림 관리 상태
  const [systemNotifications, setSystemNotifications] = useState<any[]>([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [createNotificationModal, setCreateNotificationModal] = useState(false);

  // 통계 관리 상태
  const [genreData, setGenreData] = useState<any[]>([]);
  const [userActivityData, setUserActivityData] = useState<any[]>([]);
  const [matchingData, setMatchingData] = useState<any[]>([]);
  const [systemStats, setSystemStats] = useState<any>(null);
  const [realSystemData, setRealSystemData] = useState<any>(null);
  const [statsLoading, setStatsLoading] = useState(false);

  useEffect(() => {
    checkAdminAuth();
  }, []);

  useEffect(() => {
    if (isAdmin) {
      loadReports();
      loadStats();
      if (activeTab === 'users') {
        loadUsers();
      }
      if (activeTab === 'badges') {
        loadUsers(); // 배지 관리에서도 사용자 목록 필요
        loadExpiringBadges();
      }
      if (activeTab === 'notifications') {
        loadUsers(); // 알림 대상 선택용
        loadSystemNotifications();
      }
      if (activeTab === 'statistics') {
        loadStatisticsData();
      }
    }
  }, [isAdmin, selectedStatus, currentPage, activeTab]);

  const checkAdminAuth = async () => {
    try {
      const response = await fetch('/api/chat-reports/admin/statistics', {
        credentials: 'include'
      });
      
      if (response.status === 403) {
        setIsAdmin(false);
        return;
      }
      
      if (response.ok) {
        setIsAdmin(true);
      }
    } catch (error) {
      console.error('관리자 권한 확인 실패:', error);
      setIsAdmin(false);
    }
  };

  const loadReports = async () => {
    if (!isAdmin) return;
    
    try {
      setLoading(true);
      const statusQuery = selectedStatus ? `&status=${selectedStatus}` : '';
      const response = await fetch(`/api/admin/reports?page=${currentPage}&size=10&sortBy=createdAt&sortDirection=desc${statusQuery}`, {
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setReports(data.content || []);
        setTotalPages(data.totalPages || 0);
      }
    } catch (error) {
      console.error('신고 내역 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    if (!isAdmin) return;
    
    try {
      const response = await fetch('/api/admin/reports/stats', {
        credentials: 'include'
      });
      
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (error) {
      console.error('통계 로드 실패:', error);
    }
  };

  const loadUsers = async () => {
    if (!isAdmin) return;
    
    try {
      setUsersLoading(true);
      const response = await fetch('/api/users/all', {
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setUsers(data);
      } else {
        console.error('사용자 목록 로드 실패:', response.status);
      }
    } catch (error) {
      console.error('사용자 목록 로드 실패:', error);
    } finally {
      setUsersLoading(false);
    }
  };

  const loadExpiringBadges = async () => {
    if (!isAdmin) return;
    
    try {
      setBadgesLoading(true);
      const response = await fetch('/api/badges/admin/expiring-badges', {
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setExpiringBadges(data.expiringBadges || []);
      } else {
        console.error('만료 예정 배지 로드 실패:', response.status);
      }
    } catch (error) {
      console.error('만료 예정 배지 로드 실패:', error);
    } finally {
      setBadgesLoading(false);
    }
  };

  const manualExpireBadges = async () => {
    try {
      setBadgesLoading(true);
      const response = await fetch('/api/badges/admin/expire-badges', {
        method: 'POST',
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setExpireResult(data);
        await loadExpiringBadges();
        alert(`수동 배지 만료 완료: ${data.removedCount}개 배지가 만료되었습니다.`);
      } else {
        const errorText = await response.text();
        alert(`수동 배지 만료 실패: ${errorText}`);
      }
    } catch (error) {
      console.error('수동 배지 만료 실패:', error);
      alert('수동 배지 만료 중 오류가 발생했습니다.');
    } finally {
      setBadgesLoading(false);
    }
  };

  const loadSystemNotifications = async () => {
    if (!isAdmin) return;
    
    try {
      setNotificationsLoading(true);
      const response = await fetch('/api/notifications/system/list', {
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setSystemNotifications(data || []);
      } else {
        console.error('시스템 알림 목록 로드 실패:', response.status);
      }
    } catch (error) {
      console.error('시스템 알림 목록 로드 실패:', error);
    } finally {
      setNotificationsLoading(false);
    }
  };

  const updateReportStatus = async (reportId: number, newStatus: string) => {
    try {
      const response = await fetch(`/api/admin/reports/${reportId}/status`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ status: newStatus })
      });

      if (response.ok) {
        loadReports();
        loadStats();
        alert('상태가 업데이트되었습니다.');
      } else {
        const errorData = await response.json();
        alert(errorData.message || '상태 업데이트에 실패했습니다.');
      }
    } catch (error) {
      console.error('상태 업데이트 실패:', error);
      alert('상태 업데이트 중 오류가 발생했습니다.');
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Clock className="w-4 h-4 text-yellow-500" />;
      case 'REVIEWING':
        return <AlertTriangle className="w-4 h-4 text-blue-500" />;
      case 'RESOLVED':
        return <CheckCircle className="w-4 h-4 text-green-500" />;
      case 'DISMISSED':
        return <XCircle className="w-4 h-4 text-gray-500" />;
      default:
        return <Clock className="w-4 h-4 text-yellow-500" />;
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'PENDING':
        return '대기중';
      case 'REVIEWING':
        return '검토중';
      case 'RESOLVED':
        return '해결됨';
      case 'DISMISSED':
        return '기각됨';
      default:
        return status;
    }
  };

  if (!isAdmin) {
    return (
      <div className="min-h-[60vh] grid place-items-center bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900">
        <div className="text-center text-white">
          <AlertTriangle className="w-16 h-16 mx-auto mb-4 text-red-400" />
          <h2 className="text-2xl font-bold mb-2">접근 권한 없음</h2>
          <p>관리자 권한이 필요합니다.</p>
        </div>
      </div>
    );
  }

  // 통계 데이터 로딩 함수들
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

  const objectToGenreArray = (obj: Record<string, any>) => {
    const list = Object.entries(obj || {}).map(([genre, count]) => ({
      genre,
      count: typeof count === 'number' ? count : Number(count) || 0,
    }));
    const total = list.reduce((s, x) => s + (x.count || 0), 0) || 1;
    return list.map((x) => ({ ...x, percentage: Math.round((x.count / total) * 100) }));
  };

  const computePercentIfMissing = (arr: any[]) => {
    const total = arr.reduce((s, x) => s + (x.count || 0), 0) || 1;
    return arr.map((x) =>
      typeof x.percentage === 'number'
        ? x
        : { ...x, percentage: Math.round((x.count / total) * 100) }
    );
  };

  const loadStatisticsData = async () => {
    try {
      setStatsLoading(true);

      // 실시간 시스템 상태
      try {
        const systemResponse = await fetch('/api/realtime-matching/system-status', { credentials: 'include' });
        if (systemResponse.ok) {
          setRealSystemData(await systemResponse.json());
        }
      } catch (e) {
        setRealSystemData(null);
      }

      // 실제 시스템 통계 API 호출
      try {
        const statsResponse = await fetch('/api/stats/overall', { credentials: 'include' });
        if (statsResponse.ok) {
          const statsData = await statsResponse.json();
          if (statsData.success && statsData.overall) {
            setSystemStats({
              totalUsers: statsData.overall.totalUsers,
              totalRecommendations: statsData.overall.totalRecommendations,
              avgSessionTime: statsData.overall.avgSessionTime,
              matchSuccessRate: statsData.overall.matchSuccessRate,
              activeUsers: statsData.overall.activeUsers,
              newUsersToday: statsData.overall.newUsersToday,
            });
          } else {
            throw new Error('Invalid stats response');
          }
        } else {
          throw new Error('Stats API failed');
        }
      } catch (error) {
        console.warn('실제 통계 API 실패, 기본값 사용:', error);
        // 실제 데이터가 없으면 null로 설정 (더미 데이터 제거)
        setSystemStats(null);
      }

      // 장르 데이터 - 실제 API 호출
      try {
        const genreResponse = await fetch('/api/stats/genres', { credentials: 'include' });
        if (genreResponse.ok) {
          const raw = await genreResponse.json();
          if (raw.success && raw.genres && Array.isArray(raw.genres)) {
            const normalized = raw.genres.map((x: any) => ({
              genre: String(x.genre ?? ''),
              count: Number(x.count ?? 0),
              percentage: Number(x.percentage ?? 0),
            }));
            setGenreData(normalized);
          } else {
            throw new Error('Invalid genre response');
          }
        } else {
          throw new Error('Genre API failed');
        }
      } catch (error) {
        console.warn('실제 장르 API 실패, 기본값 사용:', error);
        const mock = [
          { genre: 'K-Pop', count: 340, percentage: 28 },
          { genre: 'Pop', count: 290, percentage: 24 },
          { genre: 'Rock', count: 185, percentage: 15 },
          { genre: 'Hip-Hop', count: 165, percentage: 14 },
          { genre: 'Jazz', count: 120, percentage: 10 },
          { genre: 'Classical', count: 110, percentage: 9 },
        ];
        setGenreData(mock);
      }

      // 사용자 활동 데이터 - 실제 API 호출
      try {
        const activityResponse = await fetch('/api/stats/user-activity', { credentials: 'include' });
        if (activityResponse.ok) {
          const raw = await activityResponse.json();
          if (raw.success && raw.activity && Array.isArray(raw.activity)) {
            const list = raw.activity.map((x: any) => ({
              day: String(x.day ?? ''),
              users: Number(x.users ?? 0),
              recommendations: Number(x.recommendations ?? 0),
            }));
            setUserActivityData(list);
          } else {
            throw new Error('Invalid activity response');
          }
        } else {
          throw new Error('Activity API failed');
        }
      } catch (error) {
        console.warn('실제 활동 API 실패, 기본값 사용:', error);
        // 실제 데이터가 없으면 빈 배열로 설정 (더미 데이터 제거)
        setUserActivityData([]);
      }

      // 매칭 성공률 데이터 - 실제 API 호출
      try {
        const matchingStatsResponse = await fetch('/api/stats/matching-trends', { credentials: 'include' });
        if (matchingStatsResponse.ok) {
          const raw = await matchingStatsResponse.json();
          if (raw.success && raw.trends && Array.isArray(raw.trends)) {
            const list = raw.trends.map((x: any) => ({
              month: String(x.month ?? ''),
              successRate: Number(x.successRate ?? 0),
              totalMatches: Number(x.totalMatches ?? 0),
            }));
            setMatchingData(list);
          } else {
            throw new Error('Invalid matching response');
          }
        } else {
          throw new Error('Matching API failed');
        }
      } catch (error) {
        console.warn('실제 매칭 API 실패, 기본값 사용:', error);
        // 실제 데이터가 없으면 빈 배열로 설정 (더미 데이터 제거)
        setMatchingData([]);
      }
    } catch (error) {
      console.error('통계 데이터 로딩 실패:', error);
    } finally {
      setStatsLoading(false);
    }
  };

  return (
    <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 min-h-screen p-6">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">관리자 패널</h1>
          <p className="text-blue-200">시스템 관리</p>
        </div>

        {/* 탭 네비게이션 */}
        <div className="flex justify-center mb-8">
          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-2 border border-white/20">
            <div className="flex space-x-2">
              <button
                onClick={() => setActiveTab('reports')}
                className={`px-6 py-3 rounded-xl font-medium transition-all ${
                  activeTab === 'reports'
                    ? 'bg-white/20 text-white shadow-lg'
                    : 'text-blue-200 hover:text-white hover:bg-white/10'
                }`}
              >
                신고 관리
              </button>
              <button
                onClick={() => setActiveTab('users')}
                className={`px-6 py-3 rounded-xl font-medium transition-all flex items-center gap-2 ${
                  activeTab === 'users'
                    ? 'bg-white/20 text-white shadow-lg'
                    : 'text-blue-200 hover:text-white hover:bg-white/10'
                }`}
              >
                <Users className="w-4 h-4" />
                사용자 관리
              </button>
              <button
                onClick={() => setActiveTab('badges')}
                className={`px-6 py-3 rounded-xl font-medium transition-all flex items-center gap-2 ${
                  activeTab === 'badges'
                    ? 'bg-white/20 text-white shadow-lg'
                    : 'text-blue-200 hover:text-white hover:bg-white/10'
                }`}
              >
                <Award className="w-4 h-4" />
                배지 관리
              </button>
              <button
                onClick={() => setActiveTab('notifications')}
                className={`px-6 py-3 rounded-xl font-medium transition-all flex items-center gap-2 ${
                  activeTab === 'notifications'
                    ? 'bg-white/20 text-white shadow-lg'
                    : 'text-blue-200 hover:text-white hover:bg-white/10'
                }`}
              >
                <Bell className="w-4 h-4" />
                시스템 알림
              </button>
              <button
                onClick={() => setActiveTab('chatreports')}
                className={`px-6 py-3 rounded-xl font-medium transition-all flex items-center gap-2 ${
                  activeTab === 'chatreports'
                    ? 'bg-white/20 text-white shadow-lg'
                    : 'text-blue-200 hover:text-white hover:bg-white/10'
                }`}
              >
                <MessageSquare className="w-4 h-4" />
                채팅 신고
              </button>
              <button
                onClick={() => setActiveTab('statistics')}
                className={`px-6 py-3 rounded-xl font-medium transition-all flex items-center gap-2 ${
                  activeTab === 'statistics'
                    ? 'bg-white/20 text-white shadow-lg'
                    : 'text-blue-200 hover:text-white hover:bg-white/10'
                }`}
              >
                <BarChart className="w-4 h-4" />
                시스템 통계
              </button>
            </div>
          </div>
        </div>

        {/* 신고 관리 탭 */}
        {activeTab === 'reports' && (
          <>
            {/* 통계 카드 */}
            {stats && (
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                  <div className="text-center">
                    <div className="text-3xl font-bold text-white">{stats.totalReports}</div>
                    <div className="text-blue-200 text-sm">전체 신고</div>
                  </div>
                </div>
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                  <div className="text-center">
                    <div className="text-3xl font-bold text-yellow-400">{stats.pendingReports}</div>
                    <div className="text-blue-200 text-sm">대기중</div>
                  </div>
                </div>
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                  <div className="text-center">
                    <div className="text-3xl font-bold text-green-400">{stats.resolvedReports}</div>
                    <div className="text-blue-200 text-sm">해결됨</div>
                  </div>
                </div>
                <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                  <div className="text-center">
                    <div className="text-3xl font-bold text-gray-400">{stats.dismissedReports}</div>
                    <div className="text-blue-200 text-sm">기각됨</div>
                  </div>
                </div>
              </div>
            )}
          </>
        )}
        
        {/* 사용자 관리 탭 */}
        {activeTab === 'users' && (
          <>
            {/* 사용자 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-white">{users.length}</div>
                  <div className="text-blue-200 text-sm">전체 사용자</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-green-400">{users.filter(u => u.provider === 'GOOGLE').length}</div>
                  <div className="text-blue-200 text-sm">구글 로그인</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-yellow-400">{users.filter(u => u.provider === 'KAKAO').length}</div>
                  <div className="text-blue-200 text-sm">카카오 로그인</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-blue-400">{users.filter(u => u.provider === 'NAVER').length}</div>
                  <div className="text-blue-200 text-sm">네이버 로그인</div>
                </div>
              </div>
            </div>
          </>
        )}

        {/* 신고 필터 */}
        {activeTab === 'reports' && (
          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6 border border-white/20">
            <div className="flex flex-wrap gap-4 items-center">
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">모든 상태</option>
                <option value="PENDING">대기중</option>
                <option value="REVIEWING">검토중</option>
                <option value="RESOLVED">해결됨</option>
                <option value="DISMISSED">기각됨</option>
              </select>
            </div>
          </div>
        )}

        {/* 신고 목록 */}
        {activeTab === 'reports' && (
          <>
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl border border-white/20 overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-white/20">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">신고 ID</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">리뷰 정보</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">신고 사유</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">상태</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">신고일</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">액션</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/10">
                    {loading ? (
                      <tr>
                        <td colSpan={6} className="px-6 py-4 text-center text-white">로딩 중...</td>
                      </tr>
                    ) : reports.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-6 py-4 text-center text-white">신고 내역이 없습니다.</td>
                      </tr>
                    ) : (
                      reports.map((report) => (
                        <tr key={report.id} className="hover:bg-white/5">
                          <td className="px-6 py-4 text-white">{report.id}</td>
                          <td className="px-6 py-4 text-white">
                            <div>
                              <div className="font-medium text-sm">
                                {report.review?.user?.name || '알 수 없는 사용자'}
                              </div>
                              <div className="text-blue-200 text-xs mt-1 line-clamp-2">
                                {report.review?.reviewText || '리뷰 내용 없음'}
                              </div>
                              <div className="text-purple-300 text-xs mt-1">
                                {report.review?.musicItem?.name && report.review?.musicItem?.artistName 
                                  ? `${report.review.musicItem.name} - ${report.review.musicItem.artistName}` 
                                  : '곡 정보 없음'}
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-white">
                            <div>
                              <div className="font-medium">{report.reason}</div>
                              {report.description && (
                                <div className="text-sm text-blue-200 mt-1">{report.description}</div>
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-2">
                              {getStatusIcon(report.status)}
                              <span className="text-white">{getStatusText(report.status)}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-white">
                            {new Date(report.createdAt).toLocaleDateString()}
                          </td>
                          <td className="px-6 py-4">
                            <ReportActions
                              report={report}
                              onStatusUpdate={updateReportStatus}
                            />
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* 페이지네이션 */}
            {totalPages > 1 && (
              <div className="flex justify-center mt-6 space-x-2">
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => setCurrentPage(i)}
                    className={`px-4 py-2 rounded-lg ${
                      currentPage === i
                        ? 'bg-blue-600 text-white'
                        : 'bg-white/20 text-white hover:bg-white/30'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}
              </div>
            )}
          </>
        )}

        {/* 사용자 목록 */}
        {activeTab === 'users' && (
          <div className="bg-white/10 backdrop-blur-lg rounded-2xl border border-white/20 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-white/20">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">ID</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">이름</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">이메일</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">제공자</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">가입일</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">상태</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">액션</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  {usersLoading ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-4 text-center text-white">로딩 중...</td>
                    </tr>
                  ) : users.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-4 text-center text-white">사용자가 없습니다.</td>
                    </tr>
                  ) : (
                    users.map((user) => (
                      <tr key={user.id} className="hover:bg-white/5">
                        <td className="px-6 py-4 text-white font-medium">{user.id}</td>
                        <td className="px-6 py-4 text-white">
                          <div className="flex items-center gap-3">
                            {user.profileImageUrl && (
                              <img 
                                src={user.profileImageUrl} 
                                alt={user.name}
                                className="w-8 h-8 rounded-full"
                              />
                            )}
                            <span>{user.name}</span>
                            {user.id === 1 && (
                              <span className="px-2 py-1 bg-red-500/20 text-red-200 text-xs rounded-full">
                                관리자
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-white text-sm">{user.email}</td>
                        <td className="px-6 py-4">
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                            user.provider === 'GOOGLE' ? 'bg-red-500/20 text-red-200' :
                            user.provider === 'KAKAO' ? 'bg-yellow-500/20 text-yellow-200' :
                            user.provider === 'NAVER' ? 'bg-green-500/20 text-green-200' :
                            'bg-gray-500/20 text-gray-200'
                          }`}>
                            {user.provider}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-white text-sm">
                          {new Date(user.createdAt).toLocaleDateString()}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`px-3 py-1 text-xs rounded-full font-medium ${
                            user.status === 'ACTIVE' ? 'bg-green-500/20 text-green-200' :
                            user.status === 'SUSPENDED' ? 'bg-red-500/20 text-red-200' :
                            user.status === 'DELETED' ? 'bg-gray-500/20 text-gray-200' :
                            'bg-blue-500/20 text-blue-200'
                          }`}>
                            {user.status === 'ACTIVE' ? '활성' :
                             user.status === 'SUSPENDED' ? '정지' :
                             user.status === 'DELETED' ? '탈퇴' : user.status}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <UserActions
                            user={user}
                            onUserUpdate={loadUsers}
                          />
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 배지 관리 탭 */}
        {activeTab === 'badges' && (
          <>
            {/* 배지 관리 액션 버튼들 */}
            <div className="mb-6 flex flex-wrap gap-4 justify-end">
              <button
                onClick={() => setBatchBadgeModal(true)}
                className="px-6 py-3 bg-yellow-500/20 hover:bg-yellow-500/30 border border-yellow-500/30 rounded-lg transition-colors flex items-center gap-2"
              >
                <Award className="w-5 h-5 text-yellow-400" />
                <span className="text-white font-semibold">배치 배지 부여</span>
              </button>
              <button
                onClick={() => {
                  loadExpiringBadges();
                  manualExpireBadges();
                }}
                disabled={badgesLoading}
                className="px-6 py-3 bg-red-500/20 hover:bg-red-500/30 disabled:bg-gray-500/20 border border-red-500/30 disabled:border-gray-500/30 rounded-lg transition-colors flex items-center gap-2"
              >
                {badgesLoading ? (
                  <Clock className="w-5 h-5 animate-spin text-gray-400" />
                ) : (
                  <>
                    <Award className="w-5 h-5 text-red-400" />
                    <span className="text-white font-semibold">수동 만료 실행</span>
                  </>
                )}
              </button>
            </div>

            {/* 배지 관리 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-white">{users.length}</div>
                  <div className="text-blue-200 text-sm">전체 사용자</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-yellow-400">{expiringBadges.length}</div>
                  <div className="text-blue-200 text-sm">만료 예정 배지</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-white">{expireResult ? expireResult.removedCount : 0}</div>
                  <div className="text-blue-200 text-sm">마지막 만료된 배지</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <div className="text-3xl font-bold text-green-400">
                    {users.reduce((total, user) => total + (user.status === 'ACTIVE' ? 1 : 0), 0)}
                  </div>
                  <div className="text-blue-200 text-sm">활성 사용자</div>
                </div>
              </div>
            </div>

            {/* 사용자 배지 관리 섹션 */}
            <div className="mb-8 bg-white/10 backdrop-blur-lg rounded-2xl border border-white/20 overflow-hidden">
              <div className="px-6 py-4 border-b border-white/20">
                <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                  <Users className="w-5 h-5 text-blue-400" />
                  사용자별 배지 관리
                </h3>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-white/20">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">사용자</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">이메일</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">보유 배지 수</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">상태</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">배지 관리</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/10">
                    {usersLoading ? (
                      <tr>
                        <td colSpan={5} className="px-6 py-4 text-center text-white">로딩 중...</td>
                      </tr>
                    ) : users.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-6 py-4 text-center text-white">사용자가 없습니다.</td>
                      </tr>
                    ) : (
                      users.map((user) => (
                        <UserBadgeRow key={user.id} user={user} onUpdate={loadUsers} />
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* 만료 예정 배지 목록 */}
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl border border-white/20 overflow-hidden">
              <div className="px-6 py-4 border-b border-white/20">
                <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                  <Award className="w-5 h-5 text-yellow-400" />
                  만료 예정 배지 목록
                  <button
                    onClick={loadExpiringBadges}
                    disabled={badgesLoading}
                    className="ml-auto p-2 rounded-lg bg-white/20 hover:bg-white/30 disabled:bg-gray-500/20 transition-colors"
                  >
                    {badgesLoading ? (
                      <Clock className="w-4 h-4 animate-spin text-gray-400" />
                    ) : (
                      <RotateCcw className="w-4 h-4 text-white" />
                    )}
                  </button>
                </h3>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-white/20">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">사용자 ID</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">사용자명</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">배지명</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">배지 타입</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">등급</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">만료일</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/10">
                    {badgesLoading ? (
                      <tr>
                        <td colSpan={6} className="px-6 py-4 text-center text-white">로딩 중...</td>
                      </tr>
                    ) : expiringBadges.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-6 py-4 text-center text-gray-400">만료 예정 배지가 없습니다.</td>
                      </tr>
                    ) : (
                      expiringBadges.map((badge, index) => (
                        <tr key={index} className="hover:bg-white/5 transition-colors">
                          <td className="px-6 py-4 text-white">{badge.userId}</td>
                          <td className="px-6 py-4 text-white">{badge.userName || 'Unknown'}</td>
                          <td className="px-6 py-4">
                            <span className="text-white font-medium">{badge.badgeName}</span>
                          </td>
                          <td className="px-6 py-4 text-gray-300">{badge.badgeType}</td>
                          <td className="px-6 py-4">
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                              badge.rarity === 'LEGENDARY' ? 'bg-yellow-500/20 text-yellow-200' :
                              badge.rarity === 'EPIC' ? 'bg-purple-500/20 text-purple-200' :
                              badge.rarity === 'RARE' ? 'bg-blue-500/20 text-blue-200' :
                              'bg-gray-500/20 text-gray-200'
                            }`}>
                              {badge.rarity === 'LEGENDARY' ? '전설' :
                               badge.rarity === 'EPIC' ? '영웅' :
                               badge.rarity === 'RARE' ? '희귀' : '일반'}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-gray-300">
                            {badge.expiresAt ? new Date(badge.expiresAt).toLocaleString('ko-KR') : '만료 없음'}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}

        {/* 채팅 신고 관리 탭 */}
        {activeTab === 'chatreports' && (
          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                <MessageSquare className="w-5 h-5 text-blue-400" />
                채팅 신고 관리
              </h3>
              <a
                href="/admin/reports"
                target="_blank"
                rel="noopener noreferrer"
                className="px-4 py-2 bg-blue-500/20 hover:bg-blue-500/30 border border-blue-500/30 rounded-lg transition-colors flex items-center gap-2"
              >
                <MessageSquare className="w-4 h-4 text-blue-400" />
                <span className="text-white font-medium">신고 관리 페이지로 이동</span>
              </a>
            </div>
            <div className="text-center py-8">
              <MessageSquare className="w-16 h-16 mx-auto mb-4 text-blue-400 opacity-50" />
              <p className="text-gray-300 mb-4">
                채팅 신고를 관리하려면 전용 관리 페이지를 사용하세요.
              </p>
              <p className="text-sm text-gray-400">
                전용 페이지에서 신고 내역 조회, 상태 변경, 통계 확인 등의 작업을 수행할 수 있습니다.
              </p>
            </div>
          </div>
        )}

        {/* 시스템 알림 관리 탭 */}
        {activeTab === 'notifications' && (
          <>
            {/* 알림 생성 버튼 */}
            <div className="mb-6 flex justify-end">
              <button
                onClick={() => setCreateNotificationModal(true)}
                className="px-6 py-3 bg-blue-500/20 hover:bg-blue-500/30 border border-blue-500/30 rounded-lg transition-colors flex items-center gap-2"
              >
                <Send className="w-5 h-5 text-blue-400" />
                <span className="text-white font-semibold">새 알림 생성</span>
              </button>
            </div>

            {/* 알림 유형별 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-5 gap-6 mb-8">
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <MessageSquare className="w-8 h-8 mx-auto mb-2 text-blue-400" />
                  <div className="text-2xl font-bold text-white">
                    {systemNotifications.filter(n => n.type === 'SYSTEM_ANNOUNCEMENT').length}
                  </div>
                  <div className="text-blue-200 text-sm">시스템 공지</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <Users className="w-8 h-8 mx-auto mb-2 text-green-400" />
                  <div className="text-2xl font-bold text-white">
                    {systemNotifications.filter(n => n.type === 'ADMIN_MESSAGE').length}
                  </div>
                  <div className="text-blue-200 text-sm">운영자 메시지</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <RotateCcw className="w-8 h-8 mx-auto mb-2 text-yellow-400" />
                  <div className="text-2xl font-bold text-white">
                    {systemNotifications.filter(n => n.type === 'UPDATE_NOTIFICATION').length}
                  </div>
                  <div className="text-blue-200 text-sm">업데이트 알림</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <Settings className="w-8 h-8 mx-auto mb-2 text-red-400" />
                  <div className="text-2xl font-bold text-white">
                    {systemNotifications.filter(n => n.type === 'MAINTENANCE_NOTICE').length}
                  </div>
                  <div className="text-blue-200 text-sm">점검 안내</div>
                </div>
              </div>
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="text-center">
                  <Gift className="w-8 h-8 mx-auto mb-2 text-purple-400" />
                  <div className="text-2xl font-bold text-white">
                    {systemNotifications.filter(n => n.type === 'EVENT_NOTIFICATION').length}
                  </div>
                  <div className="text-blue-200 text-sm">이벤트 알림</div>
                </div>
              </div>
            </div>

            {/* 최근 알림 목록 */}
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl border border-white/20 overflow-hidden">
              <div className="px-6 py-4 border-b border-white/20">
                <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                  <Bell className="w-5 h-5 text-blue-400" />
                  최근 발송된 알림
                  <button
                    onClick={loadSystemNotifications}
                    disabled={notificationsLoading}
                    className="ml-auto p-2 rounded-lg bg-white/20 hover:bg-white/30 disabled:bg-gray-500/20 transition-colors"
                  >
                    {notificationsLoading ? (
                      <Clock className="w-4 h-4 animate-spin text-gray-400" />
                    ) : (
                      <RotateCcw className="w-4 h-4 text-white" />
                    )}
                  </button>
                </h3>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-white/20">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">유형</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">제목</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">대상</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">발송일</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">상태</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/10">
                    {notificationsLoading ? (
                      <tr>
                        <td colSpan={5} className="px-6 py-4 text-center text-white">로딩 중...</td>
                      </tr>
                    ) : systemNotifications.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-6 py-4 text-center text-gray-400">발송된 알림이 없습니다.</td>
                      </tr>
                    ) : (
                      systemNotifications.map((notification, index) => (
                        <tr key={index} className="hover:bg-white/5 transition-colors">
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-2">
                              {getNotificationTypeIcon(notification.type)}
                              <span className="text-white text-sm">
                                {getNotificationTypeName(notification.type)}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-white font-medium">{notification.title}</td>
                          <td className="px-6 py-4 text-gray-300">
                            {notification.targetType === 'ALL' ? '전체 사용자' :
                             notification.targetType === 'SPECIFIC' ? `${notification.targetUserCount || 0}명` :
                             '미지정'}
                          </td>
                          <td className="px-6 py-4 text-gray-300">
                            {notification.createdAt ? new Date(notification.createdAt).toLocaleString('ko-KR') : '미지정'}
                          </td>
                          <td className="px-6 py-4">
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                              notification.status === 'SENT' ? 'bg-green-500/20 text-green-200' :
                              notification.status === 'SENDING' ? 'bg-yellow-500/20 text-yellow-200' :
                              'bg-gray-500/20 text-gray-200'
                            }`}>
                              {notification.status === 'SENT' ? '발송완료' :
                               notification.status === 'SENDING' ? '발송중' : '대기'}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}

        {/* 시스템 통계 탭 */}
        {activeTab === 'statistics' && (
          <>
            {/* 차트 색상 */}
            {(() => {
              const COLORS = ['#8B5CF6', '#06B6D4', '#10B981', '#F59E0B', '#EF4444', '#3B82F6', '#EC4899'];
              
              if (statsLoading) {
                return (
                  <div className="min-h-[60vh] flex items-center justify-center">
                    <div className="text-center">
                      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-white mx-auto mb-4"></div>
                      <div className="text-white text-xl">통계를 불러오는 중...</div>
                    </div>
                  </div>
                );
              }

              return (
                <>
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
                              {systemStats?.totalUsers?.toLocaleString() ?? '—'}
                            </p>
                            <p className="text-green-400 text-sm">+{systemStats?.newUsersToday ?? 0} 오늘</p>
                          </div>
                          <Users className="w-12 h-12 text-blue-400" />
                        </div>
                      </div>

                      <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-blue-200 text-sm font-medium">총 추천곡</p>
                            <p className="text-3xl font-bold text-white">
                              {systemStats?.totalRecommendations?.toLocaleString() ?? '—'}
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
                            <p className="text-3xl font-bold text-white">{systemStats?.matchSuccessRate ?? 0}%</p>
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
                              {systemStats?.activeUsers?.toLocaleString() ?? '—'}
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
                            <p className="text-3xl font-bold text-white">{systemStats?.avgSessionTime ?? 0}분</p>
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
                  <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20 mb-8">
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
                  <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
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
                </>
              );
            })()}
          </>
        )}

        {/* 배치 배지 부여 모달 - 배지 관리 탭에서만 표시 */}
        {batchBadgeModal && activeTab === 'badges' && (
          <BatchBadgeModal 
            users={users}
            onClose={() => setBatchBadgeModal(false)}
            onUpdate={loadUsers}
          />
        )}
        
        {/* 시스템 알림 생성 모달 - 시스템 알림 탭에서만 표시 */}
        {createNotificationModal && activeTab === 'notifications' && (
          <SystemNotificationModal 
            users={users}
            onClose={() => setCreateNotificationModal(false)}
            onUpdate={loadSystemNotifications}
          />
        )}
      </div>
    </div>
  );
};

interface ReportActionsProps {
  report: ReviewReport;
  onStatusUpdate: (reportId: number, status: string) => void;
}

const ReportActions: React.FC<ReportActionsProps> = ({ report, onStatusUpdate }) => {
  const [showMenu, setShowMenu] = useState(false);
  const [buttonPosition, setButtonPosition] = useState({ top: 0, left: 0 });
  const buttonRef = React.useRef<HTMLButtonElement>(null);

  const handleButtonClick = () => {
    if (buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      setButtonPosition({
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX
      });
    }
    setShowMenu(!showMenu);
  };

  const handleStatusUpdate = (newStatus: string) => {
    setShowMenu(false);
    onStatusUpdate(report.id, newStatus);
  };

  return (
    <>
      <button
        ref={buttonRef}
        onClick={handleButtonClick}
        className="p-2 rounded-lg bg-white/20 hover:bg-white/30 transition-colors"
      >
        <MoreHorizontal className="w-4 h-4 text-white" />
      </button>

      {showMenu && createPortal(
        <>
          <div 
            className="fixed inset-0 z-[9998]" 
            onClick={() => setShowMenu(false)}
          />
          <div 
            className="fixed bg-white/10 backdrop-blur-lg border border-white/20 rounded-lg shadow-xl py-2 min-w-40 z-[9999]"
            style={{ 
              top: `${buttonPosition.top + 4}px`,
              left: `${buttonPosition.left}px`,
            }}
          >
            <button
              onClick={() => handleStatusUpdate('REVIEWING')}
              className="flex items-center gap-2 w-full px-4 py-2 text-sm text-white hover:bg-white/20 transition-colors"
            >
              <Clock className="w-4 h-4 text-blue-400" />
              검토중으로 변경
            </button>
            <button
              onClick={() => handleStatusUpdate('RESOLVED')}
              className="flex items-center gap-2 w-full px-4 py-2 text-sm text-white hover:bg-white/20 transition-colors"
            >
              <CheckCircle className="w-4 h-4 text-green-400" />
              해결됨으로 처리
            </button>
            <button
              onClick={() => handleStatusUpdate('DISMISSED')}
              className="flex items-center gap-2 w-full px-4 py-2 text-sm text-white hover:bg-white/20 transition-colors"
            >
              <XCircle className="w-4 h-4 text-gray-400" />
              기각으로 처리
            </button>
          </div>
        </>,
        document.body
      )}
    </>
  );
};

interface UserActionsProps {
  user: User;
  onUserUpdate: () => void;
}

const UserActions: React.FC<UserActionsProps> = ({ user, onUserUpdate }) => {
  const [showMenu, setShowMenu] = useState(false);
  const [buttonPosition, setButtonPosition] = useState({ top: 0, left: 0 });
  const buttonRef = React.useRef<HTMLButtonElement>(null);

  const handleButtonClick = () => {
    if (buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      setButtonPosition({
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX
      });
    }
    setShowMenu(!showMenu);
  };

  const handleUserAction = async (action: 'suspend' | 'activate' | 'delete') => {
    setShowMenu(false);
    
    const actionText = {
      suspend: '정지',
      activate: '활성화',
      delete: '탈퇴 처리'
    };

    if (!window.confirm(`정말로 이 사용자를 ${actionText[action]}하시겠습니까?`)) {
      return;
    }

    try {
      const response = await fetch(`/api/users/${user.id}/${action}`, {
        method: 'POST',
        credentials: 'include'
      });

      if (response.ok) {
        try {
          const data = await response.json();
          alert(data.message || `${actionText[action]}가 완료되었습니다.`);
        } catch (jsonError) {
          // JSON 파싱 실패 시에도 성공으로 간주 (서버에서 빈 응답을 보낸 경우)
          alert(`${actionText[action]}가 완료되었습니다.`);
        }
        onUserUpdate();
      } else {
        try {
          const errorData = await response.json();
          alert(errorData.error || `${actionText[action]}에 실패했습니다.`);
        } catch (jsonError) {
          // JSON 파싱 실패 시 기본 메시지 표시
          alert(`${actionText[action]}에 실패했습니다.`);
        }
      }
    } catch (error) {
      console.error(`사용자 ${action} 실패:`, error);
      alert(`${actionText[action]} 중 오류가 발생했습니다.`);
    }
  };

  // 관리자 계정(ID: 1)은 액션 버튼을 비활성화
  if (user.id === 1) {
    return (
      <span className="text-gray-400 text-sm">관리자 계정</span>
    );
  }

  return (
    <>
      <button
        ref={buttonRef}
        onClick={handleButtonClick}
        className="p-2 rounded-lg bg-white/20 hover:bg-white/30 transition-colors"
      >
        <MoreHorizontal className="w-4 h-4 text-white" />
      </button>

      {showMenu && createPortal(
        <>
          <div 
            className="fixed inset-0 z-[9998]" 
            onClick={() => setShowMenu(false)}
          />
          <div 
            className="fixed bg-white/10 backdrop-blur-lg border border-white/20 rounded-lg shadow-xl py-2 min-w-40 z-[9999]"
            style={{ 
              top: `${buttonPosition.top + 4}px`,
              left: `${buttonPosition.left}px`,
            }}
          >
            {user.status === 'ACTIVE' && (
              <button
                onClick={() => handleUserAction('suspend')}
                className="flex items-center gap-2 w-full px-4 py-2 text-sm text-white hover:bg-white/20 transition-colors"
              >
                <Ban className="w-4 h-4 text-red-400" />
                계정 정지
              </button>
            )}
            {user.status === 'SUSPENDED' && (
              <button
                onClick={() => handleUserAction('activate')}
                className="flex items-center gap-2 w-full px-4 py-2 text-sm text-white hover:bg-white/20 transition-colors"
              >
                <UserCheck className="w-4 h-4 text-green-400" />
                정지 해제
              </button>
            )}
            {user.status === 'DELETED' && (
              <button
                onClick={() => handleUserAction('activate')}
                className="flex items-center gap-2 w-full px-4 py-2 text-sm text-white hover:bg-white/20 transition-colors"
              >
                <UserCheck className="w-4 h-4 text-green-400" />
                계정 복구
              </button>
            )}
            {user.status !== 'DELETED' && (
              <button
                onClick={() => handleUserAction('delete')}
                className="flex items-center gap-2 w-full px-4 py-2 text-sm text-white hover:bg-white/20 transition-colors"
              >
                <Trash2 className="w-4 h-4 text-gray-400" />
                탈퇴 처리
              </button>
            )}
          </div>
        </>,
        document.body
      )}
    </>
  );
};

// 배지 관리 모달 컴포넌트
interface BadgeManagementModalProps {
  user: User;
  onClose: () => void;
  onUpdate: () => void;
}

interface Badge {
  id: number;
  badgeType: string;
  badgeName: string;
  description: string;
  rarity: string;
  earnedAt: string;
}

interface BadgeType {
  type: string;
  name: string;
  description: string;
  rarity: string;
}

const BadgeManagementModal: React.FC<BadgeManagementModalProps> = ({ user, onClose, onUpdate }) => {
  const [userBadges, setUserBadges] = useState<Badge[]>([]);
  const [renderKey, setRenderKey] = useState(0); // 강제 리렌더링용
  const [availableBadgeTypes] = useState<BadgeType[]>([
    // 리뷰 관련 배지 (COMMON)
    { type: 'FIRST_REVIEW', name: '첫 리뷰어 🎵', description: '첫 번째 리뷰를 작성했습니다', rarity: 'COMMON' },
    
    // 리뷰 관련 배지 (UNCOMMON)
    { type: 'REVIEW_MASTER', name: '리뷰 마스터 📝', description: '많은 리뷰를 작성했습니다', rarity: 'UNCOMMON' },
    
    // 리뷰 관련 배지 (RARE)
    { type: 'HELPFUL_REVIEWER', name: '도움이 되는 리뷰어 👍', description: '도움이 되는 리뷰를 많이 작성했습니다', rarity: 'RARE' },
    { type: 'CRITIC', name: '음악 평론가 🎭', description: '전문적인 음악 리뷰를 작성했습니다', rarity: 'RARE' },
    
    // 음악 탐험 배지 (UNCOMMON)
    { type: 'GENRE_EXPLORER', name: '장르 탐험가 🗺️', description: '다양한 장르를 탐험했습니다', rarity: 'UNCOMMON' },
    
    // 음악 탐험 배지 (RARE)
    { type: 'EARLY_ADOPTER', name: '얼리 어답터 ⚡', description: '새로운 음악을 빠르게 발견했습니다', rarity: 'RARE' },
    
    // 음악 탐험 배지 (EPIC)
    { type: 'MUSIC_DISCOVERER', name: '음악 발굴자 💎', description: '숨겨진 명곡을 발굴했습니다', rarity: 'EPIC' },
    
    // 소셜 활동 배지 (COMMON)
    { type: 'SOCIAL_BUTTERFLY', name: '소셜 나비 🦋', description: '활발한 소셜 활동을 했습니다', rarity: 'COMMON' },
    
    // 소셜 활동 배지 (UNCOMMON)
    { type: 'FRIEND_MAKER', name: '친구 메이커 🤝', description: '많은 친구를 사귀었습니다', rarity: 'UNCOMMON' },
    
    // 소셜 활동 배지 (EPIC)
    { type: 'CHAT_MASTER', name: '대화의 달인 💬', description: '채팅에서 활발하게 소통했습니다', rarity: 'EPIC' },
    
    // 특별 배지 (LEGENDARY)
    { type: 'BETA_TESTER', name: '베타 테스터 🧪', description: '베타 테스트에 참여했습니다', rarity: 'LEGENDARY' },
    { type: 'ANNIVERSARY', name: '기념일 🎉', description: '특별한 기념일에 참여했습니다', rarity: 'LEGENDARY' },
    { type: 'SPECIAL_EVENT', name: '특별 이벤트 🌟', description: '특별 이벤트에 참여했습니다', rarity: 'LEGENDARY' },
    
    // 운영자 전용 배지 (LEGENDARY)
    { type: 'ADMIN', name: '시스템 관리자 👑', description: '시스템을 관리합니다', rarity: 'LEGENDARY' },
    { type: 'FOUNDER', name: '창립자 🏆', description: '서비스의 창립자입니다', rarity: 'LEGENDARY' },
    { type: 'COMMUNITY_LEADER', name: '커뮤니티 리더 🌟', description: '커뮤니티를 이끌고 있습니다', rarity: 'LEGENDARY' },
    { type: 'CONTENT_CURATOR', name: '콘텐츠 큐레이터 🎨', description: '콘텐츠를 큐레이션합니다', rarity: 'LEGENDARY' },
    { type: 'TRENDSETTER', name: '트렌드세터 🔥', description: '트렌드를 선도합니다', rarity: 'LEGENDARY' },
    { type: 'QUALITY_GUARDIAN', name: '품질 관리자 🛡️', description: '품질을 관리합니다', rarity: 'LEGENDARY' },
    { type: 'INNOVATION_PIONEER', name: '혁신 개척자 🚀', description: '혁신을 개척합니다', rarity: 'LEGENDARY' },
    { type: 'MUSIC_SCHOLAR', name: '음악 학자 🎓', description: '음악에 대한 깊은 지식을 가지고 있습니다', rarity: 'LEGENDARY' },
    { type: 'PLATINUM_MEMBER', name: '플래티넘 멤버 💎', description: '최고 등급의 멤버입니다', rarity: 'LEGENDARY' },
  ]);
  const [selectedBadgeType, setSelectedBadgeType] = useState('');
  const [customName, setCustomName] = useState('');
  const [customDescription, setCustomDescription] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadUserBadges();
  }, [user.id]);

  const loadUserBadges = async () => {
    try {
      console.log('🔄 loadUserBadges 시작 - userId:', user.id);
      // 캐시 무효화를 위해 타임스탬프 추가
      const timestamp = new Date().getTime();
      const response = await fetch(`http://localhost:9090/api/badges/user/${user.id}?_t=${timestamp}`, {
        credentials: 'include',
        cache: 'no-cache'
      });
      console.log('📡 API 응답:', response.status, response.ok);
      if (response.ok) {
        const badges = await response.json();
        console.log('📥 받은 배지 데이터:', badges);
        console.log('📊 배지 개수:', badges.length);
        // 배지 상태를 강제로 업데이트
        setUserBadges([]);
        setTimeout(() => {
          setUserBadges(badges);
          console.log('✅ 배지 상태 업데이트 완료');
        }, 10);
      }
    } catch (error) {
      console.error('사용자 배지 로드 실패:', error);
    }
  };

  const awardBadge = async () => {
    if (!selectedBadgeType) {
      alert('배지 타입을 선택해주세요.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('http://localhost:9090/api/badges/admin/award', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          userId: user.id,
          badgeType: selectedBadgeType,
          customName: customName || undefined,
          customDescription: customDescription || undefined,
          expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
        }),
      });

      if (response.ok) {
        alert('배지가 성공적으로 부여되었습니다!');
        loadUserBadges();
        setSelectedBadgeType('');
        setCustomName('');
        setCustomDescription('');
        setExpiresAt('');
        onUpdate();
      } else {
        const errorText = await response.text();
        alert(`배지 부여 실패: ${errorText}`);
      }
    } catch (error) {
      console.error('배지 부여 실패:', error);
      alert('배지 부여 중 오류가 발생했습니다.');
    }
    setLoading(false);
  };

  const revokeBadge = async (badgeType: string) => {
    if (!window.confirm('정말로 이 배지를 회수하시겠습니까?')) return;

    console.log('🗑️ 배지 회수 시작 - userId:', user.id, 'badgeType:', badgeType);
    console.log('📋 현재 보유 배지 목록:', userBadges.map(b => b.badgeType));

    try {
      const response = await fetch(`http://localhost:9090/api/badges/admin/revoke/${user.id}/${badgeType}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      console.log('📡 배지 회수 API 응답:', response.status, response.ok);

      if (response.ok) {
        alert('배지가 성공적으로 회수되었습니다!');
        
        console.log('🔄 renderKey 업데이트 중...');
        // 강제 리렌더링
        setRenderKey(prev => {
          const newKey = prev + 1;
          console.log('🔑 renderKey:', prev, '->', newKey);
          return newKey;
        });
        
        console.log('📡 loadUserBadges 호출 중...');
        // 배지 목록 다시 로드 (awardBadge와 동일한 패턴)
        await loadUserBadges();
        
        console.log('📞 onUpdate 호출 중...');
        onUpdate();
        console.log('✅ 배지 회수 완료');
      } else {
        const errorText = await response.text();
        console.error('❌ 배지 회수 실패:', errorText);
        alert(`배지 회수 실패: ${errorText}`);
      }
    } catch (error) {
      console.error('배지 회수 실패:', error);
      alert('배지 회수 중 오류가 발생했습니다.');
    }
  };

  const getRarityColor = (rarity: string) => {
    switch (rarity) {
      case 'COMMON': return 'text-gray-400';
      case 'UNCOMMON': return 'text-green-400';
      case 'RARE': return 'text-blue-400';
      case 'EPIC': return 'text-purple-400';
      case 'LEGENDARY': return 'text-yellow-400';
      default: return 'text-gray-400';
    }
  };

  const getRarityBg = (rarity: string) => {
    switch (rarity) {
      case 'COMMON': return 'bg-gray-500/20';
      case 'UNCOMMON': return 'bg-green-500/20';
      case 'RARE': return 'bg-blue-500/20';
      case 'EPIC': return 'bg-purple-500/20';
      case 'LEGENDARY': return 'bg-yellow-500/20';
      default: return 'bg-gray-500/20';
    }
  };

  return createPortal(
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-[10000]">
      <div className="bg-gradient-to-br from-purple-900/90 to-pink-800/90 backdrop-blur-lg border border-white/20 rounded-2xl p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <Award className="w-6 h-6 text-yellow-400" />
            {user.name} 님의 배지 관리
          </h2>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-white/20 hover:bg-white/30 transition-colors"
          >
            <XCircle className="w-5 h-5 text-white" />
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* 현재 보유 배지 */}
          <div>
            <h3 className="text-xl font-semibold text-white mb-4">보유 배지 ({userBadges.length}개)</h3>
            <div key={renderKey} className="space-y-3 max-h-96 overflow-y-auto">
              {userBadges.length === 0 ? (
                <p className="text-gray-400 text-center py-8">보유한 배지가 없습니다.</p>
              ) : (
                userBadges.map((badge) => (
                  <div key={`${badge.id}-${renderKey}`} className={`p-4 rounded-lg ${getRarityBg(badge.rarity)} border border-white/20`}>
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <h4 className="font-semibold text-white">{badge.badgeName}</h4>
                        <p className="text-sm text-gray-300 mt-1">{badge.description}</p>
                        <div className="flex items-center gap-2 mt-2">
                          <span className={`text-xs px-2 py-1 rounded ${getRarityBg(badge.rarity)} ${getRarityColor(badge.rarity)}`}>
                            {badge.rarity}
                          </span>
                          <span className="text-xs text-gray-400">
                            {new Date(badge.earnedAt).toLocaleDateString()}
                          </span>
                        </div>
                      </div>
                      <button
                        onClick={() => revokeBadge(badge.badgeType)}
                        className="ml-2 p-2 rounded-lg bg-red-500/20 hover:bg-red-500/30 transition-colors"
                        title="배지 회수"
                      >
                        <Trash2 className="w-4 h-4 text-red-400" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* 새 배지 부여 */}
          <div>
            <h3 className="text-xl font-semibold text-white mb-4">새 배지 부여</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-white mb-2">배지 타입</label>
                <select
                  value={selectedBadgeType}
                  onChange={(e) => setSelectedBadgeType(e.target.value)}
                  className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white"
                >
                  <option value="">배지를 선택해주세요</option>
                  {availableBadgeTypes.map((badgeType) => (
                    <option key={badgeType.type} value={badgeType.type} className="bg-gray-800 text-white">
                      {badgeType.name} ({badgeType.rarity})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-white mb-2">커스텀 이름 (선택사항)</label>
                <input
                  type="text"
                  value={customName}
                  onChange={(e) => setCustomName(e.target.value)}
                  className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white placeholder-gray-400"
                  placeholder="기본 이름을 사용하려면 비워두세요"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-white mb-2">커스텀 설명 (선택사항)</label>
                <textarea
                  value={customDescription}
                  onChange={(e) => setCustomDescription(e.target.value)}
                  className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white placeholder-gray-400"
                  placeholder="기본 설명을 사용하려면 비워두세요"
                  rows={3}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-white mb-2">만료일 (선택사항)</label>
                <input
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                  className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white"
                />
              </div>

              <button
                onClick={awardBadge}
                disabled={loading || !selectedBadgeType}
                className="w-full py-3 px-6 bg-yellow-500/20 hover:bg-yellow-500/30 disabled:bg-gray-500/20 border border-yellow-500/30 disabled:border-gray-500/30 rounded-lg transition-colors flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <Clock className="w-5 h-5 animate-spin text-gray-400" />
                    <span className="text-gray-400">부여 중...</span>
                  </>
                ) : (
                  <>
                    <Award className="w-5 h-5 text-yellow-400" />
                    <span className="text-white font-semibold">배지 부여</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
};

// 배치 배지 부여 모달 컴포넌트
interface BatchBadgeModalProps {
  users: User[];
  onClose: () => void;
  onUpdate: () => void;
}

const BatchBadgeModal: React.FC<BatchBadgeModalProps> = ({ users, onClose, onUpdate }) => {
  const [selectedUsers, setSelectedUsers] = useState<number[]>([]);
  const [selectedBadgeType, setSelectedBadgeType] = useState('');
  const [customName, setCustomName] = useState('');
  const [customDescription, setCustomDescription] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<any>(null);

  const availableBadgeTypes = [
    { type: 'FIRST_REVIEW', name: '첫 리뷰어 🎵', rarity: 'COMMON' },
    { type: 'REVIEW_MASTER', name: '리뷰 마스터 📝', rarity: 'UNCOMMON' },
    { type: 'HELPFUL_REVIEWER', name: '도움이 되는 리뷰어 👍', rarity: 'RARE' },
    { type: 'CRITIC', name: '음악 평론가 🎭', rarity: 'RARE' },
    { type: 'GENRE_EXPLORER', name: '장르 탐험가 🗺️', rarity: 'UNCOMMON' },
    { type: 'EARLY_ADOPTER', name: '얼리 어답터 ⚡', rarity: 'RARE' },
    { type: 'MUSIC_DISCOVERER', name: '음악 발굴자 💎', rarity: 'EPIC' },
    { type: 'SOCIAL_BUTTERFLY', name: '소셜 나비 🦋', rarity: 'COMMON' },
    { type: 'FRIEND_MAKER', name: '친구 메이커 🤝', rarity: 'UNCOMMON' },
    { type: 'CHAT_MASTER', name: '대화의 달인 💬', rarity: 'EPIC' },
    { type: 'BETA_TESTER', name: '베타 테스터 🧪', rarity: 'LEGENDARY' },
    { type: 'ANNIVERSARY', name: '기념일 🎉', rarity: 'LEGENDARY' },
    { type: 'SPECIAL_EVENT', name: '특별 이벤트 🌟', rarity: 'LEGENDARY' },
    { type: 'ADMIN', name: '시스템 관리자 👑', rarity: 'LEGENDARY' },
    { type: 'FOUNDER', name: '창립자 🏆', rarity: 'LEGENDARY' },
    { type: 'COMMUNITY_LEADER', name: '커뮤니티 리더 🌟', rarity: 'LEGENDARY' },
    { type: 'CONTENT_CURATOR', name: '콘텐츠 큐레이터 🎨', rarity: 'LEGENDARY' },
    { type: 'TRENDSETTER', name: '트렌드세터 🔥', rarity: 'LEGENDARY' },
    { type: 'QUALITY_GUARDIAN', name: '품질 관리자 🛡️', rarity: 'LEGENDARY' },
    { type: 'INNOVATION_PIONEER', name: '혁신 개척자 🚀', rarity: 'LEGENDARY' },
    { type: 'MUSIC_SCHOLAR', name: '음악 학자 🎓', rarity: 'LEGENDARY' },
    { type: 'PLATINUM_MEMBER', name: '플래티넘 멤버 💎', rarity: 'LEGENDARY' },
  ];

  const handleUserToggle = (userId: number) => {
    setSelectedUsers(prev => 
      prev.includes(userId) 
        ? prev.filter(id => id !== userId)
        : [...prev, userId]
    );
  };

  const handleSelectAll = () => {
    if (selectedUsers.length === users.length) {
      setSelectedUsers([]);
    } else {
      setSelectedUsers(users.map(u => u.id));
    }
  };

  const handleBatchAward = async () => {
    if (selectedUsers.length === 0) {
      alert('사용자를 선택해주세요.');
      return;
    }

    if (!selectedBadgeType) {
      alert('배지 타입을 선택해주세요.');
      return;
    }

    setLoading(true);
    try {
      const requestBody = {
        userIds: selectedUsers,
        badgeType: selectedBadgeType,
        customName: customName || undefined,
        customDescription: customDescription || undefined,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      };

      const response = await fetch('http://localhost:9090/api/badges/admin/batch-award', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(requestBody),
      });

      if (response.ok) {
        const result = await response.json();
        setResults(result);
        onUpdate();
      } else {
        const errorText = await response.text();
        alert(`배치 배지 부여 실패: ${errorText}`);
      }
    } catch (error) {
      console.error('배치 배지 부여 실패:', error);
      alert('배치 배지 부여 중 오류가 발생했습니다.');
    }
    setLoading(false);
  };

  return createPortal(
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-[10000]">
      <div className="bg-gradient-to-br from-purple-900/90 to-pink-800/90 backdrop-blur-lg border border-white/20 rounded-2xl p-6 w-full max-w-6xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <Award className="w-6 h-6 text-yellow-400" />
            배치 배지 부여
          </h2>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-white/20 hover:bg-white/30 transition-colors"
          >
            <XCircle className="w-5 h-5 text-white" />
          </button>
        </div>

        {results ? (
          <div className="space-y-4">
            <div className="bg-green-500/20 border border-green-500/30 rounded-lg p-4">
              <h3 className="text-lg font-semibold text-white mb-2">배치 부여 완료!</h3>
              <div className="text-white text-sm space-y-1">
                <p>✅ 성공: {results.successCount}명</p>
                <p>❌ 실패: {results.failCount}명</p>
              </div>
            </div>

            <div className="space-y-2">
              <h4 className="font-medium text-white">상세 결과:</h4>
              <div className="max-h-60 overflow-y-auto space-y-1">
                {results.results?.map((result: any, index: number) => (
                  <div key={index} className={`p-2 rounded text-sm ${
                    result.success ? 'bg-green-500/20 text-green-200' : 'bg-red-500/20 text-red-200'
                  }`}>
                    사용자 ID {result.userId}: {result.message}
                  </div>
                ))}
              </div>
            </div>

            <button
              onClick={onClose}
              className="w-full py-3 px-6 bg-blue-500/20 hover:bg-blue-500/30 border border-blue-500/30 rounded-lg transition-colors"
            >
              <span className="text-white font-semibold">닫기</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* 사용자 선택 */}
            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-xl font-semibold text-white">사용자 선택 ({selectedUsers.length}/{users.length})</h3>
                <button
                  onClick={handleSelectAll}
                  className="px-3 py-1 bg-blue-500/20 hover:bg-blue-500/30 border border-blue-500/30 rounded text-white text-sm"
                >
                  {selectedUsers.length === users.length ? '전체 해제' : '전체 선택'}
                </button>
              </div>
              <div className="space-y-2 max-h-80 overflow-y-auto">
                {users.map((user) => (
                  <div key={user.id} className="flex items-center p-3 bg-white/10 rounded-lg">
                    <input
                      type="checkbox"
                      checked={selectedUsers.includes(user.id)}
                      onChange={() => handleUserToggle(user.id)}
                      className="mr-3 w-4 h-4"
                    />
                    <div className="flex items-center gap-3 flex-1">
                      {user.profileImageUrl && (
                        <img 
                          src={user.profileImageUrl} 
                          alt={user.name}
                          className="w-6 h-6 rounded-full"
                        />
                      )}
                      <div>
                        <span className="text-white font-medium">{user.name}</span>
                        <span className="text-gray-400 text-sm ml-2">ID: {user.id}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 배지 설정 */}
            <div>
              <h3 className="text-xl font-semibold text-white mb-4">배지 설정</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-white mb-2">배지 타입</label>
                  <select
                    value={selectedBadgeType}
                    onChange={(e) => setSelectedBadgeType(e.target.value)}
                    className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white"
                  >
                    <option value="">배지를 선택해주세요</option>
                    {availableBadgeTypes.map((badgeType) => (
                      <option key={badgeType.type} value={badgeType.type} className="bg-gray-800 text-white">
                        {badgeType.name} ({badgeType.rarity})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-white mb-2">커스텀 이름 (선택사항)</label>
                  <input
                    type="text"
                    value={customName}
                    onChange={(e) => setCustomName(e.target.value)}
                    className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white placeholder-gray-400"
                    placeholder="기본 이름을 사용하려면 비워두세요"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-white mb-2">커스텀 설명 (선택사항)</label>
                  <textarea
                    value={customDescription}
                    onChange={(e) => setCustomDescription(e.target.value)}
                    className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white placeholder-gray-400"
                    placeholder="기본 설명을 사용하려면 비워두세요"
                    rows={3}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-white mb-2">만료일 (선택사항)</label>
                  <input
                    type="datetime-local"
                    value={expiresAt}
                    onChange={(e) => setExpiresAt(e.target.value)}
                    className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white"
                  />
                </div>

                <button
                  onClick={handleBatchAward}
                  disabled={loading || selectedUsers.length === 0 || !selectedBadgeType}
                  className="w-full py-3 px-6 bg-yellow-500/20 hover:bg-yellow-500/30 disabled:bg-gray-500/20 border border-yellow-500/30 disabled:border-gray-500/30 rounded-lg transition-colors flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <>
                      <Clock className="w-5 h-5 animate-spin text-gray-400" />
                      <span className="text-gray-400">부여 중...</span>
                    </>
                  ) : (
                    <>
                      <Award className="w-5 h-5 text-yellow-400" />
                      <span className="text-white font-semibold">배치 배지 부여 ({selectedUsers.length}명)</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};

// 사용자 배지 관리 행 컴포넌트
interface UserBadgeRowProps {
  user: User;
  onUpdate: () => void;
}

const UserBadgeRow: React.FC<UserBadgeRowProps> = ({ user, onUpdate }) => {
  const [showBadgeModal, setShowBadgeModal] = useState(false);
  const [badgeCount, setBadgeCount] = useState<number>(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadBadgeCount();
  }, [user.id]);

  const loadBadgeCount = async () => {
    try {
      setLoading(true);
      const response = await fetch(`http://localhost:9090/api/badges/user/${user.id}`, {
        credentials: 'include'
      });
      if (response.ok) {
        const badges = await response.json();
        setBadgeCount(badges.length);
      }
    } catch (error) {
      console.error('배지 개수 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <tr className="hover:bg-white/5">
        <td className="px-6 py-4 text-white">
          <div className="flex items-center gap-3">
            {user.profileImageUrl && (
              <img 
                src={user.profileImageUrl} 
                alt={user.name}
                className="w-8 h-8 rounded-full"
              />
            )}
            <div>
              <span className="font-medium">{user.name}</span>
              {user.id === 1 && (
                <span className="ml-2 px-2 py-1 bg-red-500/20 text-red-200 text-xs rounded-full">
                  관리자
                </span>
              )}
              <div className="text-xs text-gray-400">ID: {user.id}</div>
            </div>
          </div>
        </td>
        <td className="px-6 py-4 text-white text-sm">{user.email}</td>
        <td className="px-6 py-4 text-white">
          {loading ? (
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 animate-spin text-gray-400" />
              <span className="text-sm text-gray-400">로딩 중...</span>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Award className="w-4 h-4 text-yellow-400" />
              <span className="font-medium">{badgeCount}개</span>
            </div>
          )}
        </td>
        <td className="px-6 py-4">
          <span className={`px-3 py-1 text-xs rounded-full font-medium ${
            user.status === 'ACTIVE' ? 'bg-green-500/20 text-green-200' :
            user.status === 'SUSPENDED' ? 'bg-red-500/20 text-red-200' :
            user.status === 'DELETED' ? 'bg-gray-500/20 text-gray-200' :
            'bg-blue-500/20 text-blue-200'
          }`}>
            {user.status === 'ACTIVE' ? '활성' :
             user.status === 'SUSPENDED' ? '정지' :
             user.status === 'DELETED' ? '탈퇴' : user.status}
          </span>
        </td>
        <td className="px-6 py-4">
          <button
            onClick={() => setShowBadgeModal(true)}
            className="px-4 py-2 bg-yellow-500/20 hover:bg-yellow-500/30 border border-yellow-500/30 rounded-lg transition-colors flex items-center gap-2"
          >
            <Award className="w-4 h-4 text-yellow-400" />
            <span className="text-white text-sm font-medium">배지 관리</span>
          </button>
        </td>
      </tr>
      
      {/* 배지 관리 모달 */}
      {showBadgeModal && (
        <BadgeManagementModal 
          user={user} 
          onClose={() => setShowBadgeModal(false)}
          onUpdate={() => {
            loadBadgeCount();
            onUpdate();
          }}
        />
      )}
    </>
  );
};

// 알림 유형별 아이콘 반환
const getNotificationTypeIcon = (type: string) => {
  switch (type) {
    case 'SYSTEM_ANNOUNCEMENT':
      return <MessageSquare className="w-4 h-4 text-blue-400" />;
    case 'ADMIN_MESSAGE':
      return <Users className="w-4 h-4 text-green-400" />;
    case 'UPDATE_NOTIFICATION':
      return <RotateCcw className="w-4 h-4 text-yellow-400" />;
    case 'MAINTENANCE_NOTICE':
      return <Settings className="w-4 h-4 text-red-400" />;
    case 'EVENT_NOTIFICATION':
      return <Gift className="w-4 h-4 text-purple-400" />;
    default:
      return <Bell className="w-4 h-4 text-gray-400" />;
  }
};

// 알림 유형별 이름 반환
const getNotificationTypeName = (type: string) => {
  switch (type) {
    case 'SYSTEM_ANNOUNCEMENT':
      return '시스템 공지';
    case 'ADMIN_MESSAGE':
      return '운영자 메시지';
    case 'UPDATE_NOTIFICATION':
      return '업데이트 알림';
    case 'MAINTENANCE_NOTICE':
      return '점검 안내';
    case 'EVENT_NOTIFICATION':
      return '이벤트 알림';
    default:
      return '일반 알림';
  }
};

// 시스템 알림 생성 모달 컴포넌트
interface SystemNotificationModalProps {
  users: User[];
  onClose: () => void;
  onUpdate: () => void;
}

const SystemNotificationModal: React.FC<SystemNotificationModalProps> = ({ users, onClose, onUpdate }) => {
  const [notificationType, setNotificationType] = useState('');
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [targetType, setTargetType] = useState<'ALL' | 'SPECIFIC'>('ALL');
  const [selectedUsers, setSelectedUsers] = useState<number[]>([]);
  const [scheduledAt, setScheduledAt] = useState('');
  const [priority, setPriority] = useState<'LOW' | 'NORMAL' | 'HIGH'>('NORMAL');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<any>(null);

  const notificationTypes = [
    { 
      type: 'SYSTEM_ANNOUNCEMENT', 
      name: '📢 시스템 공지사항', 
      description: '시스템 전반에 대한 중요한 공지사항',
      icon: <MessageSquare className="w-5 h-5 text-blue-400" />
    },
    { 
      type: 'ADMIN_MESSAGE', 
      name: '👨‍💼 운영자 메시지', 
      description: '운영진에서 직접 전하는 메시지',
      icon: <Users className="w-5 h-5 text-green-400" />
    },
    { 
      type: 'UPDATE_NOTIFICATION', 
      name: '🔄 업데이트 알림', 
      description: '새로운 기능이나 개선사항 안내',
      icon: <RotateCcw className="w-5 h-5 text-yellow-400" />
    },
    { 
      type: 'MAINTENANCE_NOTICE', 
      name: '🔧 점검 안내', 
      description: '시스템 점검 및 서비스 중단 안내',
      icon: <Settings className="w-5 h-5 text-red-400" />
    },
    { 
      type: 'EVENT_NOTIFICATION', 
      name: '🎉 이벤트 알림', 
      description: '각종 이벤트 및 프로모션 안내',
      icon: <Gift className="w-5 h-5 text-purple-400" />
    }
  ];

  const handleUserToggle = (userId: number) => {
    setSelectedUsers(prev => 
      prev.includes(userId) 
        ? prev.filter(id => id !== userId)
        : [...prev, userId]
    );
  };

  const handleSelectAll = () => {
    if (selectedUsers.length === users.length) {
      setSelectedUsers([]);
    } else {
      setSelectedUsers(users.map(u => u.id));
    }
  };

  const handleSend = async () => {
    if (!notificationType) {
      alert('알림 유형을 선택해주세요.');
      return;
    }

    if (!title.trim()) {
      alert('제목을 입력해주세요.');
      return;
    }

    if (!message.trim()) {
      alert('메시지를 입력해주세요.');
      return;
    }

    if (targetType === 'SPECIFIC' && selectedUsers.length === 0) {
      alert('알림을 받을 사용자를 선택해주세요.');
      return;
    }

    setLoading(true);
    try {
      const requestBody = {
        type: notificationType,
        title: title.trim(),
        message: message.trim(),
        targetType,
        targetUserIds: targetType === 'SPECIFIC' ? selectedUsers : null,
        scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : null,
        priority
      };

      const response = await fetch('/api/notifications/system/send', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(requestBody),
      });

      if (response.ok) {
        const result = await response.json();
        setResults(result);
        onUpdate();
      } else {
        const errorText = await response.text();
        alert(`알림 발송 실패: ${errorText}`);
      }
    } catch (error) {
      console.error('알림 발송 실패:', error);
      alert('알림 발송 중 오류가 발생했습니다.');
    }
    setLoading(false);
  };

  const getPreviewIcon = () => {
    const selectedType = notificationTypes.find(t => t.type === notificationType);
    return selectedType?.icon || <Bell className="w-5 h-5 text-gray-400" />;
  };

  return createPortal(
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-[10000]">
      <div className="bg-gradient-to-br from-purple-900/90 to-pink-800/90 backdrop-blur-lg border border-white/20 rounded-2xl p-6 w-full max-w-6xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <Send className="w-6 h-6 text-blue-400" />
            시스템 알림 생성
          </h2>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-white/20 hover:bg-white/30 transition-colors"
          >
            <XCircle className="w-5 h-5 text-white" />
          </button>
        </div>

        {results ? (
          <div className="space-y-4">
            <div className="bg-green-500/20 border border-green-500/30 rounded-lg p-4">
              <h3 className="text-lg font-semibold text-white mb-2">알림 발송 완료!</h3>
              <div className="text-white text-sm space-y-1">
                <p>✅ 성공: {results.successCount}명</p>
                <p>❌ 실패: {results.failCount}명</p>
                {results.scheduledAt && (
                  <p>⏰ 예약 발송: {new Date(results.scheduledAt).toLocaleString('ko-KR')}</p>
                )}
              </div>
            </div>

            <button
              onClick={onClose}
              className="w-full py-3 px-6 bg-blue-500/20 hover:bg-blue-500/30 border border-blue-500/30 rounded-lg transition-colors"
            >
              <span className="text-white font-semibold">닫기</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* 알림 설정 */}
            <div>
              <h3 className="text-xl font-semibold text-white mb-4">알림 설정</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-white mb-2">알림 유형</label>
                  <div className="grid grid-cols-1 gap-3">
                    {notificationTypes.map((type) => (
                      <button
                        key={type.type}
                        onClick={() => setNotificationType(type.type)}
                        className={`p-4 rounded-lg border transition-all text-left ${
                          notificationType === type.type
                            ? 'bg-blue-500/20 border-blue-500/50 shadow-lg'
                            : 'bg-white/10 border-white/20 hover:bg-white/20'
                        }`}
                      >
                        <div className="flex items-start gap-3">
                          {type.icon}
                          <div>
                            <h4 className="font-medium text-white">{type.name}</h4>
                            <p className="text-sm text-gray-300 mt-1">{type.description}</p>
                          </div>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-white mb-2">제목</label>
                  <input
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white placeholder-gray-400"
                    placeholder="알림 제목을 입력하세요"
                    maxLength={100}
                  />
                  <div className="text-xs text-gray-400 mt-1">{title.length}/100</div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-white mb-2">메시지</label>
                  <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white placeholder-gray-400"
                    placeholder="알림 내용을 입력하세요"
                    rows={4}
                    maxLength={500}
                  />
                  <div className="text-xs text-gray-400 mt-1">{message.length}/500</div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-white mb-2">우선순위</label>
                    <select
                      value={priority}
                      onChange={(e) => setPriority(e.target.value as 'LOW' | 'NORMAL' | 'HIGH')}
                      className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white"
                    >
                      <option value="LOW" className="bg-gray-800">낮음</option>
                      <option value="NORMAL" className="bg-gray-800">보통</option>
                      <option value="HIGH" className="bg-gray-800">높음</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-white mb-2">예약 발송 (선택사항)</label>
                    <input
                      type="datetime-local"
                      value={scheduledAt}
                      onChange={(e) => setScheduledAt(e.target.value)}
                      className="w-full p-3 rounded-lg bg-white/20 border border-white/30 text-white"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-white mb-2">발송 대상</label>
                  <div className="space-y-3">
                    <div className="flex gap-4">
                      <button
                        onClick={() => setTargetType('ALL')}
                        className={`flex-1 p-3 rounded-lg border transition-all ${
                          targetType === 'ALL'
                            ? 'bg-blue-500/20 border-blue-500/50'
                            : 'bg-white/10 border-white/20 hover:bg-white/20'
                        }`}
                      >
                        <div className="flex items-center justify-center gap-2">
                          <Users className="w-5 h-5 text-white" />
                          <span className="text-white font-medium">전체 사용자</span>
                        </div>
                      </button>
                      <button
                        onClick={() => setTargetType('SPECIFIC')}
                        className={`flex-1 p-3 rounded-lg border transition-all ${
                          targetType === 'SPECIFIC'
                            ? 'bg-blue-500/20 border-blue-500/50'
                            : 'bg-white/10 border-white/20 hover:bg-white/20'
                        }`}
                      >
                        <div className="flex items-center justify-center gap-2">
                          <UserCheck className="w-5 h-5 text-white" />
                          <span className="text-white font-medium">특정 사용자</span>
                        </div>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* 대상 선택 및 미리보기 */}
            <div>
              {targetType === 'SPECIFIC' ? (
                <>
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-xl font-semibold text-white">사용자 선택 ({selectedUsers.length}/{users.length})</h3>
                    <button
                      onClick={handleSelectAll}
                      className="px-3 py-1 bg-blue-500/20 hover:bg-blue-500/30 border border-blue-500/30 rounded text-white text-sm"
                    >
                      {selectedUsers.length === users.length ? '전체 해제' : '전체 선택'}
                    </button>
                  </div>
                  <div className="space-y-2 max-h-96 overflow-y-auto">
                    {users.map((user) => (
                      <div key={user.id} className="flex items-center p-3 bg-white/10 rounded-lg">
                        <input
                          type="checkbox"
                          checked={selectedUsers.includes(user.id)}
                          onChange={() => handleUserToggle(user.id)}
                          className="mr-3 w-4 h-4"
                        />
                        <div className="flex items-center gap-3 flex-1">
                          {user.profileImageUrl && (
                            <img 
                              src={user.profileImageUrl} 
                              alt={user.name}
                              className="w-6 h-6 rounded-full"
                            />
                          )}
                          <div>
                            <span className="text-white font-medium">{user.name}</span>
                            <span className="text-gray-400 text-sm ml-2">ID: {user.id}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <>
                  <h3 className="text-xl font-semibold text-white mb-4">알림 미리보기</h3>
                  <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                    <div className="flex items-start gap-3">
                      {getPreviewIcon()}
                      <div className="flex-1">
                        <h4 className="text-white font-semibold text-lg">
                          {title || '알림 제목이 여기에 표시됩니다'}
                        </h4>
                        <p className="text-gray-300 mt-2">
                          {message || '알림 내용이 여기에 표시됩니다'}
                        </p>
                        <div className="flex items-center gap-4 mt-4 text-sm text-gray-400">
                          <span>우선순위: {priority === 'HIGH' ? '높음' : priority === 'NORMAL' ? '보통' : '낮음'}</span>
                          <span>대상: 전체 사용자 ({users.length}명)</span>
                          {scheduledAt && (
                            <span>예약: {new Date(scheduledAt).toLocaleString('ko-KR')}</span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                </>
              )}

              <div className="mt-6">
                <button
                  onClick={handleSend}
                  disabled={loading || !notificationType || !title.trim() || !message.trim()}
                  className="w-full py-3 px-6 bg-blue-500/20 hover:bg-blue-500/30 disabled:bg-gray-500/20 border border-blue-500/30 disabled:border-gray-500/30 rounded-lg transition-colors flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <>
                      <Clock className="w-5 h-5 animate-spin text-gray-400" />
                      <span className="text-gray-400">발송 중...</span>
                    </>
                  ) : (
                    <>
                      <Send className="w-5 h-5 text-blue-400" />
                      <span className="text-white font-semibold">
                        {scheduledAt ? '예약 발송' : '지금 발송'} 
                        {targetType === 'ALL' ? ` (전체 ${users.length}명)` : ` (${selectedUsers.length}명)`}
                      </span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};

export default AdminPanel;


