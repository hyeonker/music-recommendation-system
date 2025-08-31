import React, { useState, useEffect } from 'react';
import { Bell, X, Check, Clock, Award, AlertCircle, Megaphone, Settings, RefreshCw, Wrench, PartyPopper } from 'lucide-react';

interface BadgeNotification {
  id: number;
  userId: number;
  type: 'BADGE_AWARDED' | 'BADGE_EXPIRING' | 'BADGE_EXPIRED' | 'SYSTEM_ANNOUNCEMENT' | 'ADMIN_MESSAGE' | 'UPDATE_NOTIFICATION' | 'MAINTENANCE_NOTICE' | 'EVENT_NOTIFICATION';
  title: string;
  message: string;
  badgeId: number;
  badgeType: string;
  badgeName: string;
  rarity?: string;
  timestamp: string;
  isRead: boolean;
}

interface NotificationCenterProps {
  userId: number;
}

export default function NotificationCenter({ userId }: NotificationCenterProps) {
  const [notifications, setNotifications] = useState<BadgeNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  // 알림 목록 조회
  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const response = await fetch(`/api/badges/notifications?userId=${userId}`, {
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();
        console.log('🔍 받아온 알림 데이터:', data); // 디버깅용
        if (data && Array.isArray(data)) {
          // 백엔드에서 'read' 필드로 오는 것을 'isRead'로 매핑
          const mappedData = data.map(notification => ({
            ...notification,
            isRead: notification.read !== undefined ? notification.read : notification.isRead
          }));
          
          mappedData.forEach((notification, index) => {
            console.log(`알림 ${index}:`, {
              id: notification.id,
              title: notification.title,
              isRead: notification.isRead,
              originalRead: data[index].read
            });
          });
          
          setNotifications(mappedData || []);
        } else {
          setNotifications(data || []);
        }
      }
    } catch (error) {
      console.error('알림 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  // 읽지 않은 알림 개수 조회
  const fetchUnreadCount = async () => {
    try {
      const response = await fetch(`/api/badges/notifications/unread-count?userId=${userId}`, {
        credentials: 'include',
      });
      if (response.ok) {
        const count = await response.json();
        setUnreadCount(count);
      }
    } catch (error) {
      console.error('읽지 않은 알림 개수 조회 실패:', error);
    }
  };

  // 알림 읽음 처리
  const markAsRead = async (notificationId: number) => {
    console.log('markAsRead 호출됨:', notificationId);
    
    if (!notificationId) {
      console.error('알림 ID가 없습니다:', notificationId);
      return;
    }
    
    try {
      const response = await fetch(`/api/badges/notifications/${notificationId}/read`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ userId }),
      });
      
      if (response.ok) {
        setNotifications(prev => 
          prev.map(notification => 
            notification.id === notificationId 
              ? { ...notification, isRead: true }
              : notification
          )
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
      } else {
        console.error('읽음 처리 API 응답 오류:', response.status, response.statusText);
        const errorText = await response.text();
        console.error('오류 내용:', errorText);
      }
    } catch (error) {
      console.error('알림 읽음 처리 실패:', error);
    }
  };

  // 모든 알림 읽음 처리
  const markAllAsRead = async () => {
    try {
      const response = await fetch(`/api/badges/notifications/read-all`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ userId }),
      });
      
      if (response.ok) {
        setNotifications(prev => 
          prev.map(notification => ({ ...notification, isRead: true }))
        );
        setUnreadCount(0);
      }
    } catch (error) {
      console.error('모든 알림 읽음 처리 실패:', error);
    }
  };

  // 알림 삭제
  const deleteNotification = async (notificationId: number) => {
    try {
      // 삭제하기 전에 해당 알림이 읽지 않은 상태인지 확인
      const targetNotification = notifications.find(n => n.id === notificationId);
      const wasUnread = targetNotification && !targetNotification.isRead;
      
      const response = await fetch(`/api/badges/notifications/${notificationId}`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ userId }),
      });
      
      if (response.ok) {
        setNotifications(prev => 
          prev.filter(notification => notification.id !== notificationId)
        );
        
        // 읽지 않은 알림이었다면 카운트도 차감
        if (wasUnread) {
          setUnreadCount(prev => Math.max(0, prev - 1));
        }
      }
    } catch (error) {
      console.error('알림 삭제 실패:', error);
    }
  };

  // 알림 타입별 아이콘
  const getNotificationIcon = (type: BadgeNotification['type']) => {
    switch (type) {
      case 'BADGE_AWARDED':
        return <Award className="w-5 h-5 text-yellow-500" />;
      case 'BADGE_EXPIRING':
        return <Clock className="w-5 h-5 text-orange-500" />;
      case 'BADGE_EXPIRED':
        return <AlertCircle className="w-5 h-5 text-red-500" />;
      case 'SYSTEM_ANNOUNCEMENT':
        return <Megaphone className="w-5 h-5 text-blue-600" />;
      case 'ADMIN_MESSAGE':
        return <Settings className="w-5 h-5 text-purple-600" />;
      case 'UPDATE_NOTIFICATION':
        return <RefreshCw className="w-5 h-5 text-green-600" />;
      case 'MAINTENANCE_NOTICE':
        return <Wrench className="w-5 h-5 text-orange-600" />;
      case 'EVENT_NOTIFICATION':
        return <PartyPopper className="w-5 h-5 text-pink-600" />;
      default:
        return <Bell className="w-5 h-5 text-blue-500" />;
    }
  };

  // 알림 타입별 색상
  const getNotificationColor = (type: BadgeNotification['type']) => {
    switch (type) {
      case 'BADGE_AWARDED':
        return 'border-l-yellow-500 bg-yellow-50';
      case 'BADGE_EXPIRING':
        return 'border-l-orange-500 bg-orange-50';
      case 'BADGE_EXPIRED':
        return 'border-l-red-500 bg-red-50';
      case 'SYSTEM_ANNOUNCEMENT':
        return 'border-l-blue-600 bg-blue-50';
      case 'ADMIN_MESSAGE':
        return 'border-l-purple-600 bg-purple-50';
      case 'UPDATE_NOTIFICATION':
        return 'border-l-green-600 bg-green-50';
      case 'MAINTENANCE_NOTICE':
        return 'border-l-orange-600 bg-orange-50';
      case 'EVENT_NOTIFICATION':
        return 'border-l-pink-600 bg-pink-50';
      default:
        return 'border-l-blue-500 bg-blue-50';
    }
  };

  // 시간 포맷
  const formatTime = (timestamp: string) => {
    const now = new Date();
    const notificationTime = new Date(timestamp);
    const diff = now.getTime() - notificationTime.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days}일 전`;
    if (hours > 0) return `${hours}시간 전`;
    if (minutes > 0) return `${minutes}분 전`;
    return '방금 전';
  };

  // 컴포넌트 마운트 시 알림 조회
  useEffect(() => {
    fetchNotifications();
    fetchUnreadCount();

    // 주기적으로 알림 확인 (30초마다)
    const interval = setInterval(() => {
      if (!isOpen) {
        fetchUnreadCount();
      }
    }, 30000);

    return () => clearInterval(interval);
  }, [userId]);

  // 알림 센터 열릴 때 알림 목록 새로고침
  useEffect(() => {
    if (isOpen) {
      fetchNotifications();
      fetchUnreadCount(); // 읽지 않은 개수도 함께 업데이트
    }
  }, [isOpen]);

  return (
    <div className="relative">
      {/* 알림 버튼 */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`relative p-2 hover:bg-gray-100 rounded-full transition-all duration-300 ${
          unreadCount > 0 
            ? 'text-blue-600 hover:text-blue-700 animate-pulse' 
            : 'text-gray-600 hover:text-gray-900'
        }`}
        title="알림"
      >
        <Bell 
          className={`w-6 h-6 transition-all duration-300 ${
            unreadCount > 0 ? 'animate-bounce' : ''
          }`} 
        />
        {unreadCount > 0 && (
          <>
            {/* 반짝이는 링 효과 */}
            <span className="absolute -top-0.5 -right-0.5 w-6 h-6 bg-red-400 rounded-full animate-ping opacity-75"></span>
            
            {/* 알림 개수 배지 */}
            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center z-10 animate-pulse shadow-lg">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          </>
        )}
      </button>

      {/* 알림 패널 */}
      {isOpen && (
        <>
          <div 
            className="fixed inset-0 z-40" 
            onClick={() => setIsOpen(false)}
          />
          <div className="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-lg border z-50 max-h-96 overflow-hidden">
            {/* 헤더 */}
            <div className="p-4 border-b bg-gray-50">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-gray-900">알림</h3>
                <div className="flex items-center space-x-2">
                  {unreadCount > 0 && (
                    <button
                      onClick={markAllAsRead}
                      className="text-sm text-blue-600 hover:text-blue-800"
                    >
                      모두 읽음
                    </button>
                  )}
                  <button
                    onClick={() => setIsOpen(false)}
                    className="text-gray-400 hover:text-gray-600"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>

            {/* 알림 목록 */}
            <div className="max-h-80 overflow-y-auto">
              {loading ? (
                <div className="p-4 text-center text-gray-500">
                  알림을 불러오는 중...
                </div>
              ) : notifications.length === 0 ? (
                <div className="p-8 text-center text-gray-500">
                  <Bell className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                  <p>새로운 알림이 없습니다</p>
                </div>
              ) : (
                notifications.map((notification) => (
                  <div
                    key={notification.id}
                    className={`relative p-4 border-b border-l-4 ${getNotificationColor(notification.type)} ${
                      !notification.isRead ? 'bg-blue-50 shadow-sm' : 'bg-white'
                    } hover:bg-gray-50 transition-colors`}
                  >
                    {/* NEW 배지 (읽지 않은 알림) */}
                    {!notification.isRead && (
                      <div className="absolute top-2 right-2">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-500 text-white animate-pulse">
                          NEW
                        </span>
                      </div>
                    )}
                    
                    <div className="flex items-start space-x-3">
                      <div className="flex-shrink-0">
                        {getNotificationIcon(notification.type)}
                      </div>
                      
                      <div className="flex-1 min-w-0 pr-12">
                        <div className="flex items-center justify-between">
                          <p className={`text-sm font-medium ${!notification.isRead ? 'text-blue-900' : 'text-gray-900'}`}>
                            {notification.title}
                          </p>
                          {!notification.isRead && (
                            <span className="w-3 h-3 bg-blue-500 rounded-full ring-2 ring-blue-200 animate-pulse ml-2"></span>
                          )}
                        </div>
                        
                        <p className="text-sm text-gray-600 mt-1">
                          {notification.message}
                        </p>
                        
                        <div className="flex items-center justify-between mt-2">
                          <span className="text-xs text-gray-500">
                            {formatTime(notification.timestamp)}
                          </span>
                          
                          <div className="flex items-center space-x-2">
                            {!notification.isRead && (
                              <button
                                onClick={() => markAsRead(notification.id)}
                                className="text-xs text-blue-600 hover:text-blue-800"
                                title="읽음 처리"
                              >
                                <Check className="w-4 h-4" />
                              </button>
                            )}
                            <button
                              onClick={() => deleteNotification(notification.id)}
                              className="text-xs text-red-600 hover:text-red-800"
                              title="삭제"
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}