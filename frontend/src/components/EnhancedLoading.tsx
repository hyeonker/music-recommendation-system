import React from 'react';
import { Music, Loader2 } from 'lucide-react';

interface EnhancedLoadingProps {
    message?: string;
    submessage?: string;
    variant?: 'full' | 'inline' | 'minimal';
}

const EnhancedLoading: React.FC<EnhancedLoadingProps> = ({
                                                             message = "로딩 중...",
                                                             submessage,
                                                             variant = 'full'
                                                         }) => {
    if (variant === 'minimal') {
        return (
            <div className="flex items-center space-x-2 text-blue-400">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span className="text-sm">{message}</span>
            </div>
        );
    }

    if (variant === 'inline') {
        return (
            <div className="bg-white/10 backdrop-blur-lg rounded-xl p-4 border border-white/20 text-center">
                <div className="flex items-center justify-center space-x-3">
                    <div className="relative">
                        <Music className="w-8 h-8 text-purple-400 animate-pulse" />
                        <Loader2 className="w-4 h-4 text-blue-400 animate-spin absolute -bottom-1 -right-1" />
                    </div>
                    <div className="text-left">
                        <p className="text-white font-medium">{message}</p>
                        {submessage && <p className="text-blue-200 text-sm">{submessage}</p>}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex items-center justify-center">
            <div className="text-center">
                {/* 애니메이션 로딩 아이콘 */}
                <div className="relative mb-8">
                    <div className="w-20 h-20 border-4 border-blue-200 rounded-full animate-pulse opacity-25"></div>
                    <div className="absolute inset-2 border-4 border-purple-500 rounded-full animate-spin border-t-transparent"></div>
                    <Music className="absolute inset-0 m-auto w-8 h-8 text-white animate-bounce" />
                </div>

                <h2 className="text-2xl font-bold text-white mb-2">{message}</h2>

                {submessage && (
                    <p className="text-blue-200 mb-4">{submessage}</p>
                )}

                {/* 진행 표시 점들 */}
                <div className="flex justify-center space-x-2">
                    <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                    <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                    <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                </div>
            </div>
        </div>
    );
};

export default EnhancedLoading;