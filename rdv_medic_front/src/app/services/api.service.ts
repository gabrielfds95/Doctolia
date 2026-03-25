import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Doctor } from '../model/doctor.model';
import { Slot } from '../model/slot.model';
import { UserProfile } from '../model/user-profile.model';

@Injectable({
  providedIn: 'root' // Rend le service disponible partout dans l'application
})
export class ApiService {

  constructor(private http: HttpClient) {} // Injection du client HTTP

  baseURL = 'http://localhost:9000';

  // Méthode GET pour récupérer la liste des médecins
  getDoctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(this.baseURL +'/doctors');
  }

  getSlotsByDoctors(idDoctor: number): Observable<Slot[]>{
    return this.http.get<Slot[]>(this.baseURL +`/doctors/${idDoctor}/slots`);
  }

getSlotsByDoctorsAndPatient(idDoctor: number, idPatient: number): Observable<Slot[]> {
  return this.http.get<Slot[]>(this.baseURL +`/slots/${idDoctor}/${idPatient}`);
}

getDoctorById(idDoctor: number): Observable<Doctor> {
  return this.http.get<Doctor>(this.baseURL +`/doctor/${idDoctor}`);
}


postNewSlot(slotData: {
  doctorId: number;
  slotDate: string;
  slotTime: string;
  endTime: string;
  slotReason: string;
}): Observable<Slot> {
  return this.http.post<Slot>(`${this.baseURL}/slot/${slotData.doctorId}`, slotData);
}

// ── Patient ──────────────────────────────────────────────────────────────────

getMySlots(): Observable<Slot[]> {
  return this.http.get<Slot[]>(`${this.baseURL}/patients/me/slots`);
}

cancelSlot(slotId: number): Observable<Slot> {
  return this.http.patch<Slot>(`${this.baseURL}/slots/${slotId}/cancel`, {});
}

// ── Doctor ───────────────────────────────────────────────────────────────────

getDoctorMySlots(): Observable<Slot[]> {
  return this.http.get<Slot[]>(`${this.baseURL}/doctors/me/slots`);
}

createUnavailability(slot: {
  slotDate: string;
  slotTime: string;
  endTime: string;
  slotReason: string;
}): Observable<Slot> {
  return this.http.post<Slot>(`${this.baseURL}/doctors/me/slots`, slot);
}

deleteUnavailability(slotId: number): Observable<void> {
  return this.http.delete<void>(`${this.baseURL}/slot/${slotId}`);
}

updateSlotReason(slotId: number, slotReason: string): Observable<Slot> {
  return this.http.patch<Slot>(`${this.baseURL}/slots/${slotId}`, { slotReason });
}

completeSlot(slotId: number): Observable<Slot> {
  return this.http.put<Slot>(`${this.baseURL}/slots/${slotId}/complete`, {});
}

// ── Profil ────────────────────────────────────────────────────────────────────

getProfile(): Observable<UserProfile> {
  return this.http.get<UserProfile>(`${this.baseURL}/users/me`);
}

updateProfile(data: Partial<UserProfile>): Observable<UserProfile> {
  return this.http.patch<UserProfile>(`${this.baseURL}/users/me`, data);
}

// ── Admin ─────────────────────────────────────────────────────────────────────

getPendingDoctors(): Observable<Doctor[]> {
  return this.http.get<Doctor[]>(`${this.baseURL}/admin/doctors/pending`);
}

approveDoctor(id: number): Observable<Doctor> {
  return this.http.put<Doctor>(`${this.baseURL}/admin/doctors/${id}/approve`, {});
}

rejectDoctor(id: number): Observable<void> {
  return this.http.delete<void>(`${this.baseURL}/admin/doctors/${id}/reject`);
}

}
