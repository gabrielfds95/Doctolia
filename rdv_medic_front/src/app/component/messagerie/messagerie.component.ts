import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Message } from '../../model/message.model';

@Component({
  selector: 'app-messagerie',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messagerie.component.html',
  styleUrls: ['./messagerie.component.scss']
})
export class MessagerieComponent implements OnInit {

  slotId!: number;
  messages: Message[] = [];
  newMessage = '';

  loading = true;
  sending = false;
  // Séparés car ils pilotent des affichages différents :
  // loadError bloque tout le fil (RDV inaccessible), sendError n'est qu'un bandeau ponctuel.
  loadError = '';
  sendError = '';

  constructor(
    private route: ActivatedRoute,
    private apiService: ApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Le fil est fixé au slotId de la route pour toute la durée de vie du composant
    // (on revient sur /mes-rdv ou /mon-planning pour changer de RDV, pas de navigation interne).
    this.slotId = Number(this.route.snapshot.paramMap.get('slotId'));
    this.loadMessages();
  }

  loadMessages(): void {
    this.loading = true;
    this.loadError = '';
    this.apiService.getSlotMessages(this.slotId).subscribe({
      next: (messages) => {
        this.messages = messages;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loadError = this.errorLabel(err.status, 'charger');
        this.loading = false;
      }
    });
  }

  sendMessage(): void {
    const content = this.newMessage.trim();
    if (!content || this.sending) return;

    this.sending = true;
    this.sendError = '';
    this.apiService.sendSlotMessage(this.slotId, content).subscribe({
      next: () => {
        this.newMessage = '';
        this.sending = false;
        // Recharge depuis le serveur plutôt qu'un ajout optimiste : on affiche
        // exactement ce que le back a enregistré (id Mongo, sentAt serveur).
        this.loadMessages();
      },
      error: (err: HttpErrorResponse) => {
        this.sendError = this.errorLabel(err.status, 'envoyer');
        this.sending = false;
      }
    });
  }

  /**
   * "C'est moi" est un affichage, pas une autorisation : le back revalide
   * l'ownership à chaque requête (403 sinon), ceci sert juste à aligner la bulle.
   */
  isMine(msg: Message): boolean {
    return msg.senderId === this.authService.getUserId();
  }

  formatTime(iso: string): string {
    const d = new Date(iso);
    const today = new Date();
    if (d.toDateString() === today.toDateString()) {
      return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    }
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  private errorLabel(status: number, action: 'charger' | 'envoyer'): string {
    if (status === 403) return "Vous n'êtes pas participant à ce rendez-vous.";
    if (status === 404) return 'Rendez-vous introuvable.';
    return action === 'charger' ? 'Impossible de charger les messages.' : "Impossible d'envoyer le message.";
  }
}
