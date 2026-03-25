import { useState, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { Slot } from '../model/types';

// Custom hook : encapsule la logique de réservation d'un créneau
// POST /slot/{idDoctor} — le patientId vient du JWT côté back
// Retourne une fonction book() et un état booking (spinner)
export function useBookSlot(onSuccess?: () => void) {
  const [booking, setBooking] = useState(false);
  const [bookError, setBookError] = useState<string | null>(null);

  const book = useCallback(
    async (slot: Slot, motif: string): Promise<boolean> => {
      setBooking(true);
      setBookError(null);
      try {
        await axiosInstance.post(`/slot/${slot.doctor.id}`, {
          slotDate: slot.slotDate,
          slotTime: slot.slotTime,
          endTime: slot.endTime,
          slotReason: motif || slot.slotReason,
          status: 'RESERVED',
        });
        onSuccess?.();
        return true;
      } catch {
        setBookError('Impossible de réserver ce créneau.');
        return false;
      } finally {
        setBooking(false);
      }
    },
    [onSuccess]
  );

  return { book, booking, bookError };
}
