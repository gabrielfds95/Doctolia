export type SlotStatus = 'AVAILABLE' | 'BOOKED' | 'CANCELLED';

export interface AuthResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  token: string;
  expiresIn: number;
  roles: string[];
}

export interface Doctor {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  speciality: string;
  department: string;
  licenseNumber: string;
  experienceYears: number;
}

export interface PatientInfo {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface Slot {
  id: number;
  slotDate: string;    // "2025-12-15"
  slotTime: string;    // "09:00:00"
  endTime: string;     // "09:30:00"
  slotReason: string;
  status: SlotStatus;
  doctor: Doctor;
  patient: PatientInfo | null;
}
