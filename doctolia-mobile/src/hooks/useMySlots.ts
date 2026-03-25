import { useState, useEffect, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { Slot } from '../model/types';

// Custom hook : RDV du patient connecté (GET /patients/me/slots)
// Expose aussi cancelSlot pour annuler un RDV depuis la liste
export function useMySlots() {
  const [slots, setSlots] = useState<Slot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [trigger, setTrigger] = useState(0);

  useEffect(() => {
    let isMounted = true;

    setLoading(true);
    axiosInstance
      .get<Slot[]>('/patients/me/slots')
      .then((res) => {
        if (isMounted) setSlots(res.data);
      })
      .catch(() => {
        if (isMounted) setError('Impossible de charger vos rendez-vous.');
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [trigger]);

  // Annule un créneau via PATCH /slots/{id}/cancel puis recharge
  const cancelSlot = useCallback(async (slotId: number): Promise<void> => {
    await axiosInstance.patch(`/slots/${slotId}/cancel`);
    setTrigger((t) => t + 1);
  }, []);

  const refresh = useCallback(() => setTrigger((t) => t + 1), []);

  return { slots, loading, error, cancelSlot, refresh };
}
