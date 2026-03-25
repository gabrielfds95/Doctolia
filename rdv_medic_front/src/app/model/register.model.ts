export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  userType: 'PATIENT' | 'DOCTOR';
  ssn?: string;
  phoneNumber?: string;
  address?: string;
  age?: number;
  speciality?: string;
  licenseNumber?: string;
  department?: string;
  experienceYears?: number;
}
