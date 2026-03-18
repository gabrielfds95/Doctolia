//Définit quelles URL affichent quels composants.
//Angular regarde l’URL et affiche le composant correspondant dans <router-outlet>

import { Routes } from '@angular/router';
import { DoctorListComponent } from './component/doctor/doctor-list.component';
import { SlotListComponent } from './component/slot/slot-list.component';

export const routes: Routes = [
  { path: '', component: DoctorListComponent },             // page d'accueil : liste des médecins
  { path: 'doctor-slots/:id', component: SlotListComponent } // créneaux d’un médecin
];
