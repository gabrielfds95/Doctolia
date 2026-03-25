import React, { useState } from 'react';
import {
  View,
  Text,
  FlatList,
  Modal,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/AppNavigator';
import { useSlots } from '../hooks/useSlots';
import { useBookSlot } from '../hooks/useBookSlot';
import { useAuth } from '../context/AuthContext';
import { SlotCard } from '../components/SlotCard';
import { Slot } from '../model/types';

type Props = NativeStackScreenProps<RootStackParamList, 'SlotList'>;

export function SlotListScreen({ route }: Props) {
  // useRoute équivalent — paramètres de navigation typés
  const { doctorId } = route.params;

  // Custom hook paramétrique + refresh
  const { slots, loading, error, refresh } = useSlots(doctorId);
  const { user } = useAuth();

  // États du formulaire de réservation
  const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null);
  const [motif, setMotif] = useState('');

  // useBookSlot : encapsule POST /slot/{idDoctor}, le patientId vient du JWT
  const { book, booking } = useBookSlot(() => {
    refresh();
  });

  async function handleBook() {
    if (!selectedSlot) return;
    const ok = await book(selectedSlot, motif);
    if (ok) {
      Alert.alert('Rendez-vous confirmé', `Le ${selectedSlot.slotDate} à ${selectedSlot.slotTime.slice(0, 5)}`);
      setSelectedSlot(null);
      setMotif('');
    } else {
      Alert.alert('Erreur', 'Impossible de réserver ce créneau.');
    }
  }

  if (loading) {
    return <ActivityIndicator style={{ flex: 1 }} size="large" color="#2563eb" />;
  }

  if (error) {
    return <Text style={styles.errorText}>{error}</Text>;
  }

  const available = slots.filter((s) => s.status === 'AVAILABLE').length;

  return (
    <View style={styles.container}>
      <Text style={styles.summary}>
        {available} créneau{available > 1 ? 'x' : ''} disponible{available > 1 ? 's' : ''}
      </Text>

      <FlatList
        data={slots}
        keyExtractor={(s) => String(s.id)}
        // SlotCard : composant réutilisable avec props typées
        renderItem={({ item }) => (
          <SlotCard slot={item} onBook={setSelectedSlot} />
        )}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
      />

      {/* Modal de confirmation — équivalent RN d'une dialog box */}
      <Modal visible={!!selectedSlot} transparent animationType="slide">
        <View style={styles.overlay}>
          <View style={styles.modal}>
            <Text style={styles.modalTitle}>Confirmer le rendez-vous</Text>

            <Text style={styles.modalInfo}>
              📅 {selectedSlot?.slotDate}
            </Text>
            <Text style={styles.modalInfo}>
              🕐 {selectedSlot?.slotTime?.slice(0, 5)} → {selectedSlot?.endTime?.slice(0, 5)}
            </Text>
            <Text style={styles.modalPatient}>
              Patient : {user?.firstName} {user?.lastName}
            </Text>

            {/* Formulaire avec état local — onChangeText remplace onChange en React Native */}
            <TextInput
              style={styles.input}
              placeholder="Motif de la consultation (optionnel)"
              value={motif}
              onChangeText={setMotif}
            />

            <TouchableOpacity
              style={[styles.btnConfirm, booking && styles.btnDisabled]}
              onPress={handleBook}
              disabled={booking}
            >
              {booking ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.btnText}>Confirmer</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.btnCancel}
              onPress={() => { setSelectedSlot(null); setMotif(''); }}
            >
              <Text style={styles.btnCancelText}>Annuler</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0f4f8',
  },
  summary: {
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
  errorText: {
    margin: 24,
    textAlign: 'center',
    color: '#dc2626',
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modal: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
    paddingBottom: 36,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#111827',
    marginBottom: 16,
  },
  modalInfo: {
    fontSize: 15,
    color: '#374151',
    marginBottom: 6,
  },
  modalPatient: {
    fontSize: 14,
    color: '#6b7280',
    marginTop: 4,
    marginBottom: 16,
  },
  input: {
    borderWidth: 1.5,
    borderColor: '#d1d5db',
    borderRadius: 10,
    padding: 12,
    fontSize: 14,
    color: '#111827',
    marginBottom: 16,
  },
  btnConfirm: {
    backgroundColor: '#2563eb',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 10,
  },
  btnDisabled: {
    opacity: 0.6,
  },
  btnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  btnCancel: {
    paddingVertical: 12,
    alignItems: 'center',
  },
  btnCancelText: {
    color: '#6b7280',
    fontSize: 15,
    fontWeight: '600',
  },
});
