import axios from "axios";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");

      if (refreshToken) {
        try {
          const response = await axios.post(
            (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api") + "/auth/refresh",
            { refreshToken }
          );

          const { token, refreshToken: newRefreshToken, user } = response.data;

          localStorage.setItem("token", token);
          localStorage.setItem("refreshToken", newRefreshToken);

          apiClient.defaults.headers.common["Authorization"] = `Bearer ${token}`;
          originalRequest.headers["Authorization"] = `Bearer ${token}`;

          return apiClient(originalRequest);
        } catch (refreshError) {
          localStorage.removeItem("token");
          localStorage.removeItem("refreshToken");
          localStorage.removeItem("user");
          window.location.href = "/login";
          return Promise.reject(refreshError);
        }
      } else {
        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
