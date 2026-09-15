import React, { useEffect, useState } from 'react';
import { Alert, Platform, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../context/AuthContext';
import { LeaveApi } from '../api/services';
import { LeaveBalance } from '../types';

const ROLE_LABEL: Record<string, string> = {
  EMPLOYEE: 'Employé',
  MANAGER: 'Responsable',
  ADMIN: 'Administrateur',
};

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const [balance, setBalance] = useState<LeaveBalance | null>(null);

  useEffect(() => {
    LeaveApi.me().then(setBalance).catch(() => setBalance(null));
  }, []);

  const initials = (user?.fullName ?? '')
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  const confirmLogout = () => {
    if (Platform.OS === 'web') {
      logout();
      return;
    }
    Alert.alert('Déconnexion', 'Voulez-vous vous déconnecter ?', [
      { text: 'Annuler', style: 'cancel' },
      { text: 'Se déconnecter', style: 'destructive', onPress: () => logout() },
    ]);
  };

  return (
    <SafeAreaView className="flex-1 bg-ink-50" edges={['top']}>
      <View className="border-b border-ink-100 bg-white px-10 py-5">
        <Text className="text-lg font-semibold text-ink-900">Profil</Text>
        <Text className="text-xs text-ink-400">Vos informations et votre solde de congés</Text>
      </View>

      <ScrollView contentContainerClassName="p-4 gap-4">
        {/* Identity */}
        <View className="items-center rounded-2xl border border-ink-100 bg-white p-6">
          <View className="h-20 w-20 items-center justify-center rounded-full bg-brand-600">
            <Text className="text-2xl font-bold text-white">{initials || '👤'}</Text>
          </View>
          <Text className="mt-3 text-xl font-bold text-ink-900">{user?.fullName}</Text>
          <View className="mt-1 rounded-full bg-brand-50 px-3 py-1">
            <Text className="text-xs font-medium text-brand-700">{ROLE_LABEL[user?.role ?? ''] ?? user?.role}</Text>
          </View>
        </View>

        {/* Leave balance */}
        <View className="rounded-2xl border border-ink-100 bg-white p-5">
          <Text className="text-xs font-semibold uppercase text-ink-400">Solde de congés {balance ? `· ${balance.year}` : ''}</Text>
          {balance ? (
            <View className="mt-3 flex-row items-end justify-between">
              <View>
                <Text className="text-4xl font-bold text-brand-600">{Math.round(balance.remainingDays)}</Text>
                <Text className="text-sm text-ink-500">jours restants</Text>
              </View>
              <View className="items-end gap-1">
                <Text className="text-xs text-ink-500">Droit annuel : {Math.round(balance.entitledDays)} j</Text>
                <Text className="text-xs text-ink-500">Déjà pris : {Math.round(balance.usedDays)} j</Text>
              </View>
            </View>
          ) : (
            <Text className="mt-3 text-sm text-ink-400">Solde indisponible.</Text>
          )}
        </View>

        {/* Details */}
        <View className="rounded-2xl border border-ink-100 bg-white p-5">
          <Text className="mb-3 text-xs font-semibold uppercase text-ink-400">Informations</Text>
          <Row label="E-mail" value={user?.email} />
          <Row label="Direction / Service" value={user?.department} />
        </View>

        {/* Logout */}
        <Pressable
          onPress={confirmLogout}
          className="mt-2 h-12 flex-row items-center justify-center rounded-xl border border-rose-200 bg-rose-50 active:bg-rose-100"
        >
          <Text className="text-base font-semibold text-rose-600">Se déconnecter</Text>
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}

function Row({ label, value }: { label: string; value?: string }) {
  return (
    <View className="flex-row justify-between border-b border-ink-50 py-2.5">
      <Text className="text-sm text-ink-400">{label}</Text>
      <Text className="ml-4 flex-1 text-right text-sm font-medium text-ink-800">{value || '—'}</Text>
    </View>
  );
}
