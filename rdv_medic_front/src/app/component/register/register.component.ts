import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest } from '../../model/register.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {

  userType: 'PATIENT' | 'DOCTOR' = 'PATIENT';

  form = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    // Patient
    ssn: '',
    phoneNumber: '',
    address: '',
    age: '',
    // Doctor
    speciality: '',
    licenseNumber: '',
    department: '',
    experienceYears: ''
  };

  errors: Record<string, string> = {};
  errorMessage = '';
  pendingMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  selectType(type: 'PATIENT' | 'DOCTOR'): void {
    this.userType = type;
    this.errors = {};
    this.errorMessage = '';
    this.pendingMessage = '';
  }

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

    if (this.userType === 'PATIENT' && this.form.age) {
      const age = Number(this.form.age);
      if (isNaN(age) || age < 0 || age > 150)
        this.errors['age'] = "Âge invalide.";
    }

    if (this.userType === 'DOCTOR' && !this.form.speciality.trim())
      this.errors['speciality'] = "La spécialité est obligatoire.";

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
      userType: this.userType,
      // Patient
      ssn: this.form.ssn || undefined,
      phoneNumber: this.form.phoneNumber || undefined,
      address: this.form.address || undefined,
      age: this.form.age ? Number(this.form.age) : undefined,
      // Doctor
      speciality: this.form.speciality || undefined,
      licenseNumber: this.form.licenseNumber || undefined,
      department: this.form.department || undefined,
      experienceYears: this.form.experienceYears ? Number(this.form.experienceYears) : undefined
    };

    this.authService.register(data).subscribe({
      next: (res) => {
        if (res.enabled === false) {
          this.pendingMessage = 'Votre compte médecin a bien été créé. Il sera activé après validation par un administrateur.';
          this.loading = false;
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.error ?? "Erreur lors de l'inscription.";
        this.loading = false;
      }
    });
  }
}
