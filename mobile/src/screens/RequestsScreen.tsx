import React, { useCallback, useState } from 'react';
import { FlatList, Pressable, RefreshControl, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RequestApi } from '../api/services';
import { RequestSummary } from '../types';
import { Badge } from '../components/ui';
import { formatDate, statusBadge, statusLabel, typeIcon, typeLabel } from '../labels';
import { RootStackParamList } from '../navigation/types';

export default function RequestsScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [items, setItems] = useState<RequestSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      setItems(await RequestApi.mine());
    } catch {
      // keep previous
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Live updates: refresh on focus and poll every 15s while the screen is visible,
  // so a decision taken on the dashboard appears here without a manual refresh.
  useFocusEffect(
    useCallback(() => {
      load();
      const timer = setInterval(load, 15000);
      return () => clearInterval(timer);
    }, [load]),
  );

  return (
    <SafeAreaView className="flex-1 bg-ink-50" edges={['top']}>
      <View className="border-b border-ink-100 bg-white px-10 py-5">
        <Text className="text-xl font-semibold text-ink-900">Mes demandes</Text>
        <Text className="text-sm text-ink-400">Suivi de vos demandes administratives</Text>
      </View>

      <FlatList
        data={items}
        keyExtractor={(r) => r.id}
        contentContainerClassName="p-4 gap-3"
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />
        }
        ListEmptyComponent={
          !loading ? (
            <View className="mt-24 items-center">
              <Ionicons name="file-tray-outline" size={40} color="#94a3b8" />
              <Text className="mt-3 text-base font-medium text-ink-700">Aucune demande</Text>
              <Text className="mt-1 text-sm text-ink-400">Créez-en une depuis l’assistant.</Text>
            </View>
          ) : null
        }
        renderItem={({ item }) => (
          <Pressable
            onPress={() => navigation.navigate('RequestDetail', { id: item.id })}
            className="rounded-2xl border border-ink-100 bg-white p-4 active:bg-ink-50"
          >
            <View className="flex-row items-center justify-between">
              <View className="flex-row items-center gap-2">
                <Ionicons name={typeIcon[item.type] as any} size={17} color="#64748b" />
                <Text className="text-sm text-ink-600">{typeLabel[item.type]}</Text>
              </View>
              <Badge label={statusLabel[item.status]} tone={statusBadge[item.status]} />
            </View>
            <Text className="mt-2 text-lg font-semibold text-ink-900" numberOfLines={1}>{item.title}</Text>
            <View className="mt-2 flex-row items-center justify-between">
              <Text className="font-mono text-xs text-ink-400">{item.reference}</Text>
              <Text className="text-xs text-ink-400">{formatDate(item.createdAt)}</Text>
            </View>
          </Pressable>
        )}
      />
    </SafeAreaView>
  );
}
