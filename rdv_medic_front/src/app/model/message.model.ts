export interface Message {
  id: string;        // ObjectId MongoDB, pas un number
  slotId: number;
  senderId: number;  // à comparer au userId du JWT pour savoir si "c'est moi"
  content: string;
  sentAt: string;     // ISO 8601, LocalDateTime Java sans timezone
}

export interface MessageCreate {
  content: string;
}
