import React, { useCallback, useState } from 'react';
import { Image, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { useAuth } from '../context/AuthContext';
import { LeaveApi, RequestApi } from '../api/services';
import { LeaveBalance, RequestSummary } from '../types';
import { statusBadge, statusLabel, typeIcon, typeLabel } from '../labels';

const QUICK: { icon: keyof typeof Ionicons.glyphMap; label: string; target: string }[] = [
  { icon: 'chatbubbles-outline', label: 'Parler à l’assistant', target: 'Assistant' },
  { icon: 'add-circle-outline', label: 'Nouvelle demande', target: 'Assistant' },
  { icon: 'document-text-outline', label: 'Mes demandes', target: 'Demandes' },
  { icon: 'person-outline', label: 'Mon profil', target: 'Profil' },
];

export default function HomeScreen() {
  const { user } = useAuth();
  const navigation = useNavigation<any>();
  const [balance, setBalance] = useState<LeaveBalance | null>(null);
  const [recent, setRecent] = useState<RequestSummary[]>([]);

  useFocusEffect(
    useCallback(() => {
      LeaveApi.me().then(setBalance).catch(() => {});
      RequestApi.mine().then((r) => setRecent(r.slice(0, 3))).catch(() => {});
    }, []),
  );

  const firstName = user?.fullName?.split(' ')[0] ?? '';

  return (
    <SafeAreaView className="flex-1 bg-ink-50" edges={['top']}>
      <ScrollView contentContainerClassName="pb-8">
        {/* Header */}
        <View className="bg-ink-900 px-5 pb-8 pt-6 rounded-b-3xl">
          <View className="flex-row items-center justify-between">
            <View>
              <Text className="text-base text-ink-300">Bonjour,</Text>
              <Text className="text-2xl font-bold text-white">{firstName}</Text>
            </View>
          </View>

          {/* Leave balance card */}
          <View className="mt-5 flex-row items-center justify-between rounded-2xl bg-white/10 p-4">
            <View>
              <Text className="text-xs text-ink-300">Solde de congés {balance ? `· ${balance.year}` : ''}</Text>
              <Text className="mt-1 text-3xl font-bold text-white">
                {balance ? Math.round(balance.remainingDays) : '—'}
                <Text className="text-base font-medium text-ink-300"> jours restants</Text>
              </Text>
            </View>
            <View className="h-12 w-12 items-center justify-center rounded-full bg-brand-500/20">
              <Ionicons name="sunny-outline" size={26} color="#fdb871" />
            </View>
          </View>
        </View>

        {/* Quick actions */}
        <View className="px-5 pt-6">
          <Text className="mb-3 text-sm font-semibold text-ink-700">Que souhaitez-vous faire ?</Text>
          <View className="flex-row flex-wrap justify-between gap-y-3">
            {QUICK.map((q) => (
              <Pressable
                key={q.label}
                onPress={() => navigation.navigate(q.target)}
                className="w-[48%] rounded-2xl border border-ink-100 bg-white p-4 active:bg-ink-50"
              >
                <View className="h-10 w-10 items-center justify-center rounded-xl bg-brand-50">
                  <Ionicons name={q.icon} size={22} color="#d96c0c" />
                </View>
                <Text className="mt-3 text-sm font-medium text-ink-800">{q.label}</Text>
              </Pressable>
            ))}
          </View>
        </View>

        {/* Recent requests */}
        <View className="px-5 pt-6">
          <View className="mb-3 flex-row items-center justify-between">
            <Text className="text-sm font-semibold text-ink-700">Demandes récentes</Text>
            <Pressable onPress={() => navigation.navigate('Demandes')}>
              <Text className="text-sm font-medium text-brand-600">Tout voir</Text>
            </Pressable>
          </View>

          {recent.length === 0 ? (
            <View className="items-center rounded-2xl border border-dashed border-ink-200 bg-white py-8">
              <Ionicons name="file-tray-outline" size={28} color="#94a3b8" />
              <Text className="mt-2 text-sm text-ink-500">Aucune demande pour l’instant.</Text>
              <Pressable onPress={() => navigation.navigate('Assistant')} className="mt-3 rounded-lg bg-brand-600 px-4 py-2">
                <Text className="text-sm font-semibold text-white">Créer une demande</Text>
              </Pressable>
            </View>
          ) : (
            <View className="gap-2.5">
              {recent.map((r) => (
                <Pressable
                  key={r.id}
                  onPress={() => navigation.navigate('RequestDetail', { id: r.id })}
                  className="flex-row items-center gap-3 rounded-2xl border border-ink-100 bg-white p-3.5 active:bg-ink-50"
                >
                  <View className="h-10 w-10 items-center justify-center rounded-xl bg-brand-50">
                    <Ionicons name={typeIcon[r.type] as any} size={20} color="#d96c0c" />
                  </View>
                  <View className="flex-1">
                    <Text className="text-sm font-medium text-ink-900" numberOfLines={1}>{r.title}</Text>
                    <Text className="text-xs text-ink-400">{typeLabel[r.type]} · {r.reference}</Text>
                  </View>
                  <View className={`rounded-full px-2.5 py-1 ${statusBadge[r.status]}`}>
                    <Text className={`text-xs font-medium ${statusBadge[r.status]}`}>{statusLabel[r.status]}</Text>
                  </View>
                </Pressable>
              ))}
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
