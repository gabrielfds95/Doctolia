export interface Message {
  id: number;
  senderId: number;
  senderName: string;
  content: string;
  sentAt: string; // ISO date string
  read: boolean;
}

export interface Conversation {
  id: number;
  participantId: number;
  participantName: string;
  participantRole: 'DOCTOR' | 'PATIENT';
  participantInitials: string;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
  messages: Message[];
}
