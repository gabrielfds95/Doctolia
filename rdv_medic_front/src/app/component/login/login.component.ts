import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {

  form = { username: '', password: '' };
  errorMessage = '';
  loading = false;
  showPassword = false;
  rememberMe = false;

  constructor(private authService: AuthService, private router: Router) {}

  submit(event: Event): void {
    event.preventDefault();
    if (!this.form.username.trim() || !this.form.password) {
      this.errorMessage = 'Identifiant et mot de passe obligatoires.';
      return;
    }
    this.errorMessage = '';
    this.loading = true;

    this.authService.login(this.form.username.trim(), this.form.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.errorMessage = err.status === 403
          ? 'Votre compte est en attente de validation.'
          : 'Identifiant ou mot de passe incorrect.';
        this.loading = false;
      }
    });
  }
}
