import React, { useState } from 'react';
import { Star, Send, X } from 'lucide-react';

interface ReviewFormProps {
  onClose: () => void;
  onSubmit: (reviewData: any) => void;
}

// 검색 관련성 점수 계산 함수
// Legacy function - replaced by calculateEnhancedRelevance
const calculateRelevance = (title: string, query: string): number => {
  const titleLower = title.toLowerCase();
  const queryLower = query.toLowerCase();
  
  if (titleLower === queryLower) return 100;
  if (titleLower.startsWith(queryLower)) return 80;
  if (titleLower.includes(queryLower)) return 60;
  
  const queryWords = queryLower.split(/\s+/);
  const allWordsFound = queryWords.every(word => titleLower.includes(word));
  if (allWordsFound) return 40;
  
  const someWordsFound = queryWords.some(word => titleLower.includes(word));
  if (someWordsFound) return 20;
  
  return 0;
};

const ReviewForm: React.FC<ReviewFormProps> = ({ onClose, onSubmit }) => {
  const [musicSearch, setMusicSearch] = useState('');
  const [selectedMusic, setSelectedMusic] = useState<any>(null);
  const [rating, setRating] = useState(5);
  const [reviewText, setReviewText] = useState('');
  const [tags, setTags] = useState('');
  const [isPublic, setIsPublic] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<any[]>([]);

  const searchMusic = async (query: string) => {
    if (!query.trim()) {
      setSearchResults([]);
      setSelectedMusic(null); // 검색어가 비어있으면 선택된 음악도 초기화
      return;
    }
    
    try {
      setIsSearching(true);
      const response = await fetch(`http://localhost:9090/api/spotify/search/artists?q=${encodeURIComponent(query)}&limit=10`);
      const data = await response.json();
      
      // 아티스트와 트랙을 모두 검색
      const trackResponse = await fetch(`http://localhost:9090/api/spotify/search/tracks?q=${encodeURIComponent(query)}&limit=15`);
      const trackData = await trackResponse.json();
      
      // 아티스트를 ARTIST 타입으로, 트랙을 TRACK 타입으로 변환
      const artists = (data.artists?.items || data.items || []).map((artist: any) => ({
        ...artist,
        itemType: 'ARTIST',
        displayName: artist.name,
        displaySubtitle: `팔로워 ${artist.followers?.total?.toLocaleString() || 0}명`,
        imageUrl: artist.images?.[0]?.url
      }));
      
      // 백엔드에서 직접 TrackDto 배열을 반환하므로 trackData 자체가 배열입니다
      const tracks = (Array.isArray(trackData) ? trackData : []).map((track: any) => ({
        ...track,
        itemType: 'TRACK',
        displayName: track.name,
        displaySubtitle: track.artists?.map((a: any) => a.name).join(', '),
        imageUrl: track.image || track.album?.images?.[0]?.url
      }));
      
      // 트랙을 먼저, 그 다음 아티스트를 표시 (트랙이 더 구체적이므로)
      // 관련성이 높은 순서로 정렬
      const sortedTracks = tracks.sort((a: any, b: any) => {
        const aRelevance = calculateRelevance(a.displayName, query);
        const bRelevance = calculateRelevance(b.displayName, query);
        return bRelevance - aRelevance;
      });
      
      const sortedArtists = artists.sort((a: any, b: any) => {
        const aRelevance = calculateRelevance(a.displayName, query);
        const bRelevance = calculateRelevance(b.displayName, query);
        return bRelevance - aRelevance;
      });
      
      setSearchResults([...sortedTracks, ...sortedArtists]);
    } catch (error) {
      console.error('음악 검색 실패:', error);
    } finally {
      setIsSearching(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMusic) {
      alert('음악을 선택해주세요');
      return;
    }

    const reviewData = {
      externalId: selectedMusic.id,
      itemType: selectedMusic.itemType || 'TRACK',
      musicName: selectedMusic.displayName || selectedMusic.name,
      artistName: selectedMusic.itemType === 'ARTIST' 
        ? selectedMusic.displayName || selectedMusic.name
        : selectedMusic.artists?.map((a: any) => a.name).join(', ') || selectedMusic.displaySubtitle,
      albumName: selectedMusic.album?.name,
      imageUrl: selectedMusic.imageUrl || selectedMusic.image,
      rating,
      reviewText: reviewText.trim(),
      tags: tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0),
      isPublic,
      isSpoiler: false
    };

    console.log('=== ReviewForm에서 전송하는 데이터 ===');
    console.log('selectedMusic:', selectedMusic);
    console.log('reviewData:', reviewData);
    console.log('externalId:', reviewData.externalId);
    console.log('musicName:', reviewData.musicName);
    console.log('artistName:', reviewData.artistName);
    console.log('=====================');

    onSubmit(reviewData);
  };

  const renderStars = () => {
    return (
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map(star => (
          <button
            key={star}
            type="button"
            onClick={() => setRating(star)}
            className={`text-2xl ${star <= rating ? 'text-yellow-400' : 'text-gray-300'} hover:text-yellow-400 transition-colors`}
          >
            <Star className="w-6 h-6" fill={star <= rating ? 'currentColor' : 'none'} />
          </button>
        ))}
        <span className="ml-2 text-gray-600">{rating}점</span>
      </div>
    );
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-50 backdrop-blur-sm">
      <div className="bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-2xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto border border-white/20 shadow-2xl">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-white">음악 리뷰 작성</h2>
          <button
            onClick={onClose}
            className="text-blue-200 hover:text-white p-1 rounded-full hover:bg-white/10 transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 음악 검색 */}
          <div>
            <label className="block text-blue-200 text-sm font-medium mb-2">
              음악 검색 *
            </label>
            <div className="relative">
              <input
                type="text"
                value={musicSearch}
                onChange={(e) => {
                  const value = e.target.value;
                  setMusicSearch(value);
                  
                  // 검색어가 변경되면 이전 선택을 초기화
                  if (selectedMusic && value !== selectedMusic.displayName && value !== selectedMusic.name) {
                    setSelectedMusic(null);
                    console.log('검색어 변경으로 selectedMusic 초기화:', selectedMusic.displayName, '->', value);
                  }
                  
                  searchMusic(value);
                }}
                placeholder="아티스트나 곡명을 입력하세요..."
                className="w-full px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              
              {/* 검색 결과 */}
              {(searchResults.length > 0 || isSearching) && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-2xl border border-gray-200 z-[200] max-h-96 overflow-hidden flex flex-col">
                  <div className="flex justify-between items-center p-3 border-b bg-gradient-to-r from-blue-50 to-purple-50">
                    <div>
                      <h3 className="font-semibold text-gray-800 text-sm">검색 결과</h3>
                      <p className="text-xs text-gray-600">
                        {isSearching ? '검색 중...' : `${searchResults.length}개 결과`}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setSearchResults([]);
                        setMusicSearch('');
                      }}
                      className="text-gray-400 hover:text-gray-600 p-1 rounded-full"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>

                  <div className="flex-1 overflow-y-auto">
                    {isSearching && (
                      <div className="p-4 text-center text-sm text-gray-500">검색 중...</div>
                    )}

                    {!isSearching && searchResults.length === 0 && musicSearch.trim() && (
                      <div className="p-4 text-center text-sm text-gray-500">결과가 없습니다</div>
                    )}

                    {!isSearching &&
                      searchResults.length > 0 &&
                      searchResults.map((item) => (
                        <div
                          key={item.id}
                          onClick={() => {
                            console.log('음악 선택:', item.displayName, 'ID:', item.id);
                            setSelectedMusic(item);
                            setMusicSearch(item.displayName);
                            setSearchResults([]);
                          }}
                          className="flex items-center p-3 hover:bg-gradient-to-r hover:from-blue-50 hover:to-purple-50 cursor-pointer border-b border-gray-100 last:border-b-0 transition-colors"
                        >
                          <img
                            src={item.imageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(item.displayName)}&backgroundColor=random`}
                            alt={item.displayName}
                            className="w-12 h-12 rounded-full object-cover mr-3 shadow-md"
                            onError={(e) => {
                              const t = e.currentTarget as HTMLImageElement;
                              t.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(item.displayName)}&backgroundColor=random`;
                            }}
                          />
                          <div className="flex-1 min-w-0">
                            <div className="font-semibold text-gray-900 text-sm flex items-center">
                              {item.displayName}
                              <span className={`ml-2 px-2 py-1 text-xs rounded-full ${
                                item.itemType === 'ARTIST' 
                                  ? 'bg-purple-100 text-purple-800' 
                                  : 'bg-green-100 text-green-800'
                              }`}>
                                {item.itemType === 'ARTIST' ? '아티스트' : '곡'}
                              </span>
                            </div>
                            <div className="text-xs text-gray-600">
                              {item.displaySubtitle}
                            </div>
                          </div>
                        </div>
                      ))}
                  </div>

                  <div className="p-2 bg-gray-50">
                    <p className="text-xs text-gray-500 text-center">
                      클릭하여 선택하거나 ESC로 닫기
                    </p>
                  </div>
                </div>
              )}
            </div>
            
            {selectedMusic && (
              <div className="mt-3 p-4 bg-white/10 backdrop-blur-lg rounded-lg border border-white/20 flex items-center space-x-3">
                <img
                  src={selectedMusic.imageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(selectedMusic.displayName)}&backgroundColor=random`}
                  alt={selectedMusic.displayName}
                  className="w-12 h-12 rounded-full object-cover shadow-md"
                  onError={(e) => {
                    const t = e.currentTarget as HTMLImageElement;
                    t.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(selectedMusic.displayName)}&backgroundColor=random`;
                  }}
                />
                <div className="flex-1">
                  <div className="font-medium text-white flex items-center">
                    {selectedMusic.displayName}
                    <span className={`ml-2 px-2 py-1 text-xs rounded-full ${
                      selectedMusic.itemType === 'ARTIST' 
                        ? 'bg-purple-500/30 text-purple-200' 
                        : 'bg-green-500/30 text-green-200'
                    }`}>
                      {selectedMusic.itemType === 'ARTIST' ? '아티스트' : '곡'}
                    </span>
                  </div>
                  <div className="text-sm text-blue-200">
                    {selectedMusic.displaySubtitle}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setSelectedMusic(null)}
                  className="text-red-400 hover:text-red-300 p-1 rounded-full hover:bg-white/10 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            )}
          </div>

          {/* 평점 */}
          <div>
            <label className="block text-blue-200 text-sm font-medium mb-2">
              평점 *
            </label>
            {renderStars()}
          </div>

          {/* 리뷰 텍스트 */}
          <div>
            <label className="block text-blue-200 text-sm font-medium mb-2">
              리뷰 내용
            </label>
            <textarea
              value={reviewText}
              onChange={(e) => setReviewText(e.target.value)}
              placeholder="이 음악에 대한 생각을 자유롭게 작성해보세요..."
              rows={4}
              maxLength={2000}
              className="w-full px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
            />
            <div className="text-sm text-blue-200 mt-1">
              {reviewText.length}/2000자
            </div>
          </div>

          {/* 태그 */}
          <div>
            <label className="block text-blue-200 text-sm font-medium mb-2">
              태그
            </label>
            <input
              type="text"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              placeholder="태그를 쉼표로 구분해서 입력하세요 (예: 신남, 감성, 추천)"
              className="w-full px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            <div className="text-sm text-blue-200 mt-1">
              최대 10개의 태그까지 입력 가능합니다
            </div>
          </div>

          {/* 공개 설정 */}
          <div>
            <label className="flex items-center space-x-3">
              <input
                type="checkbox"
                checked={isPublic}
                onChange={(e) => setIsPublic(e.target.checked)}
                className="w-5 h-5 rounded border-white/30 bg-white/20 text-blue-600 focus:ring-blue-500 focus:ring-2"
              />
              <span className="text-sm text-white">리뷰를 공개합니다</span>
            </label>
          </div>

          {/* 버튼 */}
          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2 text-blue-200 border border-white/30 rounded-lg hover:bg-white/10 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={!selectedMusic}
              className="px-6 py-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg hover:from-blue-700 hover:to-purple-700 disabled:from-gray-600 disabled:to-gray-600 disabled:cursor-not-allowed flex items-center space-x-2 transition-all transform hover:scale-105"
            >
              <Send className="w-4 h-4" />
              <span>리뷰 등록</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ReviewForm;