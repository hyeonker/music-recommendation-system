import React, { useState } from 'react';
import { Edit, Trash2, Flag, MoreHorizontal } from 'lucide-react';
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

  const handleDelete = () => {
    setShowDeleteConfirm(false);
    onDelete?.();
  };

  const handleReport = () => {
    setShowReportDialog(false);
    onReport?.();
  };

  return (
    <div className="relative">
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
          <div className="absolute right-0 top-8 bg-white border rounded-lg shadow-lg py-2 z-20 min-w-32">
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
            )}
          </div>
        </>
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
              <button
                onClick={handleDelete}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
              >
                삭제
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 신고 다이얼로그 */}
      {showReportDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md mx-4">
            <h3 className="text-lg font-semibold mb-4">리뷰 신고</h3>
            <ReportDialog
              reviewId={reviewId}
              onClose={() => setShowReportDialog(false)}
              onReport={handleReport}
            />
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
        alert('신고 접수에 실패했습니다.');
      }
    } catch (error) {
      console.error('Report error:', error);
      alert('신고 접수 중 오류가 발생했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
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
        <label className="block text-sm font-medium text-gray-700 mb-2">
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
          className="px-4 py-2 text-gray-600 hover:bg-gray-50 rounded"
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
  );
};

export default ReviewActions;