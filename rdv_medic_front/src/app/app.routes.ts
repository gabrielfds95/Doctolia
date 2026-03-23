import { Routes } from '@angular/router';
import { DoctorListComponent } from './component/doctor/doctor-list.component';
import { SlotListComponent } from './component/slot/slot-list.component';
import { LoginComponent } from './component/login/login.component';
import { RegisterComponent } from './component/register/register.component';
import { NotFoundComponent } from './component/not-found/not-found.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: '', component: DoctorListComponent, canActivate: [authGuard] },
  { path: 'doctor-slots/:id', component: SlotListComponent, canActivate: [authGuard] },
  { path: '**', component: NotFoundComponent }
];
