import React, { useEffect, useState } from 'react';
import { AlertTriangle } from 'lucide-react';

interface ProtectedRouteProps {
  children: React.ReactNode;
  adminOnly?: boolean;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, adminOnly = false }) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        // 인증 상태 확인
        const authResponse = await fetch('/api/auth/me', {
          credentials: 'include'
        });
        
        if (authResponse.ok) {
          const authData = await authResponse.json();
          console.log('인증 응답 데이터:', authData);
          
          if (authData.authenticated === true) {
            setIsAuthenticated(true);
            
            // 관리자 권한이 필요한 경우 추가 확인
            if (adminOnly) {
            try {
              const adminResponse = await fetch('/api/chat-reports/admin/statistics', {
                credentials: 'include'
              });
              
              if (adminResponse.status === 403) {
                setIsAdmin(false);
              } else if (adminResponse.ok) {
                setIsAdmin(true);
              } else {
                setIsAdmin(false);
              }
            } catch (error) {
              console.error('관리자 권한 확인 실패:', error);
              setIsAdmin(false);
            }
          } else {
            setIsAuthenticated(false);
            setIsAdmin(false);
          }
          } else {
            setIsAuthenticated(false);
            setIsAdmin(false);
          }
        } else {
          setIsAuthenticated(false);
          setIsAdmin(false);
        }
      } catch (error) {
        console.error('인증 확인 실패:', error);
        setIsAuthenticated(false);
        setIsAdmin(false);
      } finally {
        setLoading(false);
      }
    };

    checkAuth();
  }, [adminOnly]);

  // 로딩 중
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
          <p className="mt-4 text-gray-600">권한을 확인하는 중...</p>
        </div>
      </div>
    );
  }

  // 인증되지 않은 경우
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-red-900 via-red-800 to-red-900 flex items-center justify-center">
        <div className="text-center text-white">
          <AlertTriangle className="w-16 h-16 mx-auto mb-4 text-red-400" />
          <h2 className="text-2xl font-bold mb-2">로그인이 필요합니다</h2>
          <p className="text-lg mb-6">이 페이지에 접근하려면 로그인이 필요합니다.</p>
          <button
            onClick={() => window.location.href = '/'}
            className="px-6 py-3 bg-red-600 hover:bg-red-700 rounded-lg font-medium transition-colors"
          >
            홈으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  // 관리자 권한이 필요하지만 권한이 없는 경우
  if (adminOnly && !isAdmin) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex items-center justify-center">
        <div className="text-center text-white">
          <AlertTriangle className="w-16 h-16 mx-auto mb-4 text-red-400" />
          <h2 className="text-2xl font-bold mb-2">접근 권한 없음</h2>
          <p className="text-lg mb-6">이 페이지는 관리자만 접근할 수 있습니다.</p>
          <button
            onClick={() => window.location.href = '/'}
            className="px-6 py-3 bg-purple-600 hover:bg-purple-700 rounded-lg font-medium transition-colors"
          >
            홈으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  // 모든 권한 확인이 통과된 경우 자식 컴포넌트 렌더링
  return <>{children}</>;
};

export default ProtectedRoute;