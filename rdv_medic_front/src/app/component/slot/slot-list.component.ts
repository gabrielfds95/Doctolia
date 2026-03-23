import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Slot } from '../../model/slot.model';
import { Doctor } from '../../model/doctor.model';
import { CommonModule, DatePipe } from '@angular/common';

@Component({
  selector: 'app-slot-list',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './slot-list.component.html',
  styleUrls: ['./slot-list.component.scss']
})
export class SlotListComponent implements OnInit {

  doctorId!: number;
  selectedDoctor!: Doctor;
  takenSlots: Slot[] = [];
  loading = false;
  errorMessage = '';

  currentWeekStart: Date = this.getMonday(new Date());
  weekDays: Date[] = [];
  hours: string[] = [];

  selectedSlot: { day: Date; hour: string } | null = null;
  slotReason = '';
  confirmationMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private apiService: ApiService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.doctorId = Number(this.route.snapshot.paramMap.get('id'));
    this.apiService.getDoctorById(this.doctorId).subscribe((doctor: Doctor) => {
      this.selectedDoctor = doctor;
    });
    this.generateHours();
    this.generateWeekDays();
    this.loadTakenSlots();
  }

  getMonday(date: Date): Date {
    const day = date.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    const monday = new Date(date);
    monday.setDate(date.getDate() + diff);
    return monday;
  }

  generateWeekDays(): void {
    this.weekDays = [];
    for (let i = 0; i < 5; i++) {
      const day = new Date(this.currentWeekStart);
      day.setDate(this.currentWeekStart.getDate() + i);
      this.weekDays.push(day);
    }
  }

  generateHours(): void {
    for (let hour = 8; hour < 19; hour++) {
      this.hours.push(`${this.pad(hour)}:00`);
      this.hours.push(`${this.pad(hour)}:30`);
    }
  }

  pad(n: number): string {
    return n < 10 ? '0' + n : n.toString();
  }

  loadTakenSlots(): void {
    this.loading = true;
    this.apiService.getSlotsByDoctors(this.doctorId).subscribe({
      next: (slots) => {
        this.takenSlots = slots;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les créneaux.';
        this.loading = false;
      }
    });
  }

  isSlotTaken(date: Date, time: string): boolean {
    const dateStr = date.toISOString().split('T')[0];
    return this.takenSlots.some(slot => {
      const slotDateStr = new Date(slot.slotDate).toISOString().split('T')[0];
      return slotDateStr === dateStr && slot.slotTime.slice(0, 5) === time;
    });
  }

  isLunchBreak(time: string): boolean {
    return time === '12:00' || time === '12:30' || time === '13:00';
  }

  previousWeek(): void {
    this.currentWeekStart.setDate(this.currentWeekStart.getDate() - 7);
    this.generateWeekDays();
    this.loadTakenSlots();
  }

  nextWeek(): void {
    this.currentWeekStart.setDate(this.currentWeekStart.getDate() + 7);
    this.generateWeekDays();
    this.loadTakenSlots();
  }

  openForm(day: Date, hour: string): void {
    this.selectedSlot = { day, hour };
    this.slotReason = '';
    this.confirmationMessage = null;
  }

  closeForm(): void {
    this.selectedSlot = null;
    this.slotReason = '';
  }

  submitForm(event: Event): void {
    event.preventDefault();

    const patientId = this.authService.currentUser!.id;

    const endTime = this.selectedSlot!.hour.endsWith(':00')
      ? this.selectedSlot!.hour.replace(':00', ':30')
      : `${(Number(this.selectedSlot!.hour.split(':')[0]) + 1).toString().padStart(2, '0')}:00`;

    const slotData = {
      doctorId: this.doctorId,
      patientId,
      slotDate: this.selectedSlot!.day.toISOString().split('T')[0],
      slotTime: this.selectedSlot!.hour,
      endTime,
      slotReason: this.slotReason
    };

    this.apiService.postNewSlot(slotData).subscribe({
      next: () => {
        this.confirmationMessage = 'Réservation confirmée !';
        this.loadTakenSlots();
        setTimeout(() => this.closeForm(), 2000);
      },
      error: () => {
        this.confirmationMessage = 'Erreur lors de la réservation.';
      }
    });
  }
}
