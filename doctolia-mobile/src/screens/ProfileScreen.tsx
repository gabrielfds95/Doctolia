import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useProfile } from '../hooks/useProfile';
import { useAuth } from '../context/AuthContext';
import { UserProfile } from '../model/types';

export function ProfileScreen() {
  // useProfile : GET /users/me + updateProfile (PATCH /users/me)
  const { profile, loading, saving, error, updateProfile } = useProfile();
  const { logout } = useAuth();

  // Formulaire local initialisé depuis le profil chargé
  const [form, setForm] = useState<Partial<UserProfile>>({});
  const [editing, setEditing] = useState(false);

  // Synchronise le formulaire quand le profil est chargé
  useEffect(() => {
    if (profile) {
      setForm({
        firstName: profile.firstName,
        lastName: profile.lastName,
        email: profile.email,
        phoneNumber: profile.phoneNumber,
        address: profile.address,
        age: profile.age,
      });
    }
  }, [profile]);

  // Handler générique pour les TextInput du formulaire
  function handleChange(field: keyof UserProfile, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSave() {
    try {
      await updateProfile(form);
      setEditing(false);
      Alert.alert('Profil mis à jour', 'Vos informations ont été enregistrées.');
    } catch {
      Alert.alert('Erreur', 'Impossible de mettre à jour le profil.');
    }
  }

  if (loading) {
    return <ActivityIndicator style={{ flex: 1 }} size="large" color="#2563eb" />;
  }

  if (error || !profile) {
    return <Text style={styles.errorText}>{error ?? 'Profil introuvable.'}</Text>;
  }

  const isDoctor = !!profile.speciality;

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView style={styles.container} contentContainerStyle={styles.content}>
        {/* Avatar initiales */}
        <View style={styles.avatarWrap}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {profile.firstName[0]}{profile.lastName[0]}
            </Text>
          </View>
          <Text style={styles.fullName}>
            {isDoctor ? 'Dr. ' : ''}{profile.firstName} {profile.lastName}
          </Text>
          <Text style={styles.username}>@{profile.username}</Text>
          {isDoctor && (
            <Text style={styles.speciality}>{profile.speciality} · {profile.department}</Text>
          )}
        </View>

        {/* Champs communs */}
        <Text style={styles.sectionTitle}>Informations personnelles</Text>

        <Text style={styles.label}>Prénom</Text>
        <TextInput
          style={[styles.input, !editing && styles.inputReadonly]}
          value={form.firstName ?? ''}
          onChangeText={(v) => handleChange('firstName', v)}
          editable={editing}
        />

        <Text style={styles.label}>Nom</Text>
        <TextInput
          style={[styles.input, !editing && styles.inputReadonly]}
          value={form.lastName ?? ''}
          onChangeText={(v) => handleChange('lastName', v)}
          editable={editing}
        />

        <Text style={styles.label}>Email</Text>
        <TextInput
          style={[styles.input, !editing && styles.inputReadonly]}
          value={form.email ?? ''}
          onChangeText={(v) => handleChange('email', v)}
          editable={editing}
          keyboardType="email-address"
          autoCapitalize="none"
        />

        {/* Champs spécifiques patient */}
        {!isDoctor && (
          <>
            <Text style={styles.sectionTitle}>Coordonnées</Text>

            <Text style={styles.label}>Téléphone</Text>
            <TextInput
              style={[styles.input, !editing && styles.inputReadonly]}
              value={form.phoneNumber ?? ''}
              onChangeText={(v) => handleChange('phoneNumber', v)}
              editable={editing}
              keyboardType="phone-pad"
            />

            <Text style={styles.label}>Adresse</Text>
            <TextInput
              style={[styles.input, !editing && styles.inputReadonly]}
              value={form.address ?? ''}
              onChangeText={(v) => handleChange('address', v)}
              editable={editing}
            />

            <Text style={styles.label}>Âge</Text>
            <TextInput
              style={[styles.input, !editing && styles.inputReadonly]}
              value={form.age != null ? String(form.age) : ''}
              onChangeText={(v) => setForm((p) => ({ ...p, age: parseInt(v) || undefined }))}
              editable={editing}
              keyboardType="numeric"
            />
          </>
        )}

        {/* Actions */}
        {editing ? (
          <View style={styles.actions}>
            <TouchableOpacity
              style={[styles.btnSave, saving && styles.btnDisabled]}
              onPress={handleSave}
              disabled={saving}
            >
              {saving ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.btnText}>Enregistrer</Text>
              )}
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.btnEdit}
              onPress={() => { setEditing(false); setForm({ firstName: profile.firstName, lastName: profile.lastName, email: profile.email, phoneNumber: profile.phoneNumber, address: profile.address, age: profile.age }); }}
            >
              <Text style={styles.btnEditText}>Annuler</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <TouchableOpacity style={styles.btnEdit} onPress={() => setEditing(true)}>
            <Text style={styles.btnEditText}>Modifier le profil</Text>
          </TouchableOpacity>
        )}

        <TouchableOpacity style={styles.btnLogout} onPress={logout}>
          <Text style={styles.btnLogoutText}>Se déconnecter</Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0f4f8',
  },
  content: {
    padding: 20,
    paddingBottom: 40,
  },
  avatarWrap: {
    alignItems: 'center',
    marginBottom: 28,
  },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: '#dbeafe',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 10,
  },
  avatarText: {
    fontSize: 26,
    fontWeight: '700',
    color: '#2563eb',
  },
  fullName: {
    fontSize: 20,
    fontWeight: '800',
    color: '#111827',
  },
  username: {
    fontSize: 14,
    color: '#9ca3af',
    marginTop: 2,
  },
  speciality: {
    fontSize: 13,
    color: '#2563eb',
    fontWeight: '600',
    marginTop: 4,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: '#6b7280',
    textTransform: 'uppercase',
    letterSpacing: 0.6,
    marginTop: 20,
    marginBottom: 12,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 5,
  },
  input: {
    borderWidth: 1.5,
    borderColor: '#d1d5db',
    borderRadius: 10,
    padding: 12,
    fontSize: 15,
    color: '#111827',
    marginBottom: 14,
    backgroundColor: '#fff',
  },
  inputReadonly: {
    backgroundColor: '#f9fafb',
    color: '#6b7280',
  },
  actions: {
    marginTop: 8,
    gap: 10,
  },
  btnSave: {
    backgroundColor: '#2563eb',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
  },
  btnDisabled: {
    opacity: 0.6,
  },
  btnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  btnEdit: {
    borderWidth: 1.5,
    borderColor: '#2563eb',
    borderRadius: 10,
    paddingVertical: 13,
    alignItems: 'center',
    marginTop: 8,
  },
  btnEditText: {
    color: '#2563eb',
    fontSize: 15,
    fontWeight: '700',
  },
  btnLogout: {
    marginTop: 24,
    paddingVertical: 13,
    alignItems: 'center',
  },
  btnLogoutText: {
    color: '#dc2626',
    fontSize: 15,
    fontWeight: '600',
  },
  errorText: {
    margin: 24,
    textAlign: 'center',
    color: '#dc2626',
  },
});
