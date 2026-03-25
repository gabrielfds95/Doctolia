import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Slot } from '../../model/slot.model';

@Component({
  selector: 'app-mes-rdv',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mes-rdv.component.html',
  styleUrls: ['./mes-rdv.component.scss']
})
export class MesRdvComponent implements OnInit {

  slots: Slot[] = [];
  loading = true;
  errorMessage = '';

  filter: 'upcoming' | 'past' | 'all' = 'upcoming';

  editingSlot: Slot | null = null;
  editReason = '';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadSlots();
  }

  loadSlots(): void {
    this.loading = true;
    this.apiService.getMySlots().subscribe({
      next: (slots) => {
        this.slots = [...slots].sort((a, b) =>
          new Date(a.slotDate + 'T' + a.slotTime).getTime() -
          new Date(b.slotDate + 'T' + b.slotTime).getTime()
        );
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger vos rendez-vous.';
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
      const cancelled = slot.status === 'CANCELLED';
      if (this.filter === 'upcoming') return d >= today && !completed && !cancelled;
      if (this.filter === 'past')     return d < today  || completed || cancelled;
      return true;
    });
  }

  setFilter(f: 'upcoming' | 'past' | 'all'): void {
    this.filter = f;
  }

  cancel(slotId: number): void {
    if (!confirm('Confirmer l\'annulation de ce rendez-vous ?')) return;
    this.apiService.cancelSlot(slotId).subscribe({
      next: () => this.loadSlots(),
      error: () => { this.errorMessage = 'Erreur lors de l\'annulation.'; }
    });
  }

  openEditReason(slot: Slot): void {
    this.editingSlot = slot;
    this.editReason = slot.slotReason ?? '';
  }

  closeEditReason(): void {
    this.editingSlot = null;
    this.editReason = '';
  }

  saveReason(): void {
    if (!this.editingSlot) return;
    this.apiService.updateSlotReason(this.editingSlot.id, this.editReason).subscribe({
      next: () => { this.closeEditReason(); this.loadSlots(); },
      error: () => { this.errorMessage = 'Erreur lors de la modification du motif.'; }
    });
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      RESERVED: 'Confirmé', CANCELLED: 'Annulé', COMPLETED: 'Terminé', AVAILABLE: 'Disponible'
    };
    return labels[status] ?? status;
  }

  countByStatus(status: string): number {
    return this.slots.filter(s => s.status === status).length;
  }

  isCancellable(slot: Slot): boolean {
    return slot.status === 'RESERVED' && new Date(slot.slotDate) >= new Date();
  }
}
