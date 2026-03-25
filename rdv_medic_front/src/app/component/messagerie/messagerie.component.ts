import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Conversation, Message } from '../../model/message.model';

// TODO: remplacer les données en dur par un appel à l'API MessageService
const MOCK_CONVERSATIONS: Conversation[] = [
  {
    id: 1,
    participantId: 10,
    participantName: 'Dr. Sophie Martin',
    participantRole: 'DOCTOR',
    participantInitials: 'SM',
    lastMessage: 'Bonjour, vos résultats sont bons. Pas d\'inquiétude.',
    lastMessageAt: '2026-03-24T14:30:00',
    unreadCount: 1,
    messages: [
      { id: 1, senderId: 1, senderName: 'Moi', content: 'Bonjour Docteur, j\'ai reçu mes résultats d\'analyse. Pouvez-vous me les expliquer ?', sentAt: '2026-03-24T14:10:00', read: true },
      { id: 2, senderId: 10, senderName: 'Dr. Sophie Martin', content: 'Bonjour, vos résultats sont bons. Pas d\'inquiétude.', sentAt: '2026-03-24T14:30:00', read: false },
    ]
  },
  {
    id: 2,
    participantId: 11,
    participantName: 'Dr. Jean Dupont',
    participantRole: 'DOCTOR',
    participantInitials: 'JD',
    lastMessage: 'Votre prochain rendez-vous est confirmé pour le 3 avril.',
    lastMessageAt: '2026-03-23T09:15:00',
    unreadCount: 0,
    messages: [
      { id: 3, senderId: 11, senderName: 'Dr. Jean Dupont', content: 'Votre prochain rendez-vous est confirmé pour le 3 avril.', sentAt: '2026-03-23T09:15:00', read: true },
    ]
  },
  {
    id: 3,
    participantId: 12,
    participantName: 'Dr. Amina Benali',
    participantRole: 'DOCTOR',
    participantInitials: 'AB',
    lastMessage: 'N\'oubliez pas de prendre vos médicaments le matin.',
    lastMessageAt: '2026-03-20T11:00:00',
    unreadCount: 0,
    messages: [
      { id: 4, senderId: 1, senderName: 'Moi', content: 'Docteur, j\'ai oublié mes prescriptions. Pouvez-vous me les rappeler ?', sentAt: '2026-03-20T10:45:00', read: true },
      { id: 5, senderId: 12, senderName: 'Dr. Amina Benali', content: 'N\'oubliez pas de prendre vos médicaments le matin.', sentAt: '2026-03-20T11:00:00', read: true },
    ]
  }
];

@Component({
  selector: 'app-messagerie',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messagerie.component.html',
  styleUrls: ['./messagerie.component.scss']
})
export class MessagerieComponent {

  conversations: Conversation[] = MOCK_CONVERSATIONS;
  activeConversation: Conversation | null = null;
  newMessage = '';

  selectConversation(conv: Conversation): void {
    this.activeConversation = conv;
    // Marquer comme lu
    conv.unreadCount = 0;
    conv.messages.forEach(m => m.read = true);
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.activeConversation) return;
    const msg: Message = {
      id: Date.now(),
      senderId: 1,
      senderName: 'Moi',
      content: this.newMessage.trim(),
      sentAt: new Date().toISOString(),
      read: true
    };
    this.activeConversation.messages.push(msg);
    this.activeConversation.lastMessage = msg.content;
    this.activeConversation.lastMessageAt = msg.sentAt;
    this.newMessage = '';
    // TODO: appeler MessageService.send(activeConversation.id, msg.content)
  }

  get totalUnread(): number {
    return this.conversations.reduce((sum, c) => sum + c.unreadCount, 0);
  }

  isMine(msg: Message): boolean { return msg.senderId === 1; }

  formatTime(iso: string): string {
    const d = new Date(iso);
    const today = new Date();
    if (d.toDateString() === today.toDateString()) {
      return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    }
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
  }
}
