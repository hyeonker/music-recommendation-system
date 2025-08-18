import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

class ErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
        error: null
    };

    public static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error('ErrorBoundary caught an error:', error, errorInfo);
    }

    private handleReload = () => {
        window.location.reload();
    };

    private handleReset = () => {
        this.setState({ hasError: false, error: null });
    };

    public render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex items-center justify-center">
                    <div className="max-w-md mx-auto text-center p-8">
                        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-8 border border-white/20">
                            <AlertTriangle className="w-16 h-16 text-red-400 mx-auto mb-4" />
                            <h2 className="text-2xl font-bold text-white mb-4">
                                앗, 문제가 발생했습니다
                            </h2>
                            <p className="text-blue-200 mb-6">
                                예상치 못한 오류가 발생했습니다. 페이지를 새로고침하거나 다시 시도해주세요.
                            </p>

                            {this.state.error && (
                                <div className="bg-red-900/20 border border-red-500/30 rounded-lg p-4 mb-6">
                                    <p className="text-red-300 text-sm font-mono">
                                        {this.state.error.message}
                                    </p>
                                </div>
                            )}

                            <div className="space-x-4">
                                <button
                                    onClick={this.handleReset}
                                    className="bg-purple-600 hover:bg-purple-700 text-white px-6 py-2 rounded-lg transition-colors"
                                >
                                    다시 시도
                                </button>
                                <button
                                    onClick={this.handleReload}
                                    className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition-colors flex items-center space-x-2"
                                >
                                    <RefreshCw className="w-4 h-4" />
                                    <span>새로고침</span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;