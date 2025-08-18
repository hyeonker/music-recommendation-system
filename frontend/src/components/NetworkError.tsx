import React from 'react';
import { Wifi, RefreshCw, AlertCircle } from 'lucide-react';

interface NetworkErrorProps {
    onRetry: () => void;
    message?: string;
    isRetrying?: boolean;
}

const NetworkError: React.FC<NetworkErrorProps> = ({
                                                       onRetry,
                                                       message = "서버에 연결할 수 없습니다",
                                                       isRetrying = false
                                                   }) => {
    return (
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-red-500/30 text-center">
            <div className="flex justify-center mb-4">
                {isRetrying ? (
                    <RefreshCw className="w-12 h-12 text-yellow-400 animate-spin" />
                ) : (
                    <div className="relative">
                        <Wifi className="w-12 h-12 text-red-400" />
                        <AlertCircle className="w-6 h-6 text-red-500 absolute -bottom-1 -right-1" />
                    </div>
                )}
            </div>

            <h3 className="text-xl font-bold text-white mb-2">
                {isRetrying ? "재연결 중..." : "연결 오류"}
            </h3>

            <p className="text-red-200 mb-4">{message}</p>

            {!isRetrying && (
                <button
                    onClick={onRetry}
                    className="bg-red-600 hover:bg-red-700 text-white px-6 py-2 rounded-lg transition-colors flex items-center space-x-2 mx-auto"
                >
                    <RefreshCw className="w-4 h-4" />
                    <span>다시 시도</span>
                </button>
            )}
        </div>
    );
};

export default NetworkError;