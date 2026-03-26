import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * Instance Axios configurée pour l'application mobile Doctolia.
 *
 * Rôle : centraliser la configuration HTTP et injecter automatiquement
 * le JWT dans chaque requête, exactement comme authInterceptor côté Angular.
 *
 * ── Choix de l'URL selon l'environnement ────────────────────────────────────
 *
 *  iOS Simulator    : localhost fonctionne directement car le simulateur
 *                     partage la stack réseau de la machine hôte.
 *                     → BASE_URL = 'http://localhost:9000'
 *
 *  Android Emulator : "localhost" pointe vers l'émulateur LUI-MÊME, pas la machine.
 *                     L'adresse spéciale 10.0.2.2 redirige vers la machine hôte.
 *                     → BASE_URL = 'http://10.0.2.2:9000'
 *
 *  Appareil physique : utiliser l'IP locale de la machine sur le réseau Wi-Fi.
 *                     → BASE_URL = 'http://192.168.x.x:9000'
 */
const BASE_URL = 'http://10.0.2.2:9000'; // ← Android emulator (changer selon l'environnement)

/**
 * Création de l'instance Axios avec la base URL.
 * Toutes les requêtes faites via axiosInstance utilisent automatiquement cette URL.
 * Exemple : axiosInstance.get('/doctors') → GET http://10.0.2.2:9000/doctors
 */
const axiosInstance = axios.create({ baseURL: BASE_URL });

/**
 * Intercepteur de requête : injecte le JWT dans chaque appel HTTP sortant.
 *
 * Différence avec Angular :
 *  - Angular : localStorage (synchrone, navigateur web)
 *  - React Native : AsyncStorage (ASYNCHRONE, stockage natif chiffré)
 *    → obligatoirement async/await car AsyncStorage retourne une Promise
 *
 * Même principe qu'authInterceptor.ts côté Angular :
 * le composant ou le hook appelle axiosInstance.get('/doctors') sans penser au token,
 * et l'intercepteur l'ajoute automatiquement.
 *
 * Format du header injecté : "Authorization: Bearer eyJhbGciOi..."
 * Reconnu par JwtAuthenticationFilter côté Spring Boot.
 */
axiosInstance.interceptors.request.use(async (config) => {
  // Lecture asynchrone du token depuis le stockage natif sécurisé
  const token = await AsyncStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config; // retourne la config modifiée → Axios envoie la requête
});

export default axiosInstance;
