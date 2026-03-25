export interface Doctor {
  id: number;
  firstName: string;
  lastName: string;
  speciality: string;
  email?: string;
  username?: string;
  licenseNumber?: string;
  department?: string;
  experienceYears?: number;
  enabled?: boolean;
}
