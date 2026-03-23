import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

// iOS simulator  → localhost
// Android emulator → 10.0.2.2
// Appareil physique → adresse IP de votre machine
const BASE_URL = 'http://10.0.2.2:9000'; // Android emulator → ta machine

const axiosInstance = axios.create({ baseURL: BASE_URL });

// Intercepteur de requête : injecte le JWT dans chaque appel
axiosInstance.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default axiosInstance;
