export interface UserProfile {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  // Patient
  phoneNumber?: string;
  address?: string;
  age?: number;
  // Doctor
  speciality?: string;
  department?: string;
  experienceYears?: number;
}
