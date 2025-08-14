import React from 'react';

const LoadingSpinner = () => {
    return (
        <div className="min-h-screen bg-gradient-dark flex items-center justify-center">
            <div className="text-center">
                {/* 로딩 스피너 */}
                <div className="relative mb-8">
                    <div className="w-20 h-20 border-4 border-blue-200 rounded-full animate-spin border-t-blue-500 mx-auto"></div>
                    <div className="w-16 h-16 border-4 border-purple-200 rounded-full animate-spin border-t-purple-500 absolute top-2 left-1/2 transform -translate-x-1/2 animate-pulse"></div>
                </div>

                {/* 로딩 텍스트 */}
                <div className="text-white text-center">
                    <h1 className="text-3xl font-bold mb-4 animate-fade-in">
                        🎵 Music Matching
                    </h1>
                    <p className="text-blue-300 text-lg animate-pulse">
                        음악으로 연결되는 사람들...
                    </p>

                    {/* 로딩 바 */}
                    <div className="mt-6 w-64 bg-gray-700 rounded-full h-2 mx-auto overflow-hidden">
                        <div className="h-full bg-gradient-to-r from-blue-500 to-purple-500 rounded-full animate-pulse"></div>
                    </div>

                    {/* 로딩 메시지들 */}
                    <div className="mt-4 text-sm text-gray-400">
                        <p className="animate-bounce-gentle">시스템 초기화 중...</p>
                    </div>
                </div>

                {/* 음악 노트 애니메이션 */}
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                    <div className="absolute top-1/4 left-1/4 text-blue-500 text-2xl animate-bounce-slow">♪</div>
                    <div className="absolute top-1/3 right-1/4 text-purple-500 text-xl animate-bounce delay-300">♫</div>
                    <div className="absolute bottom-1/3 left-1/3 text-pink-500 text-lg animate-bounce delay-500">♪</div>
                    <div className="absolute bottom-1/4 right-1/3 text-indigo-500 text-xl animate-bounce delay-700">♫</div>
                </div>
            </div>
        </div>
    );
};

export default LoadingSpinner;