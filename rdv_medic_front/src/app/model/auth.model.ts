export interface AuthResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  token: string;
  expiresIn: number;
  roles: string[];
  enabled?: boolean;
}
