import React from 'react';
import { ActivityIndicator, View } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../context/AuthContext';
import { LoginScreen } from '../screens/LoginScreen';
import { DoctorListScreen } from '../screens/DoctorListScreen';
import { SlotListScreen } from '../screens/SlotListScreen';

// Typage des paramètres de chaque route (TypeScript strict)
export type RootStackParamList = {
  Login: undefined;
  DoctorList: undefined;
  SlotList: { doctorId: number; doctorName: string };
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function AppNavigator() {
  const { user, loading } = useAuth();

  // Affiche un spinner pendant la restauration de session au démarrage
  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" color="#2563eb" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: '#2563eb' },
          headerTintColor: '#fff',
          headerTitleStyle: { fontWeight: '700' },
        }}
      >
        {/* Navigation conditionnelle selon l'état d'authentification */}
        {user ? (
          <>
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
          </>
        ) : (
          <Stack.Screen
            name="Login"
            component={LoginScreen}
            options={{ headerShown: false }}
          />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
