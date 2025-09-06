import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { motion } from 'framer-motion';
import { Mail, Lock, User, Eye, EyeOff, ArrowLeft, Check, X } from 'lucide-react';

interface SignupForm {
  email: string;
  name: string;
  password: string;
  confirmPassword: string;
}

interface ValidationErrors {
  email?: string;
  name?: string;
  password?: string;
  confirmPassword?: string;
}

const Signup: React.FC = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState<SignupForm>({
    email: '',
    name: '',
    password: '',
    confirmPassword: ''
  });
  
  const [errors, setErrors] = useState<ValidationErrors>({});
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailAvailable, setEmailAvailable] = useState(false);

  // 실시간 유효성 검사
  const validateField = (name: string, value: string) => {
    const newErrors = { ...errors };

    switch (name) {
      case 'email':
        if (!value) {
          newErrors.email = '이메일을 입력해주세요.';
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          newErrors.email = '유효한 이메일 주소를 입력해주세요.';
        } else {
          delete newErrors.email;
        }
        setEmailChecked(false);
        setEmailAvailable(false);
        break;

      case 'name':
        if (!value) {
          newErrors.name = '이름을 입력해주세요.';
        } else if (value.length < 2) {
          newErrors.name = '이름은 2자 이상이어야 합니다.';
        } else if (value.length > 50) {
          newErrors.name = '이름은 50자 이하여야 합니다.';
        } else {
          delete newErrors.name;
        }
        break;

      case 'password':
        if (!value) {
          newErrors.password = '비밀번호를 입력해주세요.';
        } else if (value.length < 6) {
          newErrors.password = '비밀번호는 6자 이상이어야 합니다.';
        } else if (!/(?=.*[a-zA-Z])(?=.*\d)/.test(value)) {
          newErrors.password = '비밀번호는 영문자와 숫자를 모두 포함해야 합니다.';
        } else {
          delete newErrors.password;
        }
        
        // 비밀번호가 변경되면 확인 비밀번호도 재검증
        if (form.confirmPassword && value !== form.confirmPassword) {
          newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
        } else if (form.confirmPassword && value === form.confirmPassword) {
          delete newErrors.confirmPassword;
        }
        break;

      case 'confirmPassword':
        if (!value) {
          newErrors.confirmPassword = '비밀번호 확인을 입력해주세요.';
        } else if (value !== form.password) {
          newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
        } else {
          delete newErrors.confirmPassword;
        }
        break;
    }

    setErrors(newErrors);
  };

  // 입력 변경 처리
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    validateField(name, value);
  };

  // 이메일 중복 확인
  const checkEmailAvailability = async () => {
    if (!form.email || errors.email) {
      toast.error('유효한 이메일을 입력해주세요.');
      return;
    }

    try {
      const response = await fetch(`/api/auth/local/check-email?email=${encodeURIComponent(form.email)}`);
      const exists = await response.json();
      
      setEmailChecked(true);
      setEmailAvailable(!exists);
      
      if (exists) {
        toast.error('이미 사용 중인 이메일입니다.');
        setErrors(prev => ({ ...prev, email: '이미 사용 중인 이메일입니다.' }));
      } else {
        toast.success('사용 가능한 이메일입니다.');
        setErrors(prev => {
          const newErrors = { ...prev };
          delete newErrors.email;
          return newErrors;
        });
      }
    } catch (error) {
      toast.error('이메일 확인 중 오류가 발생했습니다.');
    }
  };

  // 회원가입 처리
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // 최종 유효성 검사
    const finalErrors: ValidationErrors = {};
    
    if (!form.email) finalErrors.email = '이메일을 입력해주세요.';
    if (!form.name) finalErrors.name = '이름을 입력해주세요.';
    if (!form.password) finalErrors.password = '비밀번호를 입력해주세요.';
    if (!form.confirmPassword) finalErrors.confirmPassword = '비밀번호 확인을 입력해주세요.';
    if (form.password !== form.confirmPassword) finalErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    
    if (Object.keys(finalErrors).length > 0) {
      setErrors(finalErrors);
      return;
    }

    if (!emailChecked || !emailAvailable) {
      toast.error('이메일 중복 확인을 해주세요.');
      return;
    }

    setIsLoading(true);

    try {
      const response = await fetch('/api/auth/local/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: form.email,
          name: form.name,
          password: form.password,
          confirmPassword: form.confirmPassword
        }),
      });

      const data = await response.json();

      if (data.success) {
        toast.success('회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.');
        setTimeout(() => {
          navigate('/login');
        }, 1500);
      } else {
        toast.error(data.message || '회원가입에 실패했습니다.');
      }
    } catch (error) {
      toast.error('회원가입 처리 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const isFormValid = () => {
    return Object.keys(errors).length === 0 && 
           form.email && form.name && form.password && form.confirmPassword &&
           emailChecked && emailAvailable;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50 to-blue-50 dark:from-gray-900 dark:to-purple-900 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="max-w-md w-full space-y-8"
      >
        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
          {/* 헤더 */}
          <div className="text-center mb-8">
            <Link 
              to="/login"
              className="inline-flex items-center text-sm text-gray-500 dark:text-gray-400 hover:text-purple-600 dark:hover:text-purple-400 mb-4 transition-colors"
            >
              <ArrowLeft className="w-4 h-4 mr-1" />
              로그인으로 돌아가기
            </Link>
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
              회원가입
            </h2>
            <p className="text-gray-600 dark:text-gray-400">
              MusicMatch에서 음악으로 사람들과 연결되세요
            </p>
          </div>

          {/* 회원가입 폼 */}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 이메일 입력 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                이메일 주소
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type="email"
                  name="email"
                  value={form.email}
                  onChange={handleChange}
                  className={`block w-full pl-10 pr-12 py-3 border rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 transition-colors ${
                    errors.email 
                      ? 'border-red-300 focus:ring-red-500' 
                      : emailChecked && emailAvailable
                      ? 'border-green-300 focus:ring-green-500'
                      : 'border-gray-300 dark:border-gray-600 focus:ring-purple-500'
                  }`}
                  placeholder="your@email.com"
                />
                {emailChecked && (
                  <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                    {emailAvailable ? (
                      <Check className="h-5 w-5 text-green-500" />
                    ) : (
                      <X className="h-5 w-5 text-red-500" />
                    )}
                  </div>
                )}
              </div>
              {errors.email && (
                <p className="mt-1 text-sm text-red-600">{errors.email}</p>
              )}
              <button
                type="button"
                onClick={checkEmailAvailability}
                disabled={!form.email || !!errors.email}
                className="mt-2 text-sm text-purple-600 dark:text-purple-400 hover:text-purple-800 dark:hover:text-purple-300 disabled:text-gray-400 transition-colors"
              >
                이메일 중복 확인
              </button>
            </div>

            {/* 이름 입력 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                이름
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type="text"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  className={`block w-full pl-10 py-3 border rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 transition-colors ${
                    errors.name 
                      ? 'border-red-300 focus:ring-red-500' 
                      : 'border-gray-300 dark:border-gray-600 focus:ring-purple-500'
                  }`}
                  placeholder="이름을 입력하세요"
                />
              </div>
              {errors.name && (
                <p className="mt-1 text-sm text-red-600">{errors.name}</p>
              )}
            </div>

            {/* 비밀번호 입력 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                비밀번호
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  className={`block w-full pl-10 pr-10 py-3 border rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 transition-colors ${
                    errors.password 
                      ? 'border-red-300 focus:ring-red-500' 
                      : 'border-gray-300 dark:border-gray-600 focus:ring-purple-500'
                  }`}
                  placeholder="비밀번호 (영문자+숫자, 6자 이상)"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1 text-sm text-red-600">{errors.password}</p>
              )}
            </div>

            {/* 비밀번호 확인 입력 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                비밀번호 확인
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  className={`block w-full pl-10 pr-10 py-3 border rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 transition-colors ${
                    errors.confirmPassword 
                      ? 'border-red-300 focus:ring-red-500' 
                      : form.confirmPassword && form.password === form.confirmPassword
                      ? 'border-green-300 focus:ring-green-500'
                      : 'border-gray-300 dark:border-gray-600 focus:ring-purple-500'
                  }`}
                  placeholder="비밀번호를 다시 입력하세요"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                >
                  {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="mt-1 text-sm text-red-600">{errors.confirmPassword}</p>
              )}
              {form.confirmPassword && form.password === form.confirmPassword && !errors.confirmPassword && (
                <p className="mt-1 text-sm text-green-600 flex items-center">
                  <Check className="w-4 h-4 mr-1" />
                  비밀번호가 일치합니다
                </p>
              )}
            </div>

            {/* 회원가입 버튼 */}
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              type="submit"
              disabled={!isFormValid() || isLoading}
              className={`w-full py-3 px-4 rounded-lg font-medium transition-all duration-200 ${
                isFormValid() && !isLoading
                  ? 'bg-purple-600 hover:bg-purple-700 text-white shadow-lg hover:shadow-xl'
                  : 'bg-gray-300 dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed'
              }`}
            >
              {isLoading ? (
                <div className="flex items-center justify-center">
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></div>
                  회원가입 중...
                </div>
              ) : (
                '회원가입'
              )}
            </motion.button>
          </form>

          {/* 로그인 링크 */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              이미 계정이 있나요?{' '}
              <Link 
                to="/login" 
                className="font-medium text-purple-600 dark:text-purple-400 hover:text-purple-500 dark:hover:text-purple-300 transition-colors"
              >
                로그인하기
              </Link>
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default Signup;