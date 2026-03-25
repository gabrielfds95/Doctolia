import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { UserProfile } from '../../model/user-profile.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

  profile: UserProfile | null = null;
  loading = true;
  errorMessage = '';
  successMessage = '';

  editing = false;
  form: Partial<UserProfile> = {};

  constructor(
    private apiService: ApiService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.apiService.getProfile().subscribe({
      next: (p) => {
        this.profile = p;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger votre profil.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  openEdit(): void {
    this.form = { ...this.profile };
    this.editing = true;
    this.successMessage = '';
  }

  cancelEdit(): void {
    this.editing = false;
  }

  save(): void {
    this.apiService.updateProfile(this.form).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.editing = false;
        this.successMessage = 'Profil mis à jour avec succès.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la mise à jour du profil.';
      }
    });
  }

  isDoctor(): boolean { return this.authService.isDoctor(); }
  isPatient(): boolean { return this.authService.isPatient(); }
}
