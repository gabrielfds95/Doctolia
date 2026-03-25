import { Routes } from '@angular/router';
import { DoctorListComponent } from './component/doctor/doctor-list.component';
import { SlotListComponent } from './component/slot/slot-list.component';
import { LoginComponent } from './component/login/login.component';
import { RegisterComponent } from './component/register/register.component';
import { NotFoundComponent } from './component/not-found/not-found.component';
import { MesRdvComponent } from './component/mes-rdv/mes-rdv.component';
import { MonPlanningComponent } from './component/mon-planning/mon-planning.component';
import { ProfileComponent } from './component/profile/profile.component';
import { AdminDashboardComponent } from './component/admin-dashboard/admin-dashboard.component';
import { MessagerieComponent } from './component/messagerie/messagerie.component';
import { AssistantIaComponent } from './component/assistant-ia/assistant-ia.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: '', component: DoctorListComponent, canActivate: [authGuard] },
  { path: 'doctor-slots/:id', component: SlotListComponent, canActivate: [authGuard] },
  { path: 'mes-rdv', component: MesRdvComponent, canActivate: [authGuard] },
  { path: 'mon-planning', component: MonPlanningComponent, canActivate: [authGuard] },
  { path: 'messages', component: MessagerieComponent, canActivate: [authGuard] },
  { path: 'assistant', component: AssistantIaComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [adminGuard] },
  { path: '**', component: NotFoundComponent }
];
