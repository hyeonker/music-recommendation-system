import React, { useState, useEffect } from 'react';
import { Plus } from 'lucide-react';
import { reviewApi } from '../api/client';
import ReviewForm from '../components/ReviewForm';
import ReviewCard from '../components/ReviewCard';

interface Review {
  id: number;
  userId: number;
  userNickname?: string;
  musicItem: {
    id: number;
    name: string;
    artistName: string;
    albumName?: string;
    imageUrl?: string;
    itemType: string;
  };
  rating: number;
  ratingEmoji: string;
  reviewText?: string;
  tags: string[];
  helpfulCount: number;
  createdAt: string;
}

interface Badge {
  id: number;
  badgeType: string;
  badgeName: string;
  description: string;
  iconUrl?: string;
  rarity: string;
  badgeColor: string;
  earnedAt: string;
}

const Reviews: React.FC = () => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [badges, setBadges] = useState<Badge[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'recent' | 'helpful' | 'my-reviews' | 'my-badges'>('recent');
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    setCurrentPage(0);
    loadReviews();
  }, [activeTab]);

  useEffect(() => {
    loadReviews();
  }, [currentPage]);

  useEffect(() => {
    // 현재 로그인한 사용자 정보 가져오기
    const fetchCurrentUser = async () => {
      try {
        const response = await fetch('http://localhost:9090/api/auth/me', {
          credentials: 'include'
        });
        const data = await response.json();
        if (data.authenticated && data.user) {
          setCurrentUserId(data.user.id);
        }
      } catch (error) {
        console.error('현재 사용자 정보를 가져올 수 없습니다:', error);
      }
    };
    
    fetchCurrentUser();
  }, []);

  // currentUserId가 변경되면 내 리뷰 탭일 때 다시 로드
  useEffect(() => {
    if (currentUserId && activeTab === 'my-reviews') {
      loadReviews();
    }
  }, [currentUserId]);

  const loadReviews = async () => {
    console.log(`=== loadReviews 호출 - activeTab: ${activeTab}, currentPage: ${currentPage} ===`);
    try {
      setLoading(true);
      let data;
      
      switch (activeTab) {
        case 'recent':
          data = await fetch(`http://localhost:9090/api/reviews/recent?page=${currentPage}&size=10`).then(r => r.json());
          setReviews(data.content || []);
          setTotalPages(Math.min(data.totalPages || 0, 3)); // 최대 3페이지
          break;
        case 'helpful':
          data = await fetch(`http://localhost:9090/api/reviews/helpful?page=${currentPage}&size=10`).then(r => r.json());
          setReviews(data.content || []);
          setTotalPages(Math.min(data.totalPages || 0, 3)); // 최대 3페이지
          break;
        case 'my-reviews':
          if (!currentUserId) {
            console.warn('currentUserId가 없습니다. 사용자 정보를 먼저 가져와야 합니다.');
            setReviews([]);
            setTotalPages(0);
            break;
          }
          data = await fetch(`http://localhost:9090/api/reviews/user/${currentUserId}?page=${currentPage}&size=10`, {
            credentials: 'include'
          }).then(r => r.json());
          setReviews(data.content || []);
          setTotalPages(Math.min(data.totalPages || 0, 3)); // 최대 3페이지
          break;
        case 'my-badges':
          data = await fetch('http://localhost:9090/api/badges/my', {
            credentials: 'include'
          }).then(r => r.json());
          setBadges(data || []);
          break;
      }
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleHelpful = async (reviewId: number) => {
    console.log('도움이 됨 클릭:', reviewId);
    console.log('요청 URL:', `http://localhost:9090/api/reviews/${reviewId}/helpful`);
    
    try {
      const response = await fetch(`http://localhost:9090/api/reviews/${reviewId}/helpful`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include'
      });
      
      console.log('응답 상태:', response.status);
      console.log('응답 헤더:', response.headers);
      
      if (response.ok) {
        console.log('도움이 됨 성공');
        // 실시간 업데이트: 해당 리뷰의 카운트를 즉시 증가
        setReviews(prevReviews => 
          prevReviews.map(review => 
            review.id === reviewId 
              ? { ...review, helpfulCount: review.helpfulCount + 1 }
              : review
          )
        );
        // 추가로 서버에서 최신 데이터도 가져오기
        loadReviews();
      } else if (response.status === 400) {
        console.log('이미 도움이 됨을 눌렀습니다');
        alert('이미 이 리뷰를 도움이 됨으로 표시했습니다.');
      } else if (response.status === 404) {
        alert('리뷰를 찾을 수 없습니다.');
      } else {
        const errorText = await response.text();
        console.error('도움이 됨 실패:', response.status, errorText);
        alert(`도움이 됨 처리 실패 (${response.status}): ${errorText || '알 수 없는 오류'}`);
      }
    } catch (error) {
      console.error('Failed to mark as helpful:', error);
      
      if (error instanceof Error) {
        console.error('에러 상세:', {
          name: error.name,
          message: error.message,
          stack: error.stack
        });
      } else {
        console.error('알 수 없는 에러:', error);
      }
      
      // 백엔드가 실행 중인지 확인
      try {
        const healthCheck = await fetch('http://localhost:9090/api/reviews/recent');
        console.log('백엔드 헬스 체크:', healthCheck.status);
      } catch (healthError) {
        console.error('백엔드 연결 실패:', healthError);
        alert('백엔드 서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.');
        return;
      }
      
      alert('도움이 됨 처리 중 오류가 발생했습니다.');
    }
  };

  const handleReviewSubmit = async (reviewData: any) => {
    try {
      const response = await fetch('http://localhost:9090/api/reviews', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(reviewData)
      });

      if (response.ok) {
        setShowReviewForm(false);
        setActiveTab('my-reviews'); // 리뷰 작성 후 "내 리뷰" 탭으로 이동
        setCurrentPage(0); // 첫 페이지로 이동
        
        // 잠시 기다린 후 새로고침 (DB 반영 시간 고려)
        setTimeout(() => {
          loadReviews();
        }, 500);
        
        alert('리뷰가 성공적으로 등록되었습니다!');
      } else if (response.status === 409) {
        alert('이미 이 곡에 대한 리뷰를 작성하셨습니다. 기존 리뷰를 수정하거나 삭제 후 다시 작성해주세요.');
      } else {
        alert('리뷰 등록에 실패했습니다.');
      }
    } catch (error) {
      console.error('Failed to create review:', error);
      alert('리뷰 등록 중 오류가 발생했습니다.');
    }
  };

  const renderRating = (rating: number) => {
    return (
      <div className="flex items-center space-x-1">
        {[1, 2, 3, 4, 5].map(star => (
          <span
            key={star}
            className={star <= rating ? 'text-yellow-400' : 'text-gray-300'}
          >
            ⭐
          </span>
        ))}
      </div>
    );
  };

  const renderReview = (review: Review) => (
    <ReviewCard
      key={review.id}
      review={review}
      currentUserId={currentUserId || undefined}
      onUpdate={loadReviews}
      onHelpful={handleHelpful}
    />
  );

  const renderBadge = (badge: Badge) => (
    <div key={badge.id} className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 text-center border border-white/20 shadow-xl hover:transform hover:scale-105 transition-all">
      <div
        className="w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center text-3xl shadow-lg"
        style={{ 
          backgroundColor: badge.badgeColor || '#3B82F6',
          boxShadow: `0 0 20px ${badge.badgeColor || '#3B82F6'}40`
        }}
      >
        {badge.iconUrl ? (
          <img 
            src={badge.iconUrl} 
            alt={badge.badgeName} 
            className="w-12 h-12 rounded-full"
            onError={(e) => {
              const target = e.currentTarget as HTMLImageElement;
              target.style.display = 'none';
              // 부모 div에 기본 이모지 표시
              if (target.parentElement) {
                target.parentElement.innerHTML = '🏆';
              }
            }}
          />
        ) : (
          '🏆'
        )}
      </div>
      <h3 className="font-bold text-xl mb-2 text-white">{badge.badgeName}</h3>
      <p className="text-sm text-blue-200 mb-4 leading-relaxed">{badge.description}</p>
      <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold shadow-md ${
        badge.rarity === 'LEGENDARY' ? 'bg-gradient-to-r from-yellow-400 to-yellow-600 text-yellow-900' :
        badge.rarity === 'EPIC' ? 'bg-gradient-to-r from-purple-500 to-purple-700 text-white' :
        badge.rarity === 'RARE' ? 'bg-gradient-to-r from-blue-500 to-blue-700 text-white' :
        badge.rarity === 'UNCOMMON' ? 'bg-gradient-to-r from-green-500 to-green-700 text-white' :
        'bg-gradient-to-r from-gray-500 to-gray-700 text-white'
      }`}>
        {badge.rarity}
      </span>
      <p className="text-xs text-blue-300 mt-3">
        {new Date(badge.earnedAt).toLocaleDateString('ko-KR')} 획득
      </p>
    </div>
  );

  return (
    <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 min-h-screen py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-4xl font-bold text-white">음악 리뷰 & 배지</h1>
          <button
            onClick={() => setShowReviewForm(true)}
            className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-medium py-3 px-6 rounded-xl flex items-center space-x-2 transition-all transform hover:scale-105 shadow-lg"
          >
            <Plus className="w-5 h-5" />
            <span>리뷰 작성</span>
          </button>
        </div>
        
        <div className="flex space-x-1 mb-8">
          {[
            { key: 'recent', label: '최신 리뷰' },
            { key: 'helpful', label: '도움이 되는 리뷰' },
            { key: 'my-reviews', label: '내 리뷰' },
            { key: 'my-badges', label: '내 배지' }
          ].map(tab => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              className={`px-6 py-3 rounded-xl font-medium transition-all ${
                activeTab === tab.key
                  ? 'bg-gradient-to-r from-blue-600 to-purple-600 text-white shadow-lg transform scale-105'
                  : 'bg-white/10 backdrop-blur-lg text-white hover:bg-white/20 border border-white/20'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white"></div>
          </div>
        ) : activeTab === 'my-badges' ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {badges.map(renderBadge)}
          </div>
        ) : (
          <>
            <div className="space-y-6">
              {reviews.map(renderReview)}
            </div>
            
            {/* 페이징 UI */}
            {(activeTab === 'recent' || activeTab === 'helpful' || activeTab === 'my-reviews') && totalPages > 1 && (
              <div className="flex justify-center items-center space-x-2 mt-8">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                  disabled={currentPage === 0}
                  className={`px-4 py-2 rounded-lg font-medium transition-all ${
                    currentPage === 0
                      ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                      : 'bg-white/10 backdrop-blur-lg text-white hover:bg-white/20 border border-white/20'
                  }`}
                >
                  이전
                </button>
                
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => setCurrentPage(i)}
                    className={`px-4 py-2 rounded-lg font-medium transition-all ${
                      currentPage === i
                        ? 'bg-gradient-to-r from-blue-600 to-purple-600 text-white shadow-lg transform scale-105'
                        : 'bg-white/10 backdrop-blur-lg text-white hover:bg-white/20 border border-white/20'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}
                
                <button
                  onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                  disabled={currentPage === totalPages - 1}
                  className={`px-4 py-2 rounded-lg font-medium transition-all ${
                    currentPage === totalPages - 1
                      ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                      : 'bg-white/10 backdrop-blur-lg text-white hover:bg-white/20 border border-white/20'
                  }`}
                >
                  다음
                </button>
              </div>
            )}
          </>
        )}

        {/* 리뷰 작성 모달 */}
        {showReviewForm && (
          <ReviewForm
            onClose={() => setShowReviewForm(false)}
            onSubmit={handleReviewSubmit}
          />
        )}
      </div>
    </div>
  );
};

export default Reviews;