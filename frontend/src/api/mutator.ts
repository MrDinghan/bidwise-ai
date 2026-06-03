import axios, { type AxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/auth/store';

/**
 * Shared axios instance used by every Orval-generated hook. Injects the bearer
 * token and clears it on 401 so the app can redirect to login.
 */
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/',
});

instance.interceptors.request.use((config) => {
  const { token } = useAuthStore.getState();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  },
);

export const customInstance = <T>(config: AxiosRequestConfig): Promise<T> => {
  return instance.request<unknown, T>(config);
};

export default customInstance;
