import React from 'react';
import { ActivityIndicator, View } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from './types';
import LoginScreen from '../screens/LoginScreen';
import HomeScreen from '../screens/HomeScreen';
import ChatScreen from '../screens/ChatScreen';
import RequestsScreen from '../screens/RequestsScreen';
import RequestDetailScreen from '../screens/RequestDetailScreen';
import ProfileScreen from '../screens/ProfileScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator();

type IoniconName = keyof typeof Ionicons.glyphMap;

function Tabs() {
  const icon = (name: IoniconName) =>
    ({ color, size }: { color: string; size: number }) => <Ionicons name={name} size={size} color={color} />;

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#d96c0c',
        tabBarInactiveTintColor: '#94a3b8',
        tabBarStyle: { borderTopColor: '#e2e8f0', height: 62, paddingBottom: 8, paddingTop: 6 },
        tabBarLabelStyle: { fontSize: 12 },
      }}
    >
      <Tab.Screen name="Accueil" component={HomeScreen} options={{ tabBarIcon: icon('home-outline') }} />
      <Tab.Screen name="Assistant" component={ChatScreen} options={{ tabBarIcon: icon('chatbubbles-outline') }} />
      <Tab.Screen name="Demandes" component={RequestsScreen} options={{ tabBarIcon: icon('document-text-outline') }} />
      <Tab.Screen name="Profil" component={ProfileScreen} options={{ tabBarIcon: icon('person-outline') }} />
    </Tab.Navigator>
  );
}

export default function RootNavigator() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f8fafc' }}>
        <ActivityIndicator color="#f07f17" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      {user ? (
        <Stack.Navigator>
          <Stack.Screen name="Main" component={Tabs} options={{ headerShown: false }} />
          <Stack.Screen
            name="RequestDetail"
            component={RequestDetailScreen}
            options={{ title: 'Demande', headerTintColor: '#d96c0c' }}
          />
        </Stack.Navigator>
      ) : (
        <LoginScreen />
      )}
    </NavigationContainer>
  );
}
