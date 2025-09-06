import React, { useState, useEffect, useCallback } from 'react';
import { ChevronLeft, ChevronRight, User, Calendar, AlertTriangle, CheckCircle, XCircle, Eye, MessageSquare, Clock, Send } from 'lucide-react';
import { motion } from 'framer-motion';

interface ChatReport {
  id: number;
  reporterId: number;
  reportedUserId: number;
  chatRoomId: string;
  reportType: string;
  reason: string;
  additionalInfo?: string;
  status: 'PENDING' | 'UNDER_REVIEW' | 'RESOLVED' | 'DISMISSED';
  adminResponse?: string;
  adminUserId?: number;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}

interface ReportStatistics {
  pendingCount: number;
  underReviewCount: number;
  resolvedCount: number;
  dismissedCount: number;
  totalCount: number;
  lastWeekCount: number;
  reportTypeStats: { [key: string]: number };
}

interface ChatMessage {
  id: number;
  content: string;
  senderId: number;
  senderName: string;
  createdAt: string;
  type: string;
}

const AdminReports: React.FC = () => {
  const [reports, setReports] = useState<ChatReport[]>([]);
  const [statistics, setStatistics] = useState<ReportStatistics | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<string>('all');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selectedReport, setSelectedReport] = useState<ChatReport | null>(null);
  const [adminResponse, setAdminResponse] = useState('');
  const [reportTypes, setReportTypes] = useState<{ [key: string]: string }>({});
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  // ProtectedRoute에서 권한 체크를 하므로 이 컴포넌트에서는 제거

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  };

  const statusLabels = {
    all: '전체',
    PENDING: '대기중',
    UNDER_REVIEW: '검토중',
    RESOLVED: '해결됨',
    DISMISSED: '기각됨'
  };

  const statusColors = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    UNDER_REVIEW: 'bg-blue-100 text-blue-800',
    RESOLVED: 'bg-green-100 text-green-800',
    DISMISSED: 'bg-gray-100 text-gray-800'
  };

  const fetchReportTypes = useCallback(async () => {
    try {
      const response = await fetch('/api/chat-reports/types', {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          setReportTypes(data.reportTypes);
        }
      }
    } catch (error) {
      console.error('신고 유형 조회 실패:', error);
    }
  }, []);

  const fetchStatistics = useCallback(async () => {
    try {
      const response = await fetch('/api/chat-reports/admin/statistics', {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          setStatistics(data.statistics);
        }
      }
    } catch (error) {
      console.error('통계 조회 실패:', error);
    }
  }, []);

  const fetchReports = useCallback(async (status: string = 'all', page: number = 0) => {
    setLoading(true);
    try {
      const endpoint = status === 'all' 
        ? `/api/chat-reports/admin/all?page=${page}&size=20`
        : `/api/chat-reports/admin/status/${status}?page=${page}&size=20`;
      
      const response = await fetch(endpoint, {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          setReports(data.reports);
          setCurrentPage(data.currentPage);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
        }
      }
    } catch (error) {
      console.error('신고 목록 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchChatMessages = useCallback(async (reportId: number) => {
    setLoadingMessages(true);
    try {
      const response = await fetch(`/api/chat-reports/admin/${reportId}/messages`, {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          setChatMessages(data.messages || []);
        }
      }
    } catch (error) {
      console.error('채팅 메시지 조회 실패:', error);
      setChatMessages([]);
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  const updateReportStatus = async (reportId: number, status: string, response: string) => {
    try {
      const requestBody = {
        status,
        adminResponse: response,
        adminUserId: 1 // TODO: 실제 관리자 ID 사용
      };

      const apiResponse = await fetch(`/api/chat-reports/admin/${reportId}/status`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(requestBody)
      });

      if (apiResponse.ok) {
        const data = await apiResponse.json();
        if (data.success) {
          setSelectedReport(null);
          setAdminResponse('');
          fetchReports(selectedStatus, currentPage);
          fetchStatistics();
        } else {
          alert('상태 업데이트 실패: ' + data.error);
        }
      }
    } catch (error) {
      console.error('상태 업데이트 실패:', error);
      alert('상태 업데이트 중 오류가 발생했습니다.');
    }
  };

  const handleStatusChange = (status: string) => {
    setSelectedStatus(status);
    setCurrentPage(0);
    fetchReports(status, 0);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    fetchReports(selectedStatus, page);
  };

  useEffect(() => {
    fetchReportTypes();
    fetchStatistics();
    fetchReports();
  }, [fetchReportTypes, fetchStatistics, fetchReports]);

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">채팅 신고 관리</h1>
          <p className="text-gray-600">사용자 신고를 검토하고 관리합니다.</p>
        </div>

        {/* 통계 카드 */}
        {statistics && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center">
                <div className="p-3 rounded-full bg-yellow-100">
                  <AlertTriangle className="w-6 h-6 text-yellow-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">대기중</p>
                  <p className="text-2xl font-semibold text-gray-900">{statistics.pendingCount}</p>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center">
                <div className="p-3 rounded-full bg-blue-100">
                  <Eye className="w-6 h-6 text-blue-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">검토중</p>
                  <p className="text-2xl font-semibold text-gray-900">{statistics.underReviewCount}</p>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center">
                <div className="p-3 rounded-full bg-green-100">
                  <CheckCircle className="w-6 h-6 text-green-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">해결됨</p>
                  <p className="text-2xl font-semibold text-gray-900">{statistics.resolvedCount}</p>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center">
                <div className="p-3 rounded-full bg-gray-100">
                  <XCircle className="w-6 h-6 text-gray-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-500">기각됨</p>
                  <p className="text-2xl font-semibold text-gray-900">{statistics.dismissedCount}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 필터 탭 */}
        <div className="bg-white rounded-lg shadow mb-6">
          <div className="border-b border-gray-200">
            <nav className="flex space-x-8 px-6">
              {Object.entries(statusLabels).map(([key, label]) => (
                <button
                  key={key}
                  onClick={() => handleStatusChange(key)}
                  className={`py-4 px-1 border-b-2 font-medium text-sm ${
                    selectedStatus === key
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  {label}
                </button>
              ))}
            </nav>
          </div>
        </div>

        {/* 신고 목록 */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-medium text-gray-900">
              신고 목록 ({totalElements}개)
            </h3>
          </div>

          {loading ? (
            <div className="p-6 text-center">
              <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
              <p className="mt-2 text-gray-500">로딩 중...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      신고 정보
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      유형
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      상태
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      신고일
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      작업
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {reports.map((report) => (
                    <tr key={report.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div>
                          <p className="text-sm font-medium text-gray-900">
                            신고 #{report.id}
                          </p>
                          <p className="text-sm text-gray-500">
                            신고자: {report.reporterId} → 피신고자: {report.reportedUserId}
                          </p>
                          <p className="text-sm text-gray-500">
                            채팅방: {report.chatRoomId}
                          </p>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-sm text-gray-900">
                          {reportTypes[report.reportType] || report.reportType}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          statusColors[report.status]
                        }`}>
                          {statusLabels[report.status]}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatDate(report.createdAt)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <button
                          onClick={() => {
                            setSelectedReport(report);
                            fetchChatMessages(report.id);
                          }}
                          className="text-blue-600 hover:text-blue-900"
                        >
                          상세보기
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
              <div className="flex-1 flex justify-between sm:hidden">
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                  className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  이전
                </button>
                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === totalPages - 1}
                  className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  다음
                </button>
              </div>
              <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-gray-700">
                    총 <span className="font-medium">{totalElements}</span>개 중{' '}
                    <span className="font-medium">{currentPage * 20 + 1}</span>-
                    <span className="font-medium">
                      {Math.min((currentPage + 1) * 20, totalElements)}
                    </span>개 표시
                  </p>
                </div>
                <div>
                  <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                    <button
                      onClick={() => handlePageChange(currentPage - 1)}
                      disabled={currentPage === 0}
                      className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      <ChevronLeft className="w-5 h-5" />
                    </button>
                    {Array.from({ length: totalPages }, (_, i) => (
                      <button
                        key={i}
                        onClick={() => handlePageChange(i)}
                        className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                          currentPage === i
                            ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                            : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                        }`}
                      >
                        {i + 1}
                      </button>
                    ))}
                    <button
                      onClick={() => handlePageChange(currentPage + 1)}
                      disabled={currentPage === totalPages - 1}
                      className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      <ChevronRight className="w-5 h-5" />
                    </button>
                  </nav>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 상세 모달 */}
        {selectedReport && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-11/12 md:w-4/5 lg:w-4/5 max-w-6xl shadow-lg rounded-md bg-white">
              <div className="mt-3">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-medium text-gray-900">
                    신고 상세 정보 #{selectedReport.id}
                  </h3>
                  <button
                    onClick={() => {
                      setSelectedReport(null);
                      setChatMessages([]);
                      setAdminResponse('');
                    }}
                    className="text-gray-400 hover:text-gray-600"
                  >
                    <XCircle className="w-6 h-6" />
                  </button>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* 왼쪽: 신고 정보 */}
                  <div className="space-y-4">
                    <h4 className="text-md font-medium text-gray-900 mb-3">신고 정보</h4>
                    
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm font-medium text-gray-500">신고자 ID</p>
                        <p className="text-sm text-gray-900">{selectedReport.reporterId}</p>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-500">피신고자 ID</p>
                        <p className="text-sm text-gray-900">{selectedReport.reportedUserId}</p>
                      </div>
                    </div>

                    <div>
                      <p className="text-sm font-medium text-gray-500">채팅방 ID</p>
                      <p className="text-sm text-gray-900 font-mono text-xs bg-gray-100 p-1 rounded">{selectedReport.chatRoomId}</p>
                    </div>

                    <div>
                      <p className="text-sm font-medium text-gray-500">신고 유형</p>
                      <p className="text-sm text-gray-900">
                        {reportTypes[selectedReport.reportType] || selectedReport.reportType}
                      </p>
                    </div>

                    <div>
                      <p className="text-sm font-medium text-gray-500">신고 사유</p>
                      <p className="text-sm text-gray-900 whitespace-pre-wrap bg-gray-50 p-2 rounded">
                        {selectedReport.reason}
                      </p>
                    </div>

                    {selectedReport.additionalInfo && (
                      <div>
                        <p className="text-sm font-medium text-gray-500">추가 정보</p>
                        <p className="text-sm text-gray-900 whitespace-pre-wrap bg-gray-50 p-2 rounded">
                          {selectedReport.additionalInfo}
                        </p>
                      </div>
                    )}

                    <div>
                      <p className="text-sm font-medium text-gray-500">현재 상태</p>
                      <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                        statusColors[selectedReport.status]
                      }`}>
                        {statusLabels[selectedReport.status]}
                      </span>
                    </div>

                    {selectedReport.adminResponse && (
                      <div>
                        <p className="text-sm font-medium text-gray-500">관리자 응답</p>
                        <p className="text-sm text-gray-900 whitespace-pre-wrap bg-blue-50 p-2 rounded">
                          {selectedReport.adminResponse}
                        </p>
                      </div>
                    )}

                    {selectedReport.status === 'PENDING' || selectedReport.status === 'UNDER_REVIEW' ? (
                      <div>
                        <p className="text-sm font-medium text-gray-500 mb-2">관리자 응답</p>
                        <textarea
                          value={adminResponse}
                          onChange={(e) => setAdminResponse(e.target.value)}
                          placeholder="처리 결과와 사유를 입력하세요..."
                          className="w-full p-2 border border-gray-300 rounded-md text-sm"
                          rows={3}
                        />
                        <div className="flex gap-2 mt-3">
                          <button
                            onClick={() => updateReportStatus(selectedReport.id, 'UNDER_REVIEW', adminResponse)}
                            className="px-3 py-1 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700"
                          >
                            검토 시작
                          </button>
                          <button
                            onClick={() => updateReportStatus(selectedReport.id, 'RESOLVED', adminResponse)}
                            className="px-3 py-1 bg-green-600 text-white text-sm font-medium rounded-md hover:bg-green-700"
                          >
                            해결
                          </button>
                          <button
                            onClick={() => updateReportStatus(selectedReport.id, 'DISMISSED', adminResponse)}
                            className="px-3 py-1 bg-gray-600 text-white text-sm font-medium rounded-md hover:bg-gray-700"
                          >
                            기각
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="text-sm text-gray-500">
                        처리 완료됨 - {selectedReport.resolvedAt && formatDate(selectedReport.resolvedAt)}
                      </div>
                    )}
                  </div>

                  {/* 오른쪽: 채팅 대화 내역 */}
                  <div className="space-y-4">
                    <h4 className="text-md font-medium text-gray-900 mb-3 flex items-center gap-2">
                      <MessageSquare className="w-4 h-4" />
                      채팅 대화 내역 (신고 시점까지)
                    </h4>
                    
                    <div className="border border-gray-200 rounded-lg bg-gray-50 h-96 overflow-y-auto">
                      {loadingMessages ? (
                        <div className="flex items-center justify-center h-full">
                          <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
                          <p className="ml-2 text-gray-500">채팅 내역 로딩 중...</p>
                        </div>
                      ) : chatMessages.length > 0 ? (
                        <div className="p-3 space-y-2">
                          {chatMessages.map((message) => (
                            <div
                              key={message.id}
                              className={`flex ${
                                message.senderId === selectedReport.reporterId
                                  ? 'justify-end'
                                  : 'justify-start'
                              }`}
                            >
                              <div
                                className={`max-w-xs lg:max-w-md px-3 py-2 rounded-lg text-sm ${
                                  message.senderId === selectedReport.reporterId
                                    ? 'bg-blue-500 text-white'
                                    : message.senderId === selectedReport.reportedUserId
                                    ? 'bg-red-100 text-red-900'
                                    : 'bg-gray-200 text-gray-900'
                                }`}
                              >
                                <div className="text-xs opacity-70 mb-1">
                                  {message.senderName} ({message.senderId})
                                </div>
                                <div className="break-words">{message.content}</div>
                                <div className="text-xs opacity-60 mt-1">
                                  {formatDate(message.createdAt)}
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div className="flex items-center justify-center h-full text-gray-500">
                          <div className="text-center">
                            <MessageSquare className="w-12 h-12 mx-auto mb-2 opacity-50" />
                            <p>채팅 메시지가 없습니다</p>
                          </div>
                        </div>
                      )}
                    </div>

                    <div className="text-xs text-gray-500 flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      신고 시점: {formatDate(selectedReport.createdAt)}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminReports;