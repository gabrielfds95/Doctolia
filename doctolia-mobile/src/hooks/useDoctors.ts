import { useState, useEffect } from 'react';
import axiosInstance from '../api/axiosInstance';
import { Doctor } from '../model/types';

// Custom hook : encapsule la logique de fetch des médecins
// Réutilisable partout sans répéter le useEffect + useState
export function useDoctors() {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true; // Évite les setState sur composant démonté (cleanup pattern)

    setLoading(true);
    axiosInstance
      .get<Doctor[]>('/doctors')
      .then((res) => {
        if (isMounted) setDoctors(res.data);
      })
      .catch(() => {
        if (isMounted) setError('Impossible de charger les médecins.');
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false; // Cleanup : annule la mise à jour si le composant est démonté
    };
  }, []); // Tableau vide = exécuté une seule fois au montage

  return { doctors, loading, error };
}
