import { useState, useEffect, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { Slot } from '../model/types';

// Custom hook paramétrique : reçoit un doctorId, refetch quand il change
export function useSlots(doctorId: number) {
  const [slots, setSlots] = useState<Slot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [trigger, setTrigger] = useState(0); // Compteur pour forcer le rechargement

  useEffect(() => {
    let isMounted = true;

    setLoading(true);
    axiosInstance
      .get<Slot[]>(`/doctors/${doctorId}/slots`)
      .then((res) => {
        if (isMounted) setSlots(res.data);
      })
      .catch(() => {
        if (isMounted) setError('Aucun créneau disponible pour ce médecin.');
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [doctorId, trigger]); // Re-exécuté si doctorId ou trigger change

  // Expose une fonction refresh pour recharger après une réservation
  const refresh = useCallback(() => setTrigger((t) => t + 1), []);

  return { slots, loading, error, refresh };
}
