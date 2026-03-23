import React from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/AppNavigator';
import { useDoctors } from '../hooks/useDoctors';
import { useAuth } from '../context/AuthContext';
import { Doctor } from '../model/types';

type Props = NativeStackScreenProps<RootStackParamList, 'DoctorList'>;

export function DoctorListScreen({ navigation }: Props) {
  // Custom hook — toute la logique de fetch est isolée ici
  const { doctors, loading, error } = useDoctors();
  const { user, logout } = useAuth();

  if (loading) {
    return <ActivityIndicator style={styles.center} size="large" color="#2563eb" />;
  }

  if (error) {
    return <Text style={styles.errorText}>{error}</Text>;
  }

  // Rendu d'un item de liste — composant fonctionnel avec props typées
  function renderDoctor({ item }: { item: Doctor }) {
    return (
      <TouchableOpacity
        style={styles.card}
        // useNavigation équivalent — navigation.navigate() remplace <Link>
        onPress={() =>
          navigation.navigate('SlotList', {
            doctorId: item.id,
            doctorName: `${item.firstName} ${item.lastName}`,
          })
        }
      >
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {item.firstName[0]}{item.lastName[0]}
          </Text>
        </View>
        <View style={styles.info}>
          <Text style={styles.name}>
            Dr. {item.firstName} {item.lastName}
          </Text>
          <Text style={styles.speciality}>{item.speciality}</Text>
          <Text style={styles.dept}>
            {item.department} · {item.experienceYears} ans d'expérience
          </Text>
        </View>
        <Text style={styles.chevron}>›</Text>
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      {/* En-tête avec prénom et bouton déconnexion */}
      <View style={styles.header}>
        <Text style={styles.welcome}>
          Bonjour, {user?.firstName} 👋
        </Text>
        <TouchableOpacity onPress={logout}>
          <Text style={styles.logoutBtn}>Déconnexion</Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.sectionTitle}>
        {doctors.length} médecin{doctors.length > 1 ? 's' : ''} disponible{doctors.length > 1 ? 's' : ''}
      </Text>

      {/* FlatList = équivalent RN de .map() sur un tableau */}
      <FlatList
        data={doctors}
        keyExtractor={(d) => String(d.id)}
        renderItem={renderDoctor}
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
  center: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 14,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  welcome: {
    fontSize: 15,
    fontWeight: '600',
    color: '#111827',
  },
  logoutBtn: {
    fontSize: 14,
    color: '#dc2626',
    fontWeight: '600',
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#6b7280',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 8,
  },
  list: {
    padding: 16,
    paddingTop: 0,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 14,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#dbeafe',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  avatarText: {
    fontSize: 16,
    fontWeight: '700',
    color: '#2563eb',
  },
  info: {
    flex: 1,
  },
  name: {
    fontSize: 15,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 2,
  },
  speciality: {
    fontSize: 13,
    color: '#2563eb',
    fontWeight: '600',
    marginBottom: 2,
  },
  dept: {
    fontSize: 12,
    color: '#9ca3af',
  },
  chevron: {
    fontSize: 22,
    color: '#d1d5db',
    marginLeft: 8,
  },
  errorText: {
    margin: 24,
    textAlign: 'center',
    color: '#dc2626',
  },
});
