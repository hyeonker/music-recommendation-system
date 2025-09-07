import React, { useState } from 'react';
import { Star, Send, X } from 'lucide-react';
import { validateInput, validateReviewText, validateMusicSearch } from '../utils/security';

interface ReviewFormProps {
  mode?: 'create' | 'edit';
  initialData?: {
    reviewId?: number;
    externalId?: string;
    itemType?: string;
    musicName?: string;
    artistName?: string;
    albumName?: string;
    imageUrl?: string;
    rating?: number;
    reviewText?: string;
    tags?: string[];
  };
  onClose?: () => void;
  onSubmit?: (reviewData: any) => void;
  onSuccess?: () => void;
  onCancel?: () => void;
}

const ReviewForm: React.FC<ReviewFormProps> = ({ 
  mode = 'create',
  initialData,
  onClose,
  onSubmit,
  onSuccess,
  onCancel
}) => {
  // 편집 모드일 때 초기 데이터 설정
  const isEditMode = mode === 'edit';
  
  // 2단계 검색 상태들
  const [artistSearch, setArtistSearch] = useState(initialData?.artistName || '');
  const [selectedArtist, setSelectedArtist] = useState<any>(
    isEditMode && initialData ? {
      name: initialData.artistName,
      displayName: initialData.artistName,
    } : null
  );
  const [songSearch, setSongSearch] = useState(initialData?.musicName || '');
  const [selectedMusic, setSelectedMusic] = useState<any>(
    isEditMode && initialData ? {
      id: initialData.externalId,
      name: initialData.musicName,
      displayName: initialData.musicName,
      imageUrl: initialData.imageUrl,
      artists: [{ name: initialData.artistName }],
      album: { name: initialData.albumName }
    } : null
  );
  
  // 기본 상태들
  const [rating, setRating] = useState(initialData?.rating || 5);
  const [reviewText, setReviewText] = useState(initialData?.reviewText || '');
  const [tags, setTags] = useState(initialData?.tags ? initialData.tags.join(', ') : '');
  const [isPublic, setIsPublic] = useState(true);
  
  // 검색 관련 상태들
  const [isSearchingArtists, setIsSearchingArtists] = useState(false);
  const [isSearchingSongs, setIsSearchingSongs] = useState(false);
  const [artistResults, setArtistResults] = useState<any[]>([]);
  const [songResults, setSongResults] = useState<any[]>([]);

  // 아티스트 검색 함수
  const searchArtists = async (query: string) => {
    if (!query.trim()) {
      setArtistResults([]);
      return;
    }
    
    try {
      setIsSearchingArtists(true);
      const response = await fetch(`http://localhost:9090/api/spotify/search/artists?q=${encodeURIComponent(query)}&limit=10`);
      const data = await response.json();
      
      console.log('아티스트 검색 응답:', data);
      
      // 백엔드에서 직접 ArtistDto 배열을 반환
      const artists = (Array.isArray(data) ? data : []).map((artist: any) => ({
        ...artist,
        displayName: artist.name,
        displaySubtitle: `팔로워 ${artist.followers?.toLocaleString() || 0}명`,
        imageUrl: artist.image
      }));
      
      console.log('매핑된 아티스트:', artists);
      setArtistResults(artists);
    } catch (error) {
      console.error('아티스트 검색 실패:', error);
    } finally {
      setIsSearchingArtists(false);
    }
  };

  // 선택된 아티스트의 곡 검색 함수
  const searchArtistSongs = async (query: string) => {
    if (!selectedArtist) {
      setSongResults([]);
      return;
    }
    
    try {
      setIsSearchingSongs(true);
      
      let allTracks: any[] = [];
      
      // 1. 선택된 아티스트의 인기곡 가져오기
      try {
        const topTracksResponse = await fetch(`http://localhost:9090/api/spotify/artist/${selectedArtist.id}/top-tracks?limit=20`);
        const topTracks = await topTracksResponse.json();
        
        const processedTopTracks = topTracks.map((track: any) => ({
          ...track,
          displayName: track.name,
          displaySubtitle: `${track.artists?.map((a: any) => a.name).join(', ')} • 인기곡`,
          imageUrl: track.image,
          isTopTrack: true
        }));
        
        allTracks.push(...processedTopTracks);
      } catch (error) {
        console.error('인기곡 조회 실패:', error);
      }
      
      // 2. 검색어가 있으면 아티스트별 곡 검색 사용
      if (query.trim()) {
        try {
          const searchResponse = await fetch(`http://localhost:9090/api/spotify/search/artist-tracks?artist=${encodeURIComponent(selectedArtist.name)}&q=${encodeURIComponent(query)}&limit=20`);
          const searchTracks = await searchResponse.json();
          
          const processedSearchTracks = (Array.isArray(searchTracks) ? searchTracks : [])
            .filter((track: any) => 
              // 중복 제거: 이미 인기곡에 있는 곡은 제외
              !allTracks.some(existing => existing.id === track.id)
            )
            .map((track: any) => ({
              ...track,
              displayName: track.name,
              displaySubtitle: `${track.artists?.map((a: any) => a.name).join(', ')} • 검색결과`,
              imageUrl: track.image,
              isTopTrack: false
            }));
          
          allTracks.push(...processedSearchTracks);
        } catch (error) {
          console.error('아티스트 곡 검색 실패:', error);
        }
      }
      
      // 3. 검색어로 필터링 (검색어가 있는 경우) - 구두점 차이 허용
      let filteredTracks = allTracks;
      if (query.trim()) {
        const normalizedQuery = query.toLowerCase().replace(/[,\s]+/g, ' ').trim();
        filteredTracks = allTracks.filter((track: any) => {
          const normalizedTrackName = track.name.toLowerCase().replace(/[,\s]+/g, ' ').trim();
          return normalizedTrackName.includes(normalizedQuery) || normalizedQuery.includes(normalizedTrackName);
        });
      }
      
      // 4. 인기곡을 먼저 표시하고 나머지는 뒤에
      filteredTracks.sort((a: any, b: any) => {
        if (a.isTopTrack && !b.isTopTrack) return -1;
        if (!a.isTopTrack && b.isTopTrack) return 1;
        return 0;
      });
      
      console.log('최종 곡 검색 결과:', filteredTracks);
      setSongResults(filteredTracks);
    } catch (error) {
      console.error('곡 검색 실패:', error);
    } finally {
      setIsSearchingSongs(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMusic && !isEditMode) {
      alert('음악을 선택해주세요');
      return;
    }

    if (isEditMode) {
      // 편집 모드일 때는 PUT 요청
      try {
        const updateData = {
          rating,
          reviewText: reviewText.trim(),
          tags: tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0),
        };

        console.log('=== 리뷰 수정 요청 시작 ===');
        console.log('reviewId:', initialData?.reviewId);
        console.log('updateData:', updateData);
        console.log('URL:', `/api/reviews/${initialData?.reviewId}`);

        const response = await fetch(`/api/reviews/${initialData?.reviewId}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify(updateData),
        });

        console.log('응답 상태:', response.status);
        console.log('응답 OK:', response.ok);

        if (response.ok) {
          const result = await response.json();
          console.log('수정 성공 응답:', result);
          onSuccess?.();
        } else {
          const errorText = await response.text();
          console.error('수정 실패 응답:', errorText);
          alert('리뷰 수정에 실패했습니다.');
        }
      } catch (error) {
        console.error('Edit error:', error);
        alert('리뷰 수정 중 오류가 발생했습니다.');
      }
    } else {
      // 생성 모드일 때는 기존 로직
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

      onSubmit?.(reviewData);
    }
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
      <div className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-50 bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 rounded-2xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto border border-white/20 shadow-2xl">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-white">
            {isEditMode ? '리뷰 수정' : '음악 리뷰 작성'}
          </h2>
          <button
            onClick={onClose || onCancel}
            className="text-blue-200 hover:text-white p-1 rounded-full hover:bg-white/10 transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 편집 모드가 아닐 때만 음악 선택 표시 */}
          {!isEditMode && (
            <>
              {/* 1단계: 아티스트 검색 */}
              <div>
            <label className="block text-blue-200 text-sm font-medium mb-2">
              1단계: 아티스트 검색 *
            </label>
            <div className="relative">
              <input
                type="text"
                value={artistSearch}
                onChange={(e) => {
                  const rawValue = e.target.value;
                  
                  // 음악 검색은 매우 관대하게 처리 - 기본적인 스크립트만 차단
                  if (rawValue.includes('<script') || rawValue.includes('javascript:')) {
                    alert('검색어에 허용되지 않는 코드가 포함되어 있습니다.');
                    return;
                  }
                  
                  const value = rawValue;  // trim() 제거하여 원본 그대로 사용
                  setArtistSearch(value);
                  
                  // 아티스트 검색어가 변경되면 선택된 아티스트와 곡 초기화
                  if (value !== selectedArtist?.name) {
                    setSelectedArtist(null);
                    setSelectedMusic(null);
                    setSongSearch('');
                    setSongResults([]);
                  }
                  
                  searchArtists(value);
                }}
                placeholder="아티스트명을 입력하세요..."
                className="w-full px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              
              {/* 아티스트 검색 결과 */}
              {(artistResults.length > 0 || isSearchingArtists) && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-2xl border border-gray-200 z-[200] max-h-96 overflow-hidden flex flex-col">
                  <div className="flex justify-between items-center p-3 border-b bg-gradient-to-r from-blue-50 to-purple-50">
                    <div>
                      <h3 className="font-semibold text-gray-800 text-sm">아티스트 검색 결과</h3>
                      <p className="text-xs text-gray-600">
                        {isSearchingArtists ? '검색 중...' : `${artistResults.length}개 아티스트`}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setArtistResults([]);
                        setArtistSearch('');
                      }}
                      className="text-gray-400 hover:text-gray-600 p-1 rounded-full"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>

                  <div className="flex-1 overflow-y-auto">
                    {isSearchingArtists && (
                      <div className="p-4 text-center text-sm text-gray-500">아티스트 검색 중...</div>
                    )}

                    {!isSearchingArtists && artistResults.length === 0 && artistSearch.trim() && (
                      <div className="p-4 text-center text-sm text-gray-500">아티스트를 찾을 수 없습니다</div>
                    )}

                    {artistResults.map((artist) => (
                      <div
                        key={artist.id}
                        onClick={() => {
                          console.log('아티스트 선택:', artist.displayName);
                          setSelectedArtist(artist);
                          setArtistSearch(artist.displayName);
                          setArtistResults([]);
                          setSongSearch('');
                          setSelectedMusic(null);
                          // 아티스트 선택 시 자동으로 인기곡 로드
                          searchArtistSongs('');
                        }}
                        className="flex items-center p-3 hover:bg-gradient-to-r hover:from-purple-50 hover:to-blue-50 cursor-pointer border-b border-gray-100 last:border-b-0 transition-colors"
                      >
                        <img
                          src={artist.imageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(artist.displayName)}&backgroundColor=random`}
                          alt={artist.displayName}
                          className="w-12 h-12 rounded-full object-cover mr-3 shadow-md"
                        />
                        <div className="flex-1 min-w-0">
                          <div className="font-semibold text-gray-900 text-sm flex items-center">
                            {artist.displayName}
                            <span className="ml-2 px-2 py-1 text-xs rounded-full bg-purple-100 text-purple-800">
                              아티스트
                            </span>
                          </div>
                          <div className="text-xs text-gray-600">{artist.displaySubtitle}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
            
            {/* 선택된 아티스트 표시 */}
            {selectedArtist && (
              <div className="mt-3 p-4 bg-white/10 backdrop-blur-lg rounded-lg border border-white/20 flex items-center space-x-3">
                <img
                  src={selectedArtist.imageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(selectedArtist.displayName)}&backgroundColor=random`}
                  alt={selectedArtist.displayName}
                  className="w-12 h-12 rounded-full object-cover shadow-md"
                />
                <div className="flex-1">
                  <div className="font-medium text-white flex items-center">
                    {selectedArtist.displayName}
                    <span className="ml-2 px-2 py-1 text-xs rounded-full bg-purple-500/30 text-purple-200">
                      선택된 아티스트
                    </span>
                  </div>
                  <div className="text-sm text-blue-200">
                    {selectedArtist.displaySubtitle}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedArtist(null);
                    setArtistSearch('');
                    setSelectedMusic(null);
                    setSongSearch('');
                    setSongResults([]);
                  }}
                  className="text-red-400 hover:text-red-300 p-1 rounded-full hover:bg-white/10 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            )}
          </div>

          {/* 2단계: 곡 검색 (아티스트 선택 후에만 활성화) */}
          {selectedArtist && (
            <div>
              <label className="block text-blue-200 text-sm font-medium mb-2">
                2단계: 곡 검색 *
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={songSearch}
                  onChange={(e) => {
                    const rawValue = e.target.value;
                    
                    // 곡 검색도 매우 관대하게 처리 - 기본적인 스크립트만 차단
                    if (rawValue.includes('<script') || rawValue.includes('javascript:')) {
                      alert('검색어에 허용되지 않는 코드가 포함되어 있습니다.');
                      return;
                    }
                    
                    const value = rawValue;  // trim() 제거하여 원본 그대로 사용
                    setSongSearch(value);
                    
                    if (value !== selectedMusic?.name) {
                      setSelectedMusic(null);
                    }
                    
                    searchArtistSongs(value);
                  }}
                  placeholder={`${selectedArtist.displayName}의 곡을 검색하세요...`}
                  className="w-full px-4 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
                
                {/* 곡 검색 결과 */}
                {(songResults.length > 0 || isSearchingSongs) && (
                  <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-2xl border border-gray-200 z-[200] max-h-96 overflow-hidden flex flex-col">
                    <div className="flex justify-between items-center p-3 border-b bg-gradient-to-r from-green-50 to-blue-50">
                      <div>
                        <h3 className="font-semibold text-gray-800 text-sm">{selectedArtist.displayName}의 곡</h3>
                        <p className="text-xs text-gray-600">
                          {isSearchingSongs ? '검색 중...' : `${songResults.length}개 곡`}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          setSongResults([]);
                          setSongSearch('');
                        }}
                        className="text-gray-400 hover:text-gray-600 p-1 rounded-full"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>

                    <div className="flex-1 overflow-y-auto">
                      {isSearchingSongs && (
                        <div className="p-4 text-center text-sm text-gray-500">곡 검색 중...</div>
                      )}

                      {!isSearchingSongs && songResults.length === 0 && songSearch.trim() && (
                        <div className="p-4 text-center text-sm text-gray-500">해당 곡을 찾을 수 없습니다</div>
                      )}

                      {songResults.map((track) => (
                        <div
                          key={track.id}
                          onClick={() => {
                            console.log('곡 선택:', track.displayName);
                            setSelectedMusic(track);
                            setSongSearch(track.displayName);
                            setSongResults([]);
                          }}
                          className="flex items-center p-3 hover:bg-gradient-to-r hover:from-green-50 hover:to-blue-50 cursor-pointer border-b border-gray-100 last:border-b-0 transition-colors"
                        >
                          <img
                            src={track.imageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(track.displayName)}&backgroundColor=random`}
                            alt={track.displayName}
                            className="w-12 h-12 rounded-full object-cover mr-3 shadow-md"
                          />
                          <div className="flex-1 min-w-0">
                            <div className="font-semibold text-gray-900 text-sm flex items-center">
                              {track.displayName}
                              <span className="ml-2 px-2 py-1 text-xs rounded-full bg-green-100 text-green-800">
                                곡
                              </span>
                            </div>
                            <div className="text-xs text-gray-600">{track.displaySubtitle}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

              {/* 선택된 곡 표시 */}
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
                      <span className="ml-2 px-2 py-1 text-xs rounded-full bg-green-500/30 text-green-200">
                        선택된 곡
                      </span>
                    </div>
                    <div className="text-sm text-blue-200">
                      {selectedMusic.displaySubtitle}
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedMusic(null);
                      setSongSearch('');
                    }}
                    className="text-red-400 hover:text-red-300 p-1 rounded-full hover:bg-white/10 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              )}
            </>
          )}
          
          {/* 편집 모드에서는 기존 음악 정보 표시 */}
          {isEditMode && initialData && (
            <div className="p-4 bg-white/10 backdrop-blur-lg rounded-lg border border-white/20 flex items-center space-x-3">
              <img
                src={initialData.imageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(initialData.musicName || '')}&backgroundColor=random`}
                alt={initialData.musicName}
                className="w-12 h-12 rounded-full object-cover shadow-md"
              />
              <div className="flex-1">
                <div className="font-medium text-white">
                  {initialData.musicName}
                </div>
                <div className="text-sm text-blue-200">
                  {initialData.artistName}
                </div>
              </div>
            </div>
          )}

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
              onChange={(e) => {
                const rawValue = e.target.value;
                const validation = validateReviewText(rawValue);
                if (!validation.isValid) {
                  alert(validation.error || '리뷰에 허용되지 않는 내용이 포함되어 있습니다');
                  return;
                }
                setReviewText(validation.sanitized);
              }}
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
              onChange={(e) => {
                const rawValue = e.target.value;
                const validation = validateInput(rawValue, '태그');
                if (!validation.isValid) {
                  alert(validation.error || '태그에 허용되지 않는 내용이 포함되어 있습니다');
                  return;
                }
                setTags(validation.sanitized);
              }}
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
              onClick={onClose || onCancel}
              className="px-6 py-2 text-blue-200 border border-white/30 rounded-lg hover:bg-white/10 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={!isEditMode && !selectedMusic}
              className="px-6 py-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg hover:from-blue-700 hover:to-purple-700 disabled:from-gray-600 disabled:to-gray-600 disabled:cursor-not-allowed flex items-center space-x-2 transition-all transform hover:scale-105"
            >
              <Send className="w-4 h-4" />
              <span>{isEditMode ? '수정 완료' : '리뷰 등록'}</span>
            </button>
          </div>
        </form>
      </div>
  );
};

export default ReviewForm;