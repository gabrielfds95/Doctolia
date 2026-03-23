import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest } from '../../model/register.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {

  form = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    ssn: '',
    phoneNumber: '',
    address: '',
    age: ''
  };

  errors: Record<string, string> = {};
  errorMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  private validate(): boolean {
    this.errors = {};

    if (!this.form.username.trim())
      this.errors['username'] = "L'identifiant est obligatoire.";

    if (!this.form.email.trim())
      this.errors['email'] = "L'email est obligatoire.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email))
      this.errors['email'] = "Format d'email invalide.";

    if (!this.form.password)
      this.errors['password'] = "Le mot de passe est obligatoire.";
    else if (this.form.password.length < 8)
      this.errors['password'] = "8 caractères minimum.";

    if (!this.form.firstName.trim())
      this.errors['firstName'] = "Le prénom est obligatoire.";

    if (!this.form.lastName.trim())
      this.errors['lastName'] = "Le nom est obligatoire.";

    if (this.form.age) {
      const age = Number(this.form.age);
      if (isNaN(age) || age < 0 || age > 150)
        this.errors['age'] = "Âge invalide.";
    }

    return Object.keys(this.errors).length === 0;
  }

  submit(event: Event): void {
    event.preventDefault();
    if (!this.validate()) return;

    this.errorMessage = '';
    this.loading = true;

    const data: RegisterRequest = {
      username: this.form.username,
      email: this.form.email,
      password: this.form.password,
      firstName: this.form.firstName,
      lastName: this.form.lastName,
      userType: 'PATIENT',
      ssn: this.form.ssn || undefined,
      phoneNumber: this.form.phoneNumber || undefined,
      address: this.form.address || undefined,
      age: this.form.age ? Number(this.form.age) : undefined
    };

    this.authService.register(data).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.errorMessage = err.error?.error ?? "Erreur lors de l'inscription.";
        this.loading = false;
      }
    });
  }
}
