import React from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useMySlots } from '../hooks/useMySlots';
import { Slot } from '../model/types';

// Couleurs et libellés par statut — même mapping que SlotCard
const STATUS_LABEL: Record<string, string> = {
  AVAILABLE: 'Disponible',
  RESERVED: 'Réservé',
  CANCELLED: 'Annulé',
  COMPLETED: 'Terminé',
};

const STATUS_COLOR: Record<string, string> = {
  AVAILABLE: '#dcfce7',
  RESERVED: '#dbeafe',
  CANCELLED: '#fee2e2',
  COMPLETED: '#f3f4f6',
};

export function MesRdvScreen() {
  // Custom hook : GET /patients/me/slots + cancelSlot(id)
  const { slots, loading, error, cancelSlot } = useMySlots();

  function confirmCancel(slot: Slot) {
    Alert.alert(
      'Annuler le rendez-vous',
      `Le ${slot.slotDate} à ${slot.slotTime.slice(0, 5)} avec Dr. ${slot.doctor.firstName} ${slot.doctor.lastName}`,
      [
        { text: 'Non', style: 'cancel' },
        {
          text: 'Oui, annuler',
          style: 'destructive',
          onPress: async () => {
            try {
              await cancelSlot(slot.id);
            } catch {
              Alert.alert('Erreur', "Impossible d'annuler ce rendez-vous.");
            }
          },
        },
      ]
    );
  }

  function renderSlot({ item }: { item: Slot }) {
    const canCancel = item.status === 'RESERVED';
    const badgeBg = STATUS_COLOR[item.status] ?? '#f3f4f6';
    const badgeLabel = STATUS_LABEL[item.status] ?? item.status;

    return (
      <View style={[styles.card, item.status === 'CANCELLED' && styles.cardCancelled]}>
        {/* En-tête : date + badge statut */}
        <View style={styles.cardHeader}>
          <Text style={styles.date}>{item.slotDate}</Text>
          <View style={[styles.badge, { backgroundColor: badgeBg }]}>
            <Text style={styles.badgeText}>{badgeLabel}</Text>
          </View>
        </View>

        {/* Horaire */}
        <Text style={styles.time}>
          {item.slotTime.slice(0, 5)} → {item.endTime.slice(0, 5)}
        </Text>

        {/* Médecin */}
        <Text style={styles.doctor}>
          Dr. {item.doctor.firstName} {item.doctor.lastName} · {item.doctor.speciality}
        </Text>

        {/* Motif si renseigné */}
        {item.slotReason ? (
          <Text style={styles.reason}>{item.slotReason}</Text>
        ) : null}

        {/* Bouton annulation uniquement pour les RDV RESERVED */}
        {canCancel && (
          <TouchableOpacity style={styles.btnCancel} onPress={() => confirmCancel(item)}>
            <Text style={styles.btnCancelText}>Annuler ce RDV</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  }

  if (loading) {
    return <ActivityIndicator style={{ flex: 1 }} size="large" color="#2563eb" />;
  }

  if (error) {
    return <Text style={styles.errorText}>{error}</Text>;
  }

  if (slots.length === 0) {
    return (
      <View style={styles.empty}>
        <Text style={styles.emptyText}>Vous n'avez aucun rendez-vous.</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.sectionTitle}>
        {slots.length} rendez-vous
      </Text>
      <FlatList
        data={slots}
        keyExtractor={(s) => String(s.id)}
        renderItem={renderSlot}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0f4f8',
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#6b7280',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  list: {
    paddingHorizontal: 16,
    paddingBottom: 24,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
  cardCancelled: {
    opacity: 0.6,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  date: {
    fontSize: 15,
    fontWeight: '700',
    color: '#111827',
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 20,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#374151',
  },
  time: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 4,
  },
  doctor: {
    fontSize: 13,
    color: '#2563eb',
    fontWeight: '600',
    marginBottom: 4,
  },
  reason: {
    fontSize: 13,
    color: '#6b7280',
    fontStyle: 'italic',
    marginBottom: 8,
  },
  btnCancel: {
    marginTop: 8,
    borderWidth: 1.5,
    borderColor: '#dc2626',
    borderRadius: 8,
    paddingVertical: 8,
    alignItems: 'center',
  },
  btnCancelText: {
    color: '#dc2626',
    fontWeight: '700',
    fontSize: 13,
  },
  errorText: {
    margin: 24,
    textAlign: 'center',
    color: '#dc2626',
  },
  empty: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 15,
    color: '#9ca3af',
  },
});
