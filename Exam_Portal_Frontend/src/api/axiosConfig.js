import axios from "axios";
import logger from "../utils/logger";


const axiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || "/api",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

axiosInstance.interceptors.request.use(
  (config) => {
    logger.info(
      `API Request: ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`
    );
    return config;
  },
  (error) => {
    logger.error("API Request Error", error);
    return Promise.reject(error);
  }
);

axiosInstance.interceptors.response.use(
  (response) => {
    logger.info(
      `API Response: ${response.status} ${response.config.method?.toUpperCase()} ${response.config.baseURL}${response.config.url}`
    );
    return response;
  },
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url;
    const method = error.config?.method?.toUpperCase();

    if (status >= 500) {
      logger.error(`API Server Error: ${status} ${method} ${url}`, error);
    } else if (status >= 400) {
      logger.warn(`API Client Error: ${status} ${method} ${url}`, error);
    } else {
      logger.error(`API Network Error: ${method} ${url}`, error);
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
