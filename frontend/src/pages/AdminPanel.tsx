import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, CheckCircle, XCircle, Clock, MoreHorizontal, Users, Ban, UserCheck, Trash2 } from 'lucide-react';

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
  const [activeTab, setActiveTab] = useState<'reports' | 'users'>('reports');
  const [users, setUsers] = useState<User[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);

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
    }
  }, [isAdmin, selectedStatus, currentPage, activeTab]);

  const checkAdminAuth = async () => {
    try {
      const response = await fetch('/api/admin/reports/stats', {
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
                      <th className="px-6 py-3 text-left text-xs font-medium text-blue-200 uppercase tracking-wider">리뷰 ID</th>
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
                          <td className="px-6 py-4 text-white">{report.reviewId}</td>
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

export default AdminPanel;
