import { useState, useEffect, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { UserProfile } from '../model/types';

// Custom hook : profil de l'utilisateur connecté
// GET  /users/me  → charge le profil
// PATCH /users/me → met à jour les champs modifiables
export function useProfile() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    axiosInstance
      .get<UserProfile>('/users/me')
      .then((res) => {
        if (isMounted) setProfile(res.data);
      })
      .catch(() => {
        if (isMounted) setError('Impossible de charger le profil.');
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Met à jour uniquement les champs fournis (PATCH partiel)
  const updateProfile = useCallback(async (changes: Partial<UserProfile>): Promise<void> => {
    setSaving(true);
    try {
      const { data } = await axiosInstance.patch<UserProfile>('/users/me', changes);
      setProfile(data);
    } finally {
      setSaving(false);
    }
  }, []);

  return { profile, loading, saving, error, updateProfile };
}
