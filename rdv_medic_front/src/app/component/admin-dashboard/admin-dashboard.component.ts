import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { Doctor } from '../../model/doctor.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {

  pendingDoctors: Doctor[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';

  constructor(private apiService: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.apiService.getPendingDoctors().subscribe({
      next: (doctors) => {
        this.pendingDoctors = doctors;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les médecins en attente.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  approve(id: number): void {
    this.apiService.approveDoctor(id).subscribe({
      next: () => { this.successMessage = 'Médecin approuvé.'; this.load(); },
      error: () => { this.errorMessage = 'Erreur lors de l\'approbation.'; }
    });
  }

  reject(id: number): void {
    if (!confirm('Rejeter et supprimer ce médecin ?')) return;
    this.apiService.rejectDoctor(id).subscribe({
      next: () => { this.successMessage = 'Médecin rejeté.'; this.load(); },
      error: () => { this.errorMessage = 'Erreur lors du rejet.'; }
    });
  }
}
