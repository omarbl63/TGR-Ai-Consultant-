import React, { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator, Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, Text, TextInput, View,
} from 'react-native';

const AI_LOGO = require('../../assets/ai.png');
const TGR_LOGO = require('../../assets/tgr-logo.png');
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import * as DocumentPicker from 'expo-document-picker';
import { ChatApi, RequestApi } from '../api/services';
import { ChatMessage } from '../types';
import { MarkdownText } from '../components/ui';
import { RootStackParamList } from '../navigation/types';

let idSeq = 0;
const nextId = () => `m${++idSeq}`;

type DocPhase = { requestId: string; documents: string[] };

export default function ChatScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [conversationId, setConversationId] = useState<string | undefined>();
  const [docPhase, setDocPhase] = useState<DocPhase | null>(null);
  const [attachedDocs, setAttachedDocs] = useState<Record<string, string>>({});
  const [uploadingDoc, setUploadingDoc] = useState<string | null>(null);
  const scrollRef = useRef<ScrollView>(null);

  useEffect(() => {
    const t = setTimeout(() => scrollRef.current?.scrollToEnd({ animated: true }), 80);
    return () => clearTimeout(t);
  }, [messages]);

  const send = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || sending) return;
    setInput('');
    const pendingId = nextId();
    setMessages((prev) => [
      ...prev,
      { id: nextId(), role: 'user', text: trimmed },
      { id: pendingId, role: 'assistant', text: '', pending: true },
    ]);
    setSending(true);
    try {
      const res = await ChatApi.send(trimmed, conversationId);
      setConversationId(res.conversationId);

      // Enter / leave the document-collection phase.
      if (res.awaitingDocuments && res.createdRequestId) {
        setDocPhase({ requestId: res.createdRequestId, documents: res.requiredDocuments ?? [] });
        setAttachedDocs({});
      } else {
        setDocPhase(null);
      }

      setMessages((prev) =>
        prev.map((m) =>
          m.id === pendingId
            ? {
                id: m.id,
                role: 'assistant',
                text: res.reply,
                requestId: res.createdRequestId,
                requestReference: res.requestReference,
                recommendation: res.recommendation,
                citations: res.citations,
                // Doc-phase actions live in the persistent panel, not as chat chips.
                choices: res.awaitingDocuments ? undefined : res.choices,
              }
            : m,
        ),
      );
    } catch (e: any) {
      setMessages((prev) =>
        prev.map((m) =>
          m.id === pendingId
            ? { id: m.id, role: 'assistant', text: `⚠️ ${e?.message ?? 'Erreur de communication.'}` }
            : m,
        ),
      );
    } finally {
      setSending(false);
    }
  };

  const attach = async (documentLabel: string) => {
    if (!docPhase || uploadingDoc) return;
    const result = await DocumentPicker.getDocumentAsync({ copyToCacheDirectory: true, multiple: false });
    if (result.canceled || !result.assets?.length) return;
    const asset = result.assets[0];
    setUploadingDoc(documentLabel);
    try {
      await RequestApi.uploadAttachment(
        docPhase.requestId,
        { uri: asset.uri, name: asset.name, type: asset.mimeType ?? undefined, file: (asset as any).file },
        documentLabel,
      );
      setAttachedDocs((prev) => ({ ...prev, [documentLabel]: asset.name }));
    } catch (e: any) {
      setMessages((prev) => [
        ...prev,
        { id: nextId(), role: 'assistant', text: `⚠️ ${e?.message ?? 'Échec du téléversement.'}` },
      ]);
    } finally {
      setUploadingDoc(null);
    }
  };

  const finalize = (text: string) => {
    setDocPhase(null);
    send(text);
  };

  const reset = () => {
    setMessages([]);
    setConversationId(undefined);
    setDocPhase(null);
    setAttachedDocs({});
  };

  return (
    <SafeAreaView className="flex-1 bg-ink-50" edges={['top']}>
      {/* Header */}
      <View className="flex-row items-center justify-between border-b border-ink-100 bg-white px-5 py-3">
        <View className="flex-row items-center gap-3">
          <View className="h-14.5 w-14.5 items-center justify-center rounded-xl border border-ink-200 bg-white">
            <Image source={TGR_LOGO} style={{ width: 80, height: 80 }} resizeMode="contain" />
          </View>
          <View>
            <Text className="text-base font-semibold text-ink-900">Assistant administratif</Text>
            <Text className="text-xs text-ink-400">Congés · Missions · Frais</Text>
          </View>
        </View>
        {messages.length > 0 && (
          <Pressable onPress={reset} className="rounded-lg px-3 py-1.5 active:bg-ink-100">
            <Text className="text-base font-medium text-brand-600">Nouveau</Text>
          </Pressable>
        )}
      </View>

      <KeyboardAvoidingView className="flex-1" behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView ref={scrollRef} className="flex-1" contentContainerClassName="px-4 py-4 gap-3">
          {messages.length === 0 ? (
            <View className="mt-16 items-center px-4">
              <View className="h-24 w-24 items-center justify-center rounded-3xl border border-ink-200 bg-white">
                <Image source={AI_LOGO} style={{ width: 68, height: 68 }} resizeMode="contain" />
              </View>
              <Text className="mt-5 text-center text-xl font-semibold text-ink-900">Comment puis-je vous aider ?</Text>
              <Text className="mt-2 text-center text-base leading-6 text-ink-500">
                Souhaitez-vous soumettre une demande ou obtenir une information ?
              </Text>
              <View className="mt-6 w-full gap-3">
                <Pressable
                  onPress={() => send('Soumettre une demande')}
                  className="h-12 items-center justify-center rounded-xl bg-brand-600 active:bg-brand-700"
                >
                  <Text className="text-base font-semibold text-white">Soumettre une demande</Text>
                </Pressable>
                <Pressable
                  onPress={() => send('Obtenir une information')}
                  className="h-12 items-center justify-center rounded-xl border border-brand-300 bg-white active:bg-brand-50"
                >
                  <Text className="text-base font-semibold text-brand-700">Obtenir une information</Text>
                </Pressable>
              </View>
            </View>
          ) : (
            messages.map((m, i) => (
              <Bubble
                key={m.id}
                m={m}
                isLast={i === messages.length - 1}
                onOpen={(id) => navigation.navigate('RequestDetail', { id })}
                onChoice={(text) => send(text)}
              />
            ))
          )}
        </ScrollView>

        {/* Document-collection panel (persistent while a draft awaits its documents) */}
        {docPhase && (
          <View className="border-t border-ink-100 bg-white px-4 pb-2 pt-3">
            <Text className="mb-2 text-xs font-semibold uppercase text-ink-400">
              Pièces à joindre (facultatif)
            </Text>
            <View style={{ maxHeight: 180 }}>
              <ScrollView>
                <View className="gap-2">
                  {docPhase.documents.map((doc) => {
                    const attached = attachedDocs[doc];
                    const busy = uploadingDoc === doc;
                    return (
                      <View
                        key={doc}
                        className="flex-row items-center justify-between rounded-xl border border-ink-200 bg-ink-50 px-3 py-2"
                      >
                        <View className="flex-1 pr-2">
                          <Text className="text-sm text-ink-700" numberOfLines={2}>{doc}</Text>
                          {attached && (
                            <Text className="mt-0.5 text-xs text-emerald-600" numberOfLines={1}>
                              ✓ {attached}
                            </Text>
                          )}
                        </View>
                        <Pressable
                          onPress={() => attach(doc)}
                          disabled={!!uploadingDoc}
                          className={`min-w-[84px] items-center rounded-lg px-3 py-2 ${attached ? 'bg-emerald-50' : 'bg-brand-600'}`}
                        >
                          {busy ? (
                            <ActivityIndicator size="small" color={attached ? '#059669' : '#fff'} />
                          ) : (
                            <Text className={`text-xs font-semibold ${attached ? 'text-emerald-700' : 'text-white'}`}>
                              {attached ? 'Remplacer' : 'Joindre'}
                            </Text>
                          )}
                        </Pressable>
                      </View>
                    );
                  })}
                </View>
              </ScrollView>
            </View>

            <View className="mt-3 flex-row gap-2">
              <Pressable
                onPress={() => finalize('Envoyer la demande')}
                disabled={sending || !!uploadingDoc}
                className="h-11 flex-1 items-center justify-center rounded-xl bg-brand-600"
                style={{ opacity: sending || uploadingDoc ? 0.6 : 1 }}
              >
                <Text className="text-sm font-semibold text-white">Envoyer la demande</Text>
              </Pressable>
              <Pressable
                onPress={() => finalize("Je n'ai pas ces pièces")}
                disabled={sending || !!uploadingDoc}
                className="h-11 items-center justify-center rounded-xl border border-ink-300 px-4"
              >
                <Text className="text-sm font-medium text-ink-600">Aucune pièce</Text>
              </Pressable>
            </View>
          </View>
        )}

        {/* Input bar */}
        <View className="border-t border-ink-100 bg-white px-3 py-2">
          <View className="flex-row items-end gap-2">
            <TextInput
              className="max-h-32 flex-1 rounded-2xl bg-ink-100 px-4 py-3 text-[17px] text-ink-900"
              value={input}
              onChangeText={setInput}
              placeholder="Écrivez votre message…"
              placeholderTextColor="#94a3b8"
              multiline
            />
            <Pressable
              onPress={() => send(input)}
              disabled={sending || !input.trim()}
              className="h-11 w-11 items-center justify-center rounded-full bg-brand-600"
              style={{ opacity: sending || !input.trim() ? 0.5 : 1 }}
            >
              <Ionicons name="arrow-up" size={22} color="#fff" />
            </Pressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Bubble({
  m,
  isLast,
  onOpen,
  onChoice,
}: {
  m: ChatMessage;
  isLast: boolean;
  onOpen: (id: string) => void;
  onChoice: (text: string) => void;
}) {
  if (m.role === 'user') {
    return (
      <View className="max-w-[85%] self-end rounded-2xl rounded-br-sm bg-brand-600 px-4 py-3">
        <Text className="text-[17px] leading-7 text-white">{m.text}</Text>
      </View>
    );
  }
  return (
    <View className="max-w-[88%] self-start">
      <View className="rounded-2xl rounded-bl-sm border border-ink-100 bg-white px-4 py-3">
        {m.pending ? (
          <View className="flex-row items-center gap-2">
            <ActivityIndicator size="small" color="#f07f17" />
            <Text className="text-sm text-ink-400">L’assistant réfléchit…</Text>
          </View>
        ) : (
          <MarkdownText text={m.text} className="text-[17px] leading-7 text-ink-800" />
        )}
      </View>

      {m.requestReference && (
        <Pressable
          onPress={() => m.requestId && onOpen(m.requestId)}
          className="mt-2 flex-row items-center justify-between rounded-2xl border border-brand-200 bg-brand-50 px-4 py-3 active:bg-brand-100"
        >
          <View>
            <Text className="font-mono text-xs font-semibold text-brand-700">{m.requestReference}</Text>
            <Text className="mt-0.5 text-sm font-medium text-brand-700">Voir la demande</Text>
          </View>
          <Ionicons name="chevron-forward" size={18} color="#b4550b" />
        </Pressable>
      )}

      {isLast && m.choices && m.choices.length > 0 && (
        <View className="mt-3 flex-row flex-wrap gap-2">
          {m.choices.map((c) => (
            <Pressable
              key={c}
              onPress={() => onChoice(c)}
              className="rounded-full border border-brand-300 bg-white px-4 py-2 active:bg-brand-50"
            >
              <Text className="text-sm font-medium text-brand-700">{c}</Text>
            </Pressable>
          ))}
        </View>
      )}
    </View>
  );
}
