import React, { useState } from 'react';
import { Heart, Calendar } from 'lucide-react';
import ReviewActions from './ReviewActions';
import ReviewForm from './ReviewForm';

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

interface ReviewCardProps {
  review: Review;
  currentUserId?: number;
  onUpdate?: () => void;
  onHelpful?: (reviewId: number) => void;
}

const ReviewCard: React.FC<ReviewCardProps> = ({ 
  review, 
  currentUserId, 
  onUpdate, 
  onHelpful 
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const isOwnReview = currentUserId === review.userId;

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
  };

  const handleSaveEdit = () => {
    setIsEditing(false);
    onUpdate?.();
  };

  const handleDelete = async () => {
    if (deleting) return;
    setDeleting(true);

    try {
      const response = await fetch(`/api/reviews/${review.id}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      if (response.ok) {
        onUpdate?.();
      } else {
        alert('리뷰 삭제에 실패했습니다.');
      }
    } catch (error) {
      console.error('Delete error:', error);
      alert('리뷰 삭제 중 오류가 발생했습니다.');
    } finally {
      setDeleting(false);
    }
  };

  const handleHelpful = () => {
    onHelpful?.(review.id);
  };

  if (isEditing) {
    return (
      <div className="bg-white/10 backdrop-blur-lg rounded-lg shadow-sm border border-white/20 p-6">
        <ReviewForm
          mode="edit"
          initialData={{
            reviewId: review.id,
            externalId: '',
            itemType: review.musicItem.itemType as any,
            musicName: review.musicItem.name,
            artistName: review.musicItem.artistName,
            albumName: review.musicItem.albumName || '',
            imageUrl: review.musicItem.imageUrl || '',
            rating: review.rating,
            reviewText: review.reviewText || '',
            tags: review.tags
          }}
          onSuccess={handleSaveEdit}
          onCancel={handleCancelEdit}
        />
      </div>
    );
  }

  return (
    <div className="bg-white/10 backdrop-blur-lg rounded-lg shadow-sm border border-white/20 p-6 hover:shadow-md transition-shadow">
      <div className="flex items-start gap-4">
        {/* 음악 커버 */}
        <div className="w-16 h-16 bg-gray-200 rounded-lg flex-shrink-0 overflow-hidden">
          {review.musicItem.imageUrl ? (
            <img
              src={review.musicItem.imageUrl}
              alt={review.musicItem.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400">
              🎵
            </div>
          )}
        </div>

        {/* 리뷰 내용 */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <h3 className="font-medium text-white truncate">
                {review.musicItem.name}
              </h3>
              <p className="text-sm text-blue-200">
                {review.musicItem.artistName}
                {review.musicItem.albumName && (
                  <span className="text-blue-300"> • {review.musicItem.albumName}</span>
                )}
              </p>
            </div>
            
            <div className="flex items-center gap-2 ml-4">
              <span className="text-2xl">{review.ratingEmoji}</span>
              <span className="text-sm font-medium text-white">
                {review.rating}/5
              </span>
              <ReviewActions
                reviewId={review.id}
                isOwnReview={isOwnReview}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onReport={onUpdate}
              />
            </div>
          </div>

          {/* 리뷰 텍스트 */}
          {review.reviewText && (
            <p className="mt-3 text-white text-sm leading-relaxed">
              {review.reviewText}
            </p>
          )}

          {/* 태그 */}
          {review.tags.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-1">
              {review.tags.map((tag, index) => (
                <span
                  key={index}
                  className="inline-block px-2 py-1 text-xs bg-blue-500/20 text-blue-200 rounded-full"
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}

          {/* 하단 정보 */}
          <div className="mt-4 flex items-center justify-between text-sm text-blue-200">
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1">
                <Calendar className="w-4 h-4" />
                {formatDate(review.createdAt)}
              </span>
              {review.userNickname && (
                <div className="flex items-center gap-2">
                  <span>by {review.userNickname}</span>
                  {review.representativeBadge && (
                    <div className={`flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium relative ${
                      review.representativeBadge.rarity === 'LEGENDARY' 
                        ? 'legendary-badge-glow legendary-review-border shadow-lg' 
                        : ''
                    }`} 
                         style={{ 
                           backgroundColor: `${review.representativeBadge.badgeColor}20`, 
                           color: review.representativeBadge.badgeColor,
                           boxShadow: review.representativeBadge.rarity === 'LEGENDARY' 
                             ? `0 0 6px ${review.representativeBadge.badgeColor}40, 0 0 10px ${review.representativeBadge.badgeColor}25`
                             : 'none',
                           border: review.representativeBadge.rarity === 'LEGENDARY'
                             ? `1px solid ${review.representativeBadge.badgeColor}60`
                             : 'none'
                         }}>
                      {/* LEGENDARY 배지 반짝임 효과 */}
                      {review.representativeBadge.rarity === 'LEGENDARY' && (
                        <>
                          <div className="absolute -inset-0.3 rounded-full animate-ping opacity-12"
                               style={{ backgroundColor: review.representativeBadge.badgeColor }}></div>
                          <div className="absolute top-0 left-0 w-full h-full rounded-full"
                               style={{ 
                                 background: `linear-gradient(45deg, transparent, ${review.representativeBadge.badgeColor}30, transparent, ${review.representativeBadge.badgeColor}20, transparent)`,
                                 animation: 'legendary-wave 2s ease-in-out infinite'
                               }}>
                          </div>
                        </>
                      )}
                      {review.representativeBadge.iconUrl && (
                        <img src={review.representativeBadge.iconUrl} alt="" 
                             className={`w-4 h-4 rounded-full relative z-10 ${
                               review.representativeBadge.rarity === 'LEGENDARY' ? 'legendary-icon-float' : ''
                             }`} />
                      )}
                      <span className={`relative z-10 font-bold ${
                        review.representativeBadge.rarity === 'LEGENDARY' ? 'legendary-review-text' : ''
                      }`}>{review.representativeBadge.badgeName}</span>
                      {review.representativeBadge.rarity === 'LEGENDARY' && (
                        <span className="legendary-sparkle text-yellow-300">✨</span>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className="flex items-center gap-2">
              {!isOwnReview && (
                <button
                  onClick={handleHelpful}
                  className="flex items-center gap-1 px-3 py-1 text-sm rounded-full bg-white/20 hover:bg-white/30 transition-colors text-white"
                >
                  <Heart className="w-4 h-4" />
                  도움이 됨 ({review.helpfulCount})
                </button>
              )}
              {deleting && (
                <span className="text-xs text-red-500">삭제 중...</span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReviewCard;