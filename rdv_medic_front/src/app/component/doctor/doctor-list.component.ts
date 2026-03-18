// Importation des décorateurs et outils Angular
import { Component, OnInit } from '@angular/core';
// Importation du composant de routage (utile si tu utilises des routes)
import { Router } from '@angular/router';
// Importation de ton service API (adapte le chemin si nécessaire)
import { ApiService } from '../../services/api.service';  // adapte le chemin si besoin
import { Doctor } from '../../model/doctor.model';
import { CommonModule } from '@angular/common';
// import { NgIf, NgFor } from '@angular/common';

@Component({
  selector: 'app-doctor-list',// Nom du composant utilisé dans le HTML
  standalone: true, // Indique que ce composant est standalone (pas besoin d'etre dans un module)
  imports: [CommonModule], // Importation des composants nécessaires (ici pour les directives structurelles)
  templateUrl: './doctor-list.component.html', // Fichier HTML associé au composant
  styleUrls: ['./doctor-list.component.scss']     // Fichier SCSS associé au composant
})
export class DoctorListComponent implements OnInit{
  
  doctors: Doctor[] = [];      // Liste des médecins à afficher
  loading: boolean = true;       // Indicateur de chargement
  errorMessage: string = '';     // Message d'erreur éventuel

  constructor(private apiService: ApiService, private router: Router) {}

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