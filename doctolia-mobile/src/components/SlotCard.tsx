import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Slot } from '../model/types';

// Interface TypeScript pour typer les props du composant
interface SlotCardProps {
  slot: Slot;
  onBook: (slot: Slot) => void; // Callback reçu du parent
}

// Composant fonctionnel réutilisable — équivalent d'un composant React classique
export function SlotCard({ slot, onBook }: SlotCardProps) {
  const isAvailable = slot.status === 'AVAILABLE';

  return (
    <View style={[styles.card, !isAvailable && styles.cardUnavailable]}>
      <View style={styles.timeRow}>
        <Text style={styles.time}>{slot.slotTime.slice(0, 5)}</Text>
        <Text style={styles.arrow}> → </Text>
        <Text style={styles.time}>{slot.endTime.slice(0, 5)}</Text>
        <View style={[styles.badge, !isAvailable && styles.badgeBooked]}>
          <Text style={styles.badgeText}>
            {isAvailable ? 'Disponible' : 'Réservé'}
          </Text>
        </View>
      </View>

      <Text style={styles.date}>{slot.slotDate}</Text>

      {slot.slotReason ? (
        <Text style={styles.reason}>{slot.slotReason}</Text>
      ) : null}

      {isAvailable && (
        <TouchableOpacity style={styles.btnBook} onPress={() => onBook(slot)}>
          <Text style={styles.btnBookText}>Prendre RDV</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.07,
    shadowRadius: 8,
    elevation: 3,
  },
  cardUnavailable: {
    backgroundColor: '#f9fafb',
    opacity: 0.8,
  },
  timeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  time: {
    fontSize: 18,
    fontWeight: '700',
    color: '#111827',
  },
  arrow: {
    fontSize: 16,
    color: '#9ca3af',
  },
  badge: {
    marginLeft: 'auto',
    backgroundColor: '#dcfce7',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 20,
  },
  badgeBooked: {
    backgroundColor: '#fee2e2',
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#374151',
  },
  date: {
    fontSize: 13,
    color: '#6b7280',
    marginBottom: 4,
  },
  reason: {
    fontSize: 13,
    color: '#374151',
    fontStyle: 'italic',
    marginBottom: 8,
  },
  btnBook: {
    marginTop: 8,
    backgroundColor: '#2563eb',
    borderRadius: 8,
    paddingVertical: 10,
    alignItems: 'center',
  },
  btnBookText: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 14,
  },
});
