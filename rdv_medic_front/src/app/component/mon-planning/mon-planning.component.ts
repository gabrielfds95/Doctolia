import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Slot } from '../../model/slot.model';

@Component({
  selector: 'app-mon-planning',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mon-planning.component.html',
  styleUrls: ['./mon-planning.component.scss']
})
export class MonPlanningComponent implements OnInit {

  slots: Slot[] = [];
  loading = true;
  errorMessage = '';

  filter: 'upcoming' | 'past' | 'all' = 'upcoming';

  showForm = false;
  form = { slotDate: '', slotTime: '', endTime: '', slotReason: '' };
  formError = '';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadSlots();
  }

  loadSlots(): void {
    this.loading = true;
    this.apiService.getDoctorMySlots().subscribe({
      next: (slots) => {
        this.slots = [...slots].sort((a, b) =>
          new Date(a.slotDate + 'T' + a.slotTime).getTime() -
          new Date(b.slotDate + 'T' + b.slotTime).getTime()
        );
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le planning.';
        this.loading = false;
      }
    });
  }

  get filteredSlots(): Slot[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.slots.filter(slot => {
      const d = new Date(slot.slotDate);
      const completed = slot.status === 'COMPLETED';
      if (this.filter === 'upcoming') return d >= today && !completed;
      if (this.filter === 'past')     return d < today  || completed;
      return true;
    });
  }

  setFilter(f: 'upcoming' | 'past' | 'all'): void {
    this.filter = f;
  }

  openForm(): void {
    this.showForm = true;
    this.formError = '';
    this.form = { slotDate: '', slotTime: '', endTime: '', slotReason: '' };
  }

  closeForm(): void {
    this.showForm = false;
  }

  submitForm(event: Event): void {
    event.preventDefault();
    if (!this.form.slotDate || !this.form.slotTime) {
      this.formError = 'La date et l\'heure sont obligatoires.';
      return;
    }
    if (!this.form.endTime) {
      const [h, m] = this.form.slotTime.split(':').map(Number);
      const end = m === 0
        ? `${String(h).padStart(2, '0')}:30`
        : `${String(h + 1).padStart(2, '0')}:00`;
      this.form.endTime = end;
    }
    this.apiService.createUnavailability(this.form).subscribe({
      next: () => { this.closeForm(); this.loadSlots(); },
      error: () => { this.formError = 'Erreur lors de la création de l\'indisponibilité.'; }
    });
  }

  deleteUnavailability(slotId: number): void {
    if (!confirm('Supprimer cette indisponibilité ?')) return;
    this.apiService.deleteUnavailability(slotId).subscribe({
      next: () => this.loadSlots(),
      error: () => { this.errorMessage = 'Erreur lors de la suppression.'; }
    });
  }

  completeSlot(slotId: number): void {
    if (!confirm('Marquer ce rendez-vous comme terminé ?')) return;
    this.apiService.completeSlot(slotId).subscribe({
      next: () => this.loadSlots(),
      error: () => { this.errorMessage = 'Erreur lors de la mise à jour.'; }
    });
  }

  isCompletable(slot: Slot): boolean { return slot.status === 'RESERVED'; }
  isUnavailability(slot: Slot): boolean { return slot.status === 'CANCELLED' && !slot.patient; }

  slotLabel(slot: Slot): string {
    if (slot.status === 'CANCELLED') return slot.patient ? 'Annulé' : 'Indisponible';
    if (slot.status === 'RESERVED')  return 'Réservé';
    if (slot.status === 'COMPLETED') return 'Terminé';
    return slot.status ?? '';
  }
}
