import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { Edit, Trash2, Flag, MoreHorizontal, X } from 'lucide-react';
import { validateInput } from '../utils/security';

interface ReviewActionsProps {
  reviewId: number;
  isOwnReview: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
  onReport?: () => void;
}

const ReviewActions: React.FC<ReviewActionsProps> = ({
  reviewId,
  isOwnReview,
  onEdit,
  onDelete,
  onReport
}) => {
  const [showMenu, setShowMenu] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [adminLoading, setAdminLoading] = useState(false);
  const [modalPosition, setModalPosition] = useState<{ top: number; left: number } | null>(null);
  const reportModalRef = useRef<HTMLDivElement>(null);
  const actionsRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    checkAdminStatus();
  }, []);

  // 동적 크기 조정 및 스마트 포지셔닝 로직 - ReviewCard와 완전히 동일
  useEffect(() => {
    if (showReportDialog && actionsRef.current) {
      const calculateModalPosition = () => {
        // 리뷰 카드 전체를 직접 찾기 (부모 컨테이너에서)
        const reviewCardElement = actionsRef.current!.closest('.bg-white\\/10, [class*="bg-white/10"]');
        if (!reviewCardElement) return;
        
        const cardRect = reviewCardElement.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        
        // 모달 실제 크기 측정 (렌더링 후)
        let modalWidth = 900; // 기본값
        let modalHeight = 600; // 기본값
        
        if (reportModalRef.current) {
          const modalRect = reportModalRef.current.getBoundingClientRect();
          modalWidth = modalRect.width || modalWidth;
          modalHeight = modalRect.height || modalHeight;
        }
        
        // 뷰포트 크기에 맞게 최대 크기 제한
        const maxWidth = Math.min(modalWidth, viewportWidth - 40);
        const maxHeight = Math.min(modalHeight, viewportHeight - 40);
        
        // 기본 위치: 리뷰 카드 우측 상단
        let left = cardRect.right + 10;
        let top = cardRect.top;
        
        // 우측 경계 충돌 시 왼쪽으로 이동
        if (left + maxWidth > viewportWidth - 20) {
          left = cardRect.left - maxWidth - 10;
        }
        
        // 왼쪽 경계도 충돌하면 화면 중앙으로
        if (left < 20) {
          left = (viewportWidth - maxWidth) / 2;
        }
        
        // 상단 경계 충돌 시 조정
        if (top < 20) {
          top = 20;
        }
        
        // 하단 경계 충돌 시 위로 이동
        if (top + maxHeight > viewportHeight - 20) {
          top = Math.max(20, viewportHeight - maxHeight - 20);
        }
        
        setModalPosition({ top, left });
      };
      
      // 여러 번 계산하여 정확한 크기 반영
      setTimeout(calculateModalPosition, 10);
      setTimeout(calculateModalPosition, 100);
      setTimeout(calculateModalPosition, 300);
      
      // 윈도우 리사이즈 시 재계산
      const handleResize = () => calculateModalPosition();
      window.addEventListener('resize', handleResize);
      
      return () => {
        window.removeEventListener('resize', handleResize);
      };
    }
  }, [showReportDialog]);

  const checkAdminStatus = async () => {
    setAdminLoading(true);
    try {
      const response = await fetch('/api/reviews/admin-status', {
        credentials: 'include'
      });
      
      // HTML 응답인지 확인 (로그인 페이지로 리다이렉트된 경우)
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('text/html')) {
        console.warn('관리자 상태 확인 - 인증 필요');
        setIsAdmin(false);
        return;
      }
      
      if (response.ok) {
        const data = await response.json();
        setIsAdmin(data.isAdmin);
      } else {
        setIsAdmin(false);
      }
    } catch (error) {
      console.error('관리자 상태 확인 실패:', error);
      setIsAdmin(false);
    } finally {
      setAdminLoading(false);
    }
  };

  const handleDelete = () => {
    setShowDeleteConfirm(false);
    onDelete?.();
  };

  const handleAdminDelete = async (event?: React.MouseEvent) => {
    event?.preventDefault?.();
    event?.stopPropagation?.();
    
    try {
      const response = await fetch(`/api/reviews/${reviewId}/admin`, {
        method: 'DELETE',
        credentials: 'include',
      });
      
      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setShowDeleteConfirm(false);
          alert(result.message || '리뷰가 관리자 권한으로 삭제되었습니다.');
          // 관리자 삭제 성공 시 onDelete 콜백 호출하지 않음 (이중 API 호출 방지)
          window.location.reload(); // 페이지 새로고침으로 UI 업데이트
          return;
        } else {
          setShowDeleteConfirm(false);
          alert(result.message || '삭제에 실패했습니다.');
        }
      } else {
        const errorData = await response.json().catch(() => null);
        setShowDeleteConfirm(false);
        alert(errorData?.message || '관리자 삭제 권한이 없습니다.');
      }
    } catch (error) {
      console.error('관리자 삭제 오류:', error);
      setShowDeleteConfirm(false);
      alert('삭제 중 오류가 발생했습니다.');
    }
  };

  const handleReport = () => {
    setShowReportDialog(false);
    onReport?.();
  };

  return (
    <div ref={actionsRef} className="relative">
      <button
        onClick={() => setShowMenu(!showMenu)}
        className="p-1 hover:bg-gray-100 rounded-full transition-colors"
      >
        <MoreHorizontal className="w-4 h-4 text-gray-500" />
      </button>

      {showMenu && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setShowMenu(false)}
          />
          <div className="absolute right-0 top-8 bg-white border rounded-lg shadow-lg py-2 z-20 min-w-36">
            {isOwnReview ? (
              <>
                <button
                  onClick={() => {
                    setShowMenu(false);
                    onEdit?.();
                  }}
                  className="flex items-center gap-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <Edit className="w-4 h-4" />
                  수정
                </button>
                <button
                  onClick={() => {
                    setShowMenu(false);
                    setShowDeleteConfirm(true);
                  }}
                  className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                >
                  <Trash2 className="w-4 h-4" />
                  삭제
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={() => {
                    setShowMenu(false);
                    setShowReportDialog(true);
                  }}
                  className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                >
                  <Flag className="w-4 h-4" />
                  신고
                </button>
                {isAdmin && !adminLoading && (
                  <button
                    onClick={() => {
                      setShowMenu(false);
                      setShowDeleteConfirm(true);
                    }}
                    className="flex items-center gap-2 w-full px-4 py-2 text-sm text-orange-600 hover:bg-gray-50 border-t border-gray-200"
                  >
                    <Trash2 className="w-4 h-4" />
                    관리자 삭제
                  </button>
                )}
              </>
            )}
          </div>
        </>
      )}

      {/* 신고 모달을 Portal로 렌더링 */}
      {showReportDialog && createPortal(
        <>
          {/* 모달 배경 오버레이 */}
          <div className="fixed inset-0 bg-black bg-opacity-50 z-[9998]" onClick={() => setShowReportDialog(false)} />
          <ReportDialog
            reviewId={reviewId}
            onClose={() => setShowReportDialog(false)}
            onReport={handleReport}
          />
        </>,
        document.body
      )}

      {/* 삭제 확인 다이얼로그 */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-sm mx-4">
            <h3 className="text-lg font-semibold mb-2">리뷰 삭제</h3>
            <p className="text-gray-600 mb-4">
              이 리뷰를 삭제하시겠습니까? 삭제된 리뷰는 복구할 수 없습니다.
            </p>
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-50 rounded"
              >
                취소
              </button>
              {isOwnReview ? (
                <button
                  onClick={handleDelete}
                  className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                >
                  삭제
                </button>
              ) : (
                <button
                  onClick={handleAdminDelete}
                  className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                >
                  관리자 삭제
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

interface ReportDialogProps {
  reviewId: number;
  onClose: () => void;
  onReport: () => void;
}

const ReportDialog: React.FC<ReportDialogProps> = ({ reviewId, onClose, onReport }) => {
  const [reason, setReason] = useState('SPAM');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const reportReasons = [
    { value: 'SPAM', label: '스팸' },
    { value: 'INAPPROPRIATE', label: '부적절한 내용' },
    { value: 'HATE_SPEECH', label: '혐오 발언' },
    { value: 'FAKE_REVIEW', label: '가짜 리뷰' },
    { value: 'COPYRIGHT', label: '저작권 침해' },
    { value: 'OTHER', label: '기타' }
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    // 전송 전 최종 보안 검증
    const descValidation = validateInput(description, '신고 상세 설명');
    if (!descValidation.isValid) {
      alert(descValidation.error || '신고 상세 설명에 올바르지 않은 내용이 포함되어 있습니다');
      setSubmitting(false);
      return;
    }

    try {
      const response = await fetch(`/api/reviews/${reviewId}/report`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ reason, description: descValidation.sanitized })
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          alert('신고가 접수되었습니다.');
          onReport();
          onClose();
        } else {
          alert(result.message || '신고 접수에 실패했습니다.');
        }
      } else {
        const errorData = await response.json().catch(() => null);
        if (response.status === 400 && errorData?.message) {
          // 400 에러 (중복 신고 등)의 경우 서버 메시지 표시
          alert(errorData.message);
        } else {
          alert('신고 접수에 실패했습니다.');
        }
      }
    } catch (error) {
      console.error('Report error:', error);
      alert('신고 접수 중 오류가 발생했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-[9999] bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-2xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto border border-white/20 shadow-2xl">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-white">리뷰 신고</h2>
        <button
          onClick={onClose}
          className="text-blue-200 hover:text-white p-1 rounded-full hover:bg-white/10 transition-colors"
        >
          <X className="w-6 h-6" />
        </button>
      </div>
      
      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-white mb-2">
            신고 사유
          </label>
          <select
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            {reportReasons.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-white mb-2">
            상세 설명 (선택사항)
          </label>
          <textarea
            value={description}
            onChange={(e) => {
              const rawValue = e.target.value;
              const validation = validateInput(rawValue, '신고 상세 설명');
              if (!validation.isValid) {
                alert(validation.error || '올바르지 않은 내용입니다');
                return;
              }
              setDescription(validation.sanitized);
            }}
            placeholder="신고 사유에 대한 상세한 설명을 입력해주세요"
            rows={3}
            className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex gap-2 justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-blue-200 hover:bg-white/10 rounded"
            disabled={submitting}
          >
            취소
          </button>
          <button
            type="submit"
            className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:bg-gray-400"
            disabled={submitting}
          >
            {submitting ? '신고 중...' : '신고하기'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default ReviewActions;