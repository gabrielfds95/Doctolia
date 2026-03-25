import React from 'react';
import { ActivityIndicator, View, Text } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useAuth } from '../context/AuthContext';
import { LoginScreen } from '../screens/LoginScreen';
import { DoctorListScreen } from '../screens/DoctorListScreen';
import { SlotListScreen } from '../screens/SlotListScreen';
import { MesRdvScreen } from '../screens/MesRdvScreen';
import { ProfileScreen } from '../screens/ProfileScreen';

// ── Types de routes ───────────────────────────────────────────────────────────

// Garde la compatibilité avec les imports existants dans DoctorListScreen / SlotListScreen
export type RootStackParamList = {
  Login: undefined;
  DoctorList: undefined;
  SlotList: { doctorId: number; doctorName: string };
};

// Tabs du bas : visible une fois connecté
type MainTabParamList = {
  Médecins: undefined;
  'Mes RDV': undefined;
  Profil: undefined;
};

// ── Navigateurs ───────────────────────────────────────────────────────────────

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();

// Stack médecins → créneaux (imbriqué dans l'onglet "Médecins")
function HomeStack() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: '#2563eb' },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: '700' },
      }}
    >
      <Stack.Screen
        name="DoctorList"
        component={DoctorListScreen}
        options={{ title: 'Doctolia' }}
      />
      <Stack.Screen
        name="SlotList"
        component={SlotListScreen}
        options={({ route }) => ({ title: `Dr. ${route.params.doctorName}` })}
      />
    </Stack.Navigator>
  );
}

// Onglets principaux de l'app authentifiée
function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerStyle: { backgroundColor: '#2563eb' },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: '700' },
        tabBarActiveTintColor: '#2563eb',
        tabBarInactiveTintColor: '#9ca3af',
        // Label personnalisé — évite d'avoir à installer une lib d'icônes
        tabBarLabel: ({ color, focused }) => (
          <Text style={{ fontSize: 11, color, fontWeight: focused ? '700' : '400' }}>
            {route.name}
          </Text>
        ),
      })}
    >
      {/* Onglet 1 : liste des médecins + créneaux (stack imbriqué) */}
      <Tab.Screen
        name="Médecins"
        component={HomeStack}
        options={{ headerShown: false }}
      />
      {/* Onglet 2 : mes rendez-vous */}
      <Tab.Screen
        name="Mes RDV"
        component={MesRdvScreen}
        options={{ title: 'Mes rendez-vous' }}
      />
      {/* Onglet 3 : profil */}
      <Tab.Screen
        name="Profil"
        component={ProfileScreen}
        options={{ title: 'Mon profil' }}
      />
    </Tab.Navigator>
  );
}

// ── Navigateur racine ─────────────────────────────────────────────────────────

export function AppNavigator() {
  const { user, loading } = useAuth();

  // Spinner pendant la restauration de session au démarrage
  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" color="#2563eb" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      {user ? (
        // Utilisateur connecté → onglets
        <MainTabs />
      ) : (
        // Non connecté → écran de login uniquement
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          <Stack.Screen name="Login" component={LoginScreen} />
        </Stack.Navigator>
      )}
    </NavigationContainer>
  );
}
