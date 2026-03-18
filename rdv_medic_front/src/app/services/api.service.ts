import { Injectable } from '@angular/core'; // Permet de marquer la classe comme injectable
import { HttpClient } from '@angular/common/http'; // Permet de faire des requêtes HTTP
import { Observable } from 'rxjs'; // Permet de gérer les flux de données asynchrones
import {Doctor} from '../model/doctor.model';
import {Slot} from '../model/slot.model';
import {Patient} from '../model/patient.model';

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
  patientId: number;
  slotDate: string;
  slotTime: string;
  slotReason: string;
}): Observable<Slot> {
  return this.http.post<Slot>(`${this.baseURL}/slot/${slotData.doctorId}/${slotData.patientId}`, slotData);
}



postPatient(patientData: {
  firstName: string;
  lastName: string;
  email: string;
  age: number;
}): Observable<Patient> {
  return this.http.post<Patient>(`${this.baseURL}/patient`, patientData);



}

}