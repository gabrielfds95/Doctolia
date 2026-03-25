// Importation des décorateurs et outils Angular
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Doctor } from '../../model/doctor.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-doctor-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './doctor-list.component.html',
  styleUrls: ['./doctor-list.component.scss']
})
export class DoctorListComponent implements OnInit {

  doctors: Doctor[] = [];
  loading = true;
  errorMessage = '';
  searchQuery = '';

  get firstName(): string {
    return this.authService.currentUser?.firstName ?? 'vous';
  }

  get filteredDoctors(): Doctor[] {
    if (!this.searchQuery.trim()) return this.doctors;
    const q = this.searchQuery.toLowerCase();
    return this.doctors.filter(d =>
      `${d.firstName} ${d.lastName} ${d.speciality}`.toLowerCase().includes(q)
    );
  }

  constructor(private apiService: ApiService, private router: Router, private authService: AuthService) {}

  // Appel de l'API dès le chargement du composant
  ngOnInit(): void {
    // subrcribe() :méthode pour écouter les données retournés par l'API
    this.apiService.getDoctors().subscribe({
        // si tout se passe bien on stock les données dans doctors et on arrete le chargement
      next: (data) => {
        this.doctors = data;
        this.loading = false;
      },
      // si err > msg d'erreur et stop chargement
      error: (err) => {
        this.errorMessage = 'Erreur lors du chargement des médecins';
        console.error(err);
        this.loading = false;
      }
    });
  }
  goToDoctorSlots(doctor: Doctor) {
    // Navigue vers la page des créneaux pour ce médecin
    this.router.navigate(['/doctor-slots', doctor.id]);
  }
}