import React, { useState, useEffect } from 'react';
import { Plus } from 'lucide-react';
import { reviewApi } from '../api/client';
import ReviewForm from '../components/ReviewForm';
import ReviewCard from '../components/ReviewCard';

interface Review {
  id: number;
  userId: number;
  userNickname?: string;
  representativeBadge?: {
    id: number;
    badgeType: string;
    badgeName: string;
    description: string;
    iconUrl?: string;
    rarity: string;
    badgeColor: string;
    earnedAt: string;
  };
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

interface BadgeWithProgress {
  badgeType: string;
  badgeName: string;
  description: string;
  iconUrl?: string;
  rarity: string;
  badgeColor: string;
  isEarned: boolean;
  earnedAt?: string;
  badgeId?: number;
  currentProgress: number;
  targetProgress: number;
  progressText: string;
}

interface AllBadgesResponse {
  badges: BadgeWithProgress[];
  totalBadges: number;
  earnedBadges: number;
  completionRate: number;
}

const Reviews: React.FC = () => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [badges, setBadges] = useState<Badge[]>([]);
  const [allBadgesData, setAllBadgesData] = useState<AllBadgesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'recent' | 'helpful' | 'my-reviews' | 'my-badges'>('recent');
  const [badgeSubTab, setBadgeSubTab] = useState<'earned' | 'all'>('earned');
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [representativeBadgeId, setRepresentativeBadgeId] = useState<number | null>(null);

  useEffect(() => {
    setCurrentPage(0);
    loadReviews();
  }, [activeTab, badgeSubTab]);

  useEffect(() => {
    loadReviews();
  }, [currentPage]);

  useEffect(() => {
    // 현재 로그인한 사용자 정보 가져오기 (OAuth2 및 LOCAL 사용자 모두 지원)
    const fetchCurrentUser = async () => {
      try {
        console.log('=== Reviews 현재 사용자 정보 조회 시작 ===');
        
        let userId: number | null = null;
        
        // 먼저 OAuth2 사용자 확인
        try {
          const response = await fetch('http://localhost:9090/api/auth/me', {
            credentials: 'include'
          });
          
          // HTML 응답이 아닌 경우만 처리
          const contentType = response.headers.get('content-type');
          if (response.ok && contentType && contentType.includes('application/json')) {
            const data = await response.json();
            console.log('OAuth2 사용자 인증 성공:', data);
            if (data.authenticated && data.user) {
              userId = data.user.id;
              console.log('OAuth2 사용자 ID 설정:', userId);
            }
          }
        } catch (error) {
          console.log('OAuth2 인증 확인 실패:', error);
        }
        
        // OAuth2 인증이 실패했으면 LOCAL 사용자 확인
        if (!userId) {
          try {
            const response = await fetch('http://localhost:9090/api/auth/local/me', {
              credentials: 'include'
            });
            
            const contentType = response.headers.get('content-type');
            if (response.ok && contentType && contentType.includes('application/json')) {
              const data = await response.json();
              console.log('LOCAL 사용자 인증 응답:', data);
              if (data.success && data.user) {
                userId = data.user.id;
                console.log('LOCAL 사용자 ID 설정:', userId);
              }
            }
          } catch (error) {
            console.log('LOCAL 인증 확인 실패:', error);
          }
        }
        
        if (userId) {
          setCurrentUserId(userId);
          console.log('✅ 최종 사용자 ID 설정:', userId);
        } else {
          console.warn('❌ 사용자 인증 실패 - currentUserId가 null로 설정됨');
          setCurrentUserId(null);
        }
      } catch (error) {
        console.error('현재 사용자 정보를 가져올 수 없습니다:', error);
        setCurrentUserId(null);
      }
    };
    
    fetchCurrentUser();
  }, []);

  // currentUserId가 변경되면 내 리뷰 탭이나 내 배지 탭일 때 다시 로드
  useEffect(() => {
    if (currentUserId && (activeTab === 'my-reviews' || activeTab === 'my-badges')) {
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
          // 획득한 배지 로드
          try {
            const badgeResponse = await fetch('http://localhost:9090/api/badges/my', {
              credentials: 'include'
            });
            
            const contentType = badgeResponse.headers.get('content-type');
            if (badgeResponse.ok && contentType && contentType.includes('application/json')) {
              const badgeData = await badgeResponse.json();
              setBadges(Array.isArray(badgeData) ? badgeData : []);
              console.log('내 배지 로드 성공:', badgeData);
            } else {
              console.warn('배지 API 응답이 JSON이 아님 또는 실패:', badgeResponse.status);
              setBadges([]);
            }
          } catch (error) {
            console.error('내 배지 로드 오류:', error);
            setBadges([]);
          }
          
          // 전체 배지 데이터 로드 (진행률 포함)
          try {
            const allBadgesResponse = await fetch('http://localhost:9090/api/badges/all', {
              credentials: 'include'
            });
            
            const allBadgesContentType = allBadgesResponse.headers.get('content-type');
            if (allBadgesResponse.ok && allBadgesContentType && allBadgesContentType.includes('application/json')) {
              const allBadgesData = await allBadgesResponse.json();
              console.log('=== 전체 배지 데이터 로드 성공 ===', allBadgesData);
              // 전체 배지 데이터가 올바른 구조인지 확인
              if (allBadgesData && Array.isArray(allBadgesData.badges)) {
                setAllBadgesData(allBadgesData);
              } else {
                console.warn('전체 배지 데이터가 예상 구조와 다름:', allBadgesData);
                setAllBadgesData(null);
              }
            } else {
              console.warn('전체 배지 API 응답이 JSON이 아님 또는 실패:', allBadgesResponse.status);
              setAllBadgesData(null);
            }
          } catch (error) {
            console.error('전체 배지 데이터 로드 오류:', error);
            setAllBadgesData(null);
          }
          
          // 현재 설정된 대표 배지 ID 가져오기 (currentUserId가 있을 때만)
          if (currentUserId) {
            try {
              const repResponse = await fetch(`http://localhost:9090/api/badges/user/${currentUserId}/representative`, {
                credentials: 'include'
              });
              if (repResponse.ok) {
                const text = await repResponse.text();
                if (text) {
                  const repBadge = JSON.parse(text);
                  setRepresentativeBadgeId(repBadge.id);
                } else {
                  setRepresentativeBadgeId(null);
                }
              } else if (repResponse.status === 204) {
                // 대표 배지가 설정되지 않음
                setRepresentativeBadgeId(null);
              }
            } catch (repError) {
              console.error('대표 배지 조회 오류:', repError);
              setRepresentativeBadgeId(null);
            }
          }
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
      console.log('Content-Type:', response.headers.get('content-type'));
      
      // HTML 응답인지 확인 (로그인 페이지로 리다이렉트된 경우)
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('text/html')) {
        console.error('HTML 응답 받음 - 로그인 필요');
        alert('로그인이 필요합니다. 다시 로그인해주세요.');
        window.location.href = '/login';
        return;
      }
      
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

      // HTML 응답인지 확인 (로그인 페이지로 리다이렉트된 경우)
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('text/html')) {
        console.error('리뷰 등록 - HTML 응답 받음, 로그인 필요');
        alert('로그인이 필요합니다. 다시 로그인해주세요.');
        window.location.href = '/login';
        return;
      }

      if (response.ok) {
        setShowReviewForm(false);
        alert('리뷰가 성공적으로 등록되었습니다!');
        
        // 리뷰 작성 후 "내 리뷰" 탭으로 이동하고 첫 페이지로 이동
        setActiveTab('my-reviews');
        setCurrentPage(0);
        
        // 상태 변경이 완료된 후 리뷰 다시 로드
        setTimeout(() => {
          loadReviews();
        }, 100);
      } else if (response.status === 409) {
        alert('이미 이 곡에 대한 리뷰를 작성하셨습니다. 기존 리뷰를 수정하거나 삭제 후 다시 작성해주세요.');
      } else {
        const errorText = await response.text();
        console.error('리뷰 등록 실패:', response.status, errorText);
        alert(`리뷰 등록에 실패했습니다. (${response.status})`);
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

  // 대표 배지 설정 함수
  const setRepresentativeBadge = async (badgeId: number) => {
    try {
      const response = await fetch('http://localhost:9090/api/badges/representative', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ badgeId })
      });

      if (response.ok) {
        setRepresentativeBadgeId(badgeId);
        alert('대표 배지가 설정되었습니다!');
      } else {
        const errorText = await response.text();
        alert(`대표 배지 설정 실패: ${errorText}`);
      }
    } catch (error) {
      console.error('대표 배지 설정 오류:', error);
      alert('대표 배지 설정 중 오류가 발생했습니다.');
    }
  };

  // 대표 배지 해제 함수
  const removeRepresentativeBadge = async () => {
    try {
      const response = await fetch('http://localhost:9090/api/badges/representative', {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        setRepresentativeBadgeId(null);
        alert('대표 배지가 해제되었습니다!');
      } else {
        const errorText = await response.text();
        alert(`대표 배지 해제 실패: ${errorText}`);
      }
    } catch (error) {
      console.error('대표 배지 해제 오류:', error);
      alert('대표 배지 해제 중 오류가 발생했습니다.');
    }
  };

  const renderBadgeWithProgress = (badgeData: BadgeWithProgress) => (
    <div key={badgeData.badgeType} className={`bg-white/10 backdrop-blur-lg rounded-2xl p-6 text-center border shadow-xl transition-all duration-500 relative ${
      badgeData.isEarned ? 'hover:transform hover:scale-105' : 'opacity-60'
    } ${
      badgeData.isEarned && representativeBadgeId === badgeData.badgeId 
        ? 'border-2 transform scale-105' 
        : 'border-white/20'
    } ${
      badgeData.isEarned && badgeData.rarity === 'LEGENDARY' ? 'legendary-premium-border border-2' : ''
    }`} style={{
      boxShadow: undefined,
      ...(badgeData.isEarned && representativeBadgeId === badgeData.badgeId ? {
        borderColor: badgeData.badgeColor || '#F59E0B',
        background: `linear-gradient(135deg, rgba(255,255,255,0.1), ${badgeData.badgeColor}15, rgba(255,255,255,0.05))`,
        boxShadow: `inset 0 1px 0 rgba(255,255,255,0.2)`
      } : {}),
      ...(badgeData.isEarned && badgeData.rarity === 'LEGENDARY' ? {
        animation: 'legendary-scale 3s ease-in-out infinite'
      } : {})
    }}>
      {/* 미획득 배지 잠금 표시 */}
      {!badgeData.isEarned && (
        <div className="absolute top-2 right-2 text-gray-400">
          🔒
        </div>
      )}
      
      <div
        className={`w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center text-3xl shadow-lg relative ${
          badgeData.isEarned && badgeData.rarity === 'LEGENDARY' ? '' : ''
        }`}
        style={{ 
          backgroundColor: badgeData.isEarned ? badgeData.badgeColor || '#3B82F6' : '#6B7280',
          boxShadow: 'none',
          ...(badgeData.isEarned && badgeData.rarity === 'LEGENDARY' ? {
            animation: 'none'
          } : {})
        }}
      >
        {/* LEGENDARY 배지 특별 효과 (획득한 경우만) */}
        {badgeData.isEarned && badgeData.rarity === 'LEGENDARY' && (
          <>
            <div className="absolute -inset-0.3 rounded-full animate-ping opacity-6"
                 style={{ backgroundColor: badgeData.badgeColor }}></div>
          </>
        )}
        
        {badgeData.isEarned && representativeBadgeId === badgeData.badgeId && (
          <div className="absolute -top-2 -right-2 rounded-full w-6 h-6 flex items-center justify-center animate-bounce"
               style={{
                 backgroundColor: badgeData.badgeColor || '#F59E0B',
                 boxShadow: 'none'
               }}>
            <span className="text-white text-xs font-bold drop-shadow-sm">★</span>
          </div>
        )}
        
        {badgeData.iconUrl ? (
          <img 
            src={badgeData.iconUrl} 
            alt={badgeData.badgeName} 
            className={`w-12 h-12 rounded-full relative z-10 ${
              badgeData.isEarned && badgeData.rarity === 'LEGENDARY' ? 'animate-bounce' : ''
            } ${!badgeData.isEarned ? 'grayscale' : ''}`}
          />
        ) : (
          <span className={`${badgeData.isEarned && badgeData.rarity === 'LEGENDARY' ? 'animate-bounce' : ''} ${!badgeData.isEarned ? 'grayscale' : ''}`}>🏆</span>
        )}
      </div>
      
      <h3 className={`font-bold text-xl mb-2 text-white ${
        badgeData.isEarned && badgeData.rarity === 'LEGENDARY' ? 'legendary-text-float' : ''
      }`} style={{
        textShadow: 'none'
      }}>
        {badgeData.badgeName}
        {badgeData.isEarned && badgeData.rarity === 'LEGENDARY' && (
          <span className="ml-2 legendary-sparkle">✨👑✨</span>
        )}
      </h3>
      
      <p className="text-sm text-blue-200 mb-4 leading-relaxed">{badgeData.description}</p>
      
      {/* 진행률 바 */}
      {!badgeData.isEarned && badgeData.targetProgress > 0 && (
        <div className="mb-4">
          <div className="flex justify-between text-xs text-blue-300 mb-1">
            <span>{badgeData.currentProgress}/{badgeData.targetProgress}</span>
            <span>{Math.round((badgeData.currentProgress / badgeData.targetProgress) * 100)}%</span>
          </div>
          <div className="w-full bg-gray-600 rounded-full h-2">
            <div 
              className="bg-gradient-to-r from-blue-500 to-purple-500 h-2 rounded-full transition-all duration-500"
              style={{ width: `${Math.min((badgeData.currentProgress / badgeData.targetProgress) * 100, 100)}%` }}
            ></div>
          </div>
          <p className="text-xs text-blue-300 mt-1">{badgeData.progressText}</p>
        </div>
      )}
      
      <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold shadow-md relative ${
        badgeData.rarity === 'LEGENDARY' ? 'bg-gradient-to-r from-yellow-400 to-yellow-600 text-yellow-900 legendary-rarity-glow' :
        badgeData.rarity === 'EPIC' ? 'bg-gradient-to-r from-purple-500 to-purple-700 text-white' :
        badgeData.rarity === 'RARE' ? 'bg-gradient-to-r from-blue-500 to-blue-700 text-white' :
        badgeData.rarity === 'UNCOMMON' ? 'bg-gradient-to-r from-green-500 to-green-700 text-white' :
        'bg-gradient-to-r from-gray-500 to-gray-700 text-white'
      }`} style={{
        boxShadow: 'none'
      }}>
        {badgeData.rarity === 'LEGENDARY' && (
          <div className="absolute -inset-0.15 rounded-full bg-gradient-to-r from-yellow-300 to-yellow-500 animate-ping opacity-8"></div>
        )}
        <span className="relative z-10">
          {badgeData.rarity}
          {badgeData.rarity === 'LEGENDARY' && (
            <span className="ml-1 inline-block">⭐</span>
          )}
        </span>
      </span>
      
      {badgeData.isEarned && (
        <p className="text-xs text-blue-300 mt-3 mb-4">
          {new Date(badgeData.earnedAt!).toLocaleDateString('ko-KR')} 획득
        </p>
      )}
      
      {/* 대표 배지 설정/해제 버튼 (획득한 배지만) */}
      {badgeData.isEarned && (
        <div className="flex flex-col gap-2">
          {representativeBadgeId === badgeData.badgeId ? (
            <button
              onClick={() => removeRepresentativeBadge()}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-all"
            >
              대표 배지 해제
            </button>
          ) : (
            <button
              onClick={() => setRepresentativeBadge(badgeData.badgeId!)}
              className="px-4 py-2 text-white text-sm font-medium rounded-lg transition-all duration-300 hover:transform hover:scale-105"
              style={{
                background: `linear-gradient(135deg, ${badgeData.badgeColor || '#3B82F6'}, ${badgeData.badgeColor || '#3B82F6'}CC)`,
                boxShadow: `0 4px 12px ${badgeData.badgeColor || '#3B82F6'}40`,
                border: `1px solid ${badgeData.badgeColor || '#3B82F6'}60`
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.boxShadow = `0 6px 20px ${badgeData.badgeColor || '#3B82F6'}60`;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.boxShadow = `0 4px 12px ${badgeData.badgeColor || '#3B82F6'}40`;
              }}
            >
              대표 배지 설정
            </button>
          )}
        </div>
      )}
    </div>
  );

  const renderBadge = (badge: Badge) => (
    <div key={badge.id} className={`bg-white/10 backdrop-blur-lg rounded-2xl p-6 text-center border shadow-xl hover:transform hover:scale-105 transition-all duration-500 relative ${
      representativeBadgeId === badge.id 
        ? 'border-2 transform scale-105' 
        : 'border-white/20'
    } ${
      badge.rarity === 'LEGENDARY' ? 'border-2 legendary-premium-border' : ''
    }`} style={{
      boxShadow: undefined,
      ...(representativeBadgeId === badge.id ? {
        borderColor: badge.badgeColor || '#F59E0B',
        background: `linear-gradient(135deg, rgba(255,255,255,0.1), ${badge.badgeColor}15, rgba(255,255,255,0.05))`,
        boxShadow: `inset 0 1px 0 rgba(255,255,255,0.2)`
      } : {}),
      ...(badge.rarity === 'LEGENDARY' ? {
        animation: 'legendary-scale 3s ease-in-out infinite'
      } : {})
    }}>
      <div
        className={`w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center text-3xl shadow-lg relative ${
          badge.rarity === 'LEGENDARY' ? '' : ''
        }`}
        style={{ 
          backgroundColor: badge.badgeColor || '#3B82F6',
          boxShadow: 'none',
          ...(badge.rarity === 'LEGENDARY' ? {
            animation: 'none'
          } : {})
        }}
      >
        {/* LEGENDARY 배지 특별 효과 */}
        {badge.rarity === 'LEGENDARY' && (
          <>
            <div className="absolute -inset-0.3 rounded-full animate-ping opacity-6"
                 style={{ backgroundColor: badge.badgeColor }}></div>
          </>
        )}
        
        {representativeBadgeId === badge.id && (
          <div className="absolute -top-2 -right-2 rounded-full w-6 h-6 flex items-center justify-center animate-bounce"
               style={{
                 backgroundColor: badge.badgeColor || '#F59E0B',
                 boxShadow: 'none'
               }}>
            <span className="text-white text-xs font-bold drop-shadow-sm">★</span>
          </div>
        )}
        {badge.iconUrl ? (
          <img 
            src={badge.iconUrl} 
            alt={badge.badgeName} 
            className={`w-12 h-12 rounded-full relative z-10 ${
              badge.rarity === 'LEGENDARY' ? 'animate-bounce' : ''
            }`}
            onError={(e) => {
              const target = e.currentTarget as HTMLImageElement;
              target.style.display = 'none';
              if (target.parentElement) {
                target.parentElement.innerHTML = '🏆';
              }
            }}
          />
        ) : (
          <span className={badge.rarity === 'LEGENDARY' ? 'animate-bounce' : ''}>🏆</span>
        )}
      </div>
      <h3 className={`font-bold text-xl mb-2 text-white ${
        badge.rarity === 'LEGENDARY' ? 'legendary-text-float' : ''
      }`} style={{
        textShadow: 'none'
      }}>
        {badge.badgeName}
        {badge.rarity === 'LEGENDARY' && (
          <span className="ml-2 legendary-sparkle">✨👑✨</span>
        )}
      </h3>
      <p className="text-sm text-blue-200 mb-4 leading-relaxed">{badge.description}</p>
      <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold shadow-md relative ${
        badge.rarity === 'LEGENDARY' ? 'bg-gradient-to-r from-yellow-400 to-yellow-600 text-yellow-900 legendary-rarity-glow' :
        badge.rarity === 'EPIC' ? 'bg-gradient-to-r from-purple-500 to-purple-700 text-white' :
        badge.rarity === 'RARE' ? 'bg-gradient-to-r from-blue-500 to-blue-700 text-white' :
        badge.rarity === 'UNCOMMON' ? 'bg-gradient-to-r from-green-500 to-green-700 text-white' :
        'bg-gradient-to-r from-gray-500 to-gray-700 text-white'
      }`} style={{
        boxShadow: 'none'
      }}>
        {badge.rarity === 'LEGENDARY' && (
          <div className="absolute -inset-0.15 rounded-full bg-gradient-to-r from-yellow-300 to-yellow-500 animate-ping opacity-8"></div>
        )}
        <span className="relative z-10">
          {badge.rarity}
          {badge.rarity === 'LEGENDARY' && (
            <span className="ml-1 inline-block">⭐</span>
          )}
        </span>
      </span>
      <p className="text-xs text-blue-300 mt-3 mb-4">
        {new Date(badge.earnedAt).toLocaleDateString('ko-KR')} 획득
      </p>
      
      {/* 대표 배지 설정/해제 버튼 */}
      <div className="flex flex-col gap-2">
        {representativeBadgeId === badge.id ? (
          <button
            onClick={() => removeRepresentativeBadge()}
            className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-all"
          >
            대표 배지 해제
          </button>
        ) : (
          <button
            onClick={() => setRepresentativeBadge(badge.id)}
            className="px-4 py-2 text-white text-sm font-medium rounded-lg transition-all duration-300 hover:transform hover:scale-105"
            style={{
              background: `linear-gradient(135deg, ${badge.badgeColor || '#3B82F6'}, ${badge.badgeColor || '#3B82F6'}CC)`,
              boxShadow: `0 4px 12px ${badge.badgeColor || '#3B82F6'}40`,
              border: `1px solid ${badge.badgeColor || '#3B82F6'}60`
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = `0 6px 20px ${badge.badgeColor || '#3B82F6'}60`;
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = `0 4px 12px ${badge.badgeColor || '#3B82F6'}40`;
            }}
          >
            대표 배지 설정
          </button>
        )}
      </div>
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
          <div>
            {/* 배지 서브탭 */}
            <div className="flex space-x-1 mb-6">
              <button
                onClick={() => setBadgeSubTab('earned')}
                className={`px-4 py-2 rounded-lg font-medium transition-all ${
                  badgeSubTab === 'earned'
                    ? 'bg-gradient-to-r from-green-600 to-blue-600 text-white shadow-lg transform scale-105'
                    : 'bg-white/10 backdrop-blur-lg text-white hover:bg-white/20 border border-white/20'
                }`}
              >
                획득한 배지 ({badges.length})
              </button>
              <button
                onClick={() => setBadgeSubTab('all')}
                className={`px-4 py-2 rounded-lg font-medium transition-all ${
                  badgeSubTab === 'all'
                    ? 'bg-gradient-to-r from-green-600 to-blue-600 text-white shadow-lg transform scale-105'
                    : 'bg-white/10 backdrop-blur-lg text-white hover:bg-white/20 border border-white/20'
                }`}
              >
                전체 배지 도감 {allBadgesData ? `(${allBadgesData.earnedBadges}/${allBadgesData.totalBadges})` : ''}
              </button>
            </div>

            {/* 완성률 표시 (전체 배지 도감에서만) */}
            {badgeSubTab === 'all' && allBadgesData && (
              <div className="mb-6 bg-white/10 backdrop-blur-lg rounded-xl p-4 border border-white/20">
                <div className="flex justify-between items-center mb-2">
                  <h3 className="text-lg font-bold text-white">배지 수집 진행률</h3>
                  <span className="text-lg font-bold text-green-400">
                    {Math.round(allBadgesData.completionRate)}%
                  </span>
                </div>
                <div className="w-full bg-gray-600 rounded-full h-3">
                  <div 
                    className="bg-gradient-to-r from-green-500 to-blue-500 h-3 rounded-full transition-all duration-1000"
                    style={{ width: `${allBadgesData.completionRate}%` }}
                  ></div>
                </div>
                <p className="text-sm text-blue-200 mt-2">
                  {allBadgesData.earnedBadges}개 획득 / {allBadgesData.totalBadges}개 전체
                </p>
              </div>
            )}

            {/* 배지 그리드 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {badgeSubTab === 'earned' ? (
                badges.map(renderBadge)
              ) : (
                (() => {
                  console.log('=== 전체 배지 렌더링 체크 ===', { 
                    allBadgesData, 
                    badgesCount: allBadgesData?.badges?.length,
                    badgeSubTab 
                  });
                  return allBadgesData?.badges.map(renderBadgeWithProgress) || [];
                })()
              )}
            </div>
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