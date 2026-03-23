import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {

  username = '';
  password = '';
  errors: Record<string, string> = {};
  errorMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  submit(event: Event): void {
    event.preventDefault();
    this.errors = {};

    if (!this.username.trim()) this.errors['username'] = "L'identifiant est obligatoire.";
    if (!this.password) this.errors['password'] = "Le mot de passe est obligatoire.";
    if (Object.keys(this.errors).length > 0) return;

    this.errorMessage = '';
    this.loading = true;

    this.authService.login(this.username, this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => {
        this.errorMessage = 'Identifiant ou mot de passe incorrect.';
        this.loading = false;
      }
    });
  }
}
