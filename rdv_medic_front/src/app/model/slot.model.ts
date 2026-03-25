import { Doctor } from './doctor.model';
import { Patient } from './patient.model';

export interface Slot {
  id: number;
  slotDate: string;  // Angular récupère les dates du backend en string ISO
  slotTime: string;  // Pareil pour l'heure
  endTime: string;
  slotReason: string;
  status: string | null;  // AVAILABLE | RESERVED | CANCELLED | COMPLETED
  doctor: Doctor;    // Tu peux garder l'objet complet si tu veux afficher nom, spécialité, etc.
  patient?: Patient; // facultatif, peut être null si le créneau est libre
}