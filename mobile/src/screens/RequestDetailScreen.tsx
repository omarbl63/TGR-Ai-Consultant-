import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Platform, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRoute, RouteProp } from '@react-navigation/native';
import { RequestApi } from '../api/services';
import { RequestDetail } from '../types';
import { Badge } from '../components/ui';
import { fieldName, statusBadge, statusLabel, typeIcon, typeLabel } from '../labels';
import { RootStackParamList } from '../navigation/types';

const eventLabel: Record<string, string> = {
  CREATED: 'Demande créée', SUBMITTED: 'Soumise pour validation', AI_ANALYZED: 'Analyse réalisée',
  APPROVED: 'Approuvée', REJECTED: 'Rejetée', CHANGES_REQUESTED: 'Modifications demandées', COMMENTED: 'Commentaire',
};

type DecisionStyle = { label: string; card: string; text: string; icon: any; iconColor: string };
const DECISION_META: Record<string, DecisionStyle> = {
  APPROVED: { label: 'Demande approuvée', card: 'border-emerald-200 bg-emerald-50', text: 'text-emerald-800', icon: 'checkmark-circle-outline', iconColor: '#047857' },
  REJECTED: { label: 'Demande rejetée', card: 'border-rose-200 bg-rose-50', text: 'text-rose-800', icon: 'close-circle-outline', iconColor: '#be123c' },
  CHANGES_REQUESTED: { label: 'Modifications demandées', card: 'border-amber-200 bg-amber-50', text: 'text-amber-800', icon: 'create-outline', iconColor: '#b45309' },
};

export default function RequestDetailScreen() {
  const route = useRoute<RouteProp<RootStackParamList, 'RequestDetail'>>();
  const { id } = route.params;
  const [detail, setDetail] = useState<RequestDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [resubmitting, setResubmitting] = useState(false);
  const [edits, setEdits] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    RequestApi.detail(id)
      .then(setDetail)
      .catch(() => setDetail(null))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  // Populate the editable fields when a request comes back for modification.
  useEffect(() => {
    if (detail?.status === 'CHANGES_REQUESTED') {
      const init: Record<string, string> = {};
      Object.entries(detail.structuredData ?? {}).forEach(([k, v]) => {
        init[k] = v == null ? '' : String(v);
      });
      setEdits(init);
    }
  }, [detail?.id, detail?.status]);

  const saveEdits = async () => {
    if (saving) return;
    setSaving(true);
    try {
      const updated = await RequestApi.update(id, edits);
      setDetail(updated);
      if (Platform.OS !== 'web') Alert.alert('Enregistré', 'Vos modifications ont été enregistrées.');
    } catch (e: any) {
      Alert.alert('Erreur', e?.message ?? "L'enregistrement a échoué.");
    } finally {
      setSaving(false);
    }
  };

  const resubmit = async () => {
    if (resubmitting) return;
    setResubmitting(true);
    try {
      const updated = await RequestApi.submit(id);
      setDetail(updated);
      const msg = 'Votre demande a été renvoyée pour validation.';
      if (Platform.OS === 'web') {
        // no native alert on web
      } else {
        Alert.alert('Demande renvoyée', msg);
      }
    } catch (e: any) {
      Alert.alert('Erreur', e?.message ?? "Le renvoi de la demande a échoué.");
    } finally {
      setResubmitting(false);
    }
  };

  if (loading) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center bg-ink-50">
        <ActivityIndicator color="#f07f17" />
      </SafeAreaView>
    );
  }
  if (!detail) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center bg-ink-50">
        <Text className="text-sm text-rose-600">Demande introuvable.</Text>
      </SafeAreaView>
    );
  }

  const a = detail.analysis;
  const data = Object.entries(detail.structuredData ?? {});
  const toFix = [...(a?.missingInfo ?? []), ...(a?.missingDocuments ?? [])];
  const decisionEvent = [...detail.timeline].reverse().find((e) => !!DECISION_META[e.type]);
  const meta = decisionEvent ? DECISION_META[decisionEvent.type] : null;

  return (
    <SafeAreaView className="flex-1 bg-ink-50" edges={['bottom']}>
      <ScrollView contentContainerClassName="p-4 gap-4">
        {/* Header */}
        <View className="rounded-2xl border border-ink-100 bg-white p-4">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center gap-2">
              <Ionicons name={typeIcon[detail.type] as any} size={18} color="#64748b" />
              <Text className="text-sm text-ink-600">{typeLabel[detail.type]}</Text>
            </View>
            <Badge label={statusLabel[detail.status]} tone={statusBadge[detail.status]} />
          </View>
          <Text className="mt-2 text-xl font-bold text-ink-900">{detail.title}</Text>
          <Text className="mt-1 font-mono text-xs text-ink-400">{detail.reference}</Text>
        </View>

        {/* Manager's decision + comment (visible to the employee) */}
        {meta && (
          <View className={`rounded-2xl border p-4 ${meta.card}`}>
            <View className="flex-row items-center gap-2">
              <Ionicons name={meta.icon} size={20} color={meta.iconColor} />
              <Text className={`text-sm font-semibold ${meta.text}`}>{meta.label}</Text>
            </View>
            <Text className={`mt-2 text-sm ${meta.text}`}>
              {decisionEvent?.comment
                ? `Réponse du responsable : « ${decisionEvent.comment} »`
                : 'Aucun commentaire du responsable.'}
            </Text>
            {detail.status === 'CHANGES_REQUESTED' && (
              <Pressable
                onPress={resubmit}
                disabled={resubmitting}
                className="mt-3 h-11 items-center justify-center rounded-xl bg-brand-600 active:bg-brand-700"
                style={{ opacity: resubmitting ? 0.6 : 1 }}
              >
                {resubmitting ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <Text className="text-sm font-semibold text-white">Renvoyer la demande</Text>
                )}
              </Pressable>
            )}
          </View>
        )}

        {/* Structured data — editable when the manager asked for changes */}
        {data.length > 0 && (
          <View className="rounded-2xl border border-ink-100 bg-white p-4">
            <Text className="mb-3 text-xs font-semibold uppercase text-ink-400">
              {detail.status === 'CHANGES_REQUESTED' ? 'Modifier les détails' : 'Détails'}
            </Text>
            {detail.status === 'CHANGES_REQUESTED' ? (
              <View className="gap-3">
                {data.map(([k]) => (
                  <View key={k}>
                    <Text className="mb-1 text-xs text-ink-400">{fieldName(k)}</Text>
                    <TextInput
                      value={edits[k] ?? ''}
                      onChangeText={(t) => setEdits((p) => ({ ...p, [k]: t }))}
                      className="rounded-xl bg-ink-100 px-3 py-2.5 text-base text-ink-900"
                      placeholderTextColor="#94a3b8"
                    />
                  </View>
                ))}
                <Pressable
                  onPress={saveEdits}
                  disabled={saving}
                  className="mt-1 h-11 items-center justify-center rounded-xl border border-brand-300 bg-brand-50 active:bg-brand-100"
                  style={{ opacity: saving ? 0.6 : 1 }}
                >
                  {saving ? (
                    <ActivityIndicator color="#d96c0c" size="small" />
                  ) : (
                    <Text className="text-sm font-semibold text-brand-700">Enregistrer les modifications</Text>
                  )}
                </Pressable>
              </View>
            ) : (
              <View className="gap-3">
                {data.map(([k, v]) => (
                  <View key={k} className="flex-row justify-between">
                    <Text className="text-sm text-ink-400">{fieldName(k)}</Text>
                    <Text className="ml-4 flex-1 text-right text-base font-medium text-ink-800">{String(v)}</Text>
                  </View>
                ))}
              </View>
            )}
          </View>
        )}

        {/* Actionable: what to provide */}
        {toFix.length > 0 && (
          <View className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
            <View className="flex-row items-center gap-2">
              <Ionicons name="information-circle-outline" size={20} color="#b45309" />
              <Text className="text-sm font-semibold text-amber-800">À compléter</Text>
            </View>
            <View className="mt-2 gap-1">
              {toFix.map((m, i) => (
                <Text key={i} className="text-sm text-amber-800">• {m}</Text>
              ))}
            </View>
          </View>
        )}

        {/* Timeline */}
        <View className="rounded-2xl border border-ink-100 bg-white p-4">
          <Text className="mb-3 text-xs font-semibold uppercase text-ink-400">Chronologie</Text>
          <View className="gap-3">
            {detail.timeline.map((e) => (
              <View key={e.id} className="flex-row gap-3">
                <View className="mt-1.5 h-2 w-2 rounded-full bg-brand-500" />
                <View className="flex-1">
                  <Text className="text-sm font-medium text-ink-800">{eventLabel[e.type] ?? e.type}</Text>
                  <Text className="text-xs text-ink-400">{e.actorName}</Text>
                  {e.comment ? <Text className="mt-0.5 text-xs italic text-ink-500">“{e.comment}”</Text> : null}
                </View>
              </View>
            ))}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
