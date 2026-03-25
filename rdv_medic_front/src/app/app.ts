import { Component } from '@angular/core';
import { RouterOutlet, RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule, CommonModule],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  constructor(public authService: AuthService, private router: Router) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isPatient(): boolean { return this.authService.isPatient(); }
  isDoctor(): boolean  { return this.authService.isDoctor(); }
  isAdmin(): boolean   { return this.authService.isAdmin(); }

  initials(): string {
    const u = this.authService.currentUser;
    if (!u) return '';
    return `${u.firstName?.[0] ?? ''}${u.lastName?.[0] ?? ''}`.toUpperCase();
  }

  roleLabel(): string {
    if (this.isAdmin())   return 'Admin';
    if (this.isDoctor())  return 'Médecin';
    return 'Patient';
  }
}
