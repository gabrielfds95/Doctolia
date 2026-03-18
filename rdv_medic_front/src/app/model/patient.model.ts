import { EmailValidator } from "@angular/forms";

export interface Patient {
  id: number;           // Identifiant du patient
  firstName: string;          // Nom de famille
  lastName: string;       // Prénom
  age: number; 
  email : EmailValidator ;  
}