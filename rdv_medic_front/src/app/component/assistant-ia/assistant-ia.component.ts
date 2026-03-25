import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

// TODO: remplacer par un appel à l'API (ex: Claude API ou endpoint /assistant/ask)
const MOCK_RESPONSES: Record<string, string> = {
  default: 'Je suis l\'assistant médical Doctolia. Je peux vous donner des informations générales sur la santé, mais je ne remplace pas un médecin. Pour tout problème médical, consultez un professionnel de santé.',
  fièvre: 'Une fièvre supérieure à 38,5°C chez l\'adulte mérite attention. Hydratez-vous bien, reposez-vous, et consultez si la fièvre persiste plus de 3 jours ou dépasse 39,5°C.',
  mal: 'Les douleurs persistantes doivent être évaluées par un médecin. Pouvez-vous décrire où vous avez mal et depuis combien de temps ?',
  toux: 'Une toux persistante plus de 3 semaines doit être consultée. En attendant, restez hydraté et évitez les irritants comme la fumée.',
  tension: 'Une tension artérielle normale est inférieure à 120/80 mmHg. En cas de valeurs élevées répétées, consultez votre médecin.',
  rendez: 'Pour prendre rendez-vous avec un médecin, rendez-vous sur l\'onglet "Médecins" et choisissez un praticien disponible.',
};

function getMockResponse(input: string): string {
  const lower = input.toLowerCase();
  for (const [key, val] of Object.entries(MOCK_RESPONSES)) {
    if (key !== 'default' && lower.includes(key)) return val;
  }
  return MOCK_RESPONSES['default'];
}

@Component({
  selector: 'app-assistant-ia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assistant-ia.component.html',
  styleUrls: ['./assistant-ia.component.scss']
})
export class AssistantIaComponent {

  messages: ChatMessage[] = [
    {
      role: 'assistant',
      content: 'Bonjour ! Je suis l\'assistant médical Doctolia. Je peux répondre à vos questions de santé générales. Comment puis-je vous aider ?',
      timestamp: new Date()
    }
  ];

  input = '';
  isTyping = false;

  readonly suggestions = [
    'J\'ai de la fièvre',
    'J\'ai mal à la tête',
    'Comment prendre rendez-vous ?',
    'Ma tension est élevée',
  ];

  sendMessage(text?: string): void {
    const content = (text ?? this.input).trim();
    if (!content) return;

    this.messages.push({ role: 'user', content, timestamp: new Date() });
    this.input = '';
    this.isTyping = true;

    // TODO: remplacer par un vrai appel API
    setTimeout(() => {
      this.messages.push({
        role: 'assistant',
        content: getMockResponse(content),
        timestamp: new Date()
      });
      this.isTyping = false;
    }, 900);
  }

  useSuggestion(s: string): void {
    this.sendMessage(s);
  }
}
