import React, { useState } from 'react';
import {
  ActivityIndicator, Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, Text, TextInput, View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../context/AuthContext';

type Mode = 'login' | 'register';

export default function LoginScreen() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<Mode>('login');
  const [email, setEmail] = useState('employe@adminai.ma');
  const [password, setPassword] = useState('Password123!');
  const [fullName, setFullName] = useState('');
  const [department, setDepartment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isRegister = mode === 'register';

  const switchMode = (m: Mode) => {
    setMode(m);
    setError(null);
    if (m === 'register') {
      setEmail('');
      setPassword('');
    } else {
      setEmail('employe@adminai.ma');
      setPassword('Password123!');
    }
  };

  const submit = async () => {
    if (!email || !password || (isRegister && !fullName)) return;
    setLoading(true);
    setError(null);
    try {
      if (isRegister) {
        await register({
          email: email.trim(),
          password,
          fullName: fullName.trim(),
          department: department.trim() || undefined,
        });
      } else {
        await login(email.trim(), password);
      }
    } catch (e: any) {
      setError(e?.message ?? 'Une erreur est survenue.');
      setLoading(false);
    }
  };

  return (
    <SafeAreaView className="flex-1 bg-white">
      <KeyboardAvoidingView className="flex-1" behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerClassName="flex-grow pb-10" keyboardShouldPersistTaps="handled">
          {/* Header */}
          <View className="bg-ink-900 px-6 pb-10 pt-16 rounded-b-[32px]">
            <View className="h-16 w-16 items-center justify-center rounded-2xl bg-white p-2">
              <Image
                source={require('../../assets/tgr-logo.png')}
                style={{ width: 48, height: 48 }}
                resizeMode="contain"
              />
            </View>
            <Text className="mt-5 text-2xl font-bold leading-8 text-white">Trésorerie Générale{'\n'}du Royaume</Text>
            <Text className="mt-3 text-base leading-6 text-ink-300">
              Votre assistant administratif. Décrivez votre besoin en langage naturel, l'IA s'occupe du reste.
            </Text>
          </View>

          {/* Form */}
          <View className="px-6 pt-8">
            <Text className="text-xl font-bold text-ink-900">{isRegister ? 'Créer un compte' : 'Connexion'}</Text>
            <Text className="mb-6 mt-1 text-sm text-ink-500">
              {isRegister ? 'Renseignez vos informations pour commencer.' : 'Accédez à votre espace employé.'}
            </Text>

            {isRegister && (
              <>
                <Text className="mb-1.5 text-sm font-medium text-ink-700">Nom complet</Text>
                <TextInput
                  className="mb-4 rounded-xl border border-ink-200 bg-white px-4 py-3 text-base text-ink-900"
                  value={fullName}
                  onChangeText={setFullName}
                  placeholder="Prénom Nom"
                  placeholderTextColor="#94a3b8"
                />
                <Text className="mb-1.5 text-sm font-medium text-ink-700">Direction / Service (facultatif)</Text>
                <TextInput
                  className="mb-4 rounded-xl border border-ink-200 bg-white px-4 py-3 text-base text-ink-900"
                  value={department}
                  onChangeText={setDepartment}
                  placeholder="Ex : Direction des Ressources Humaines"
                  placeholderTextColor="#94a3b8"
                />
              </>
            )}

            <Text className="mb-1.5 text-sm font-medium text-ink-700">Adresse e-mail</Text>
            <TextInput
              className="rounded-xl border border-ink-200 bg-white px-4 py-3 text-base text-ink-900"
              value={email}
              onChangeText={setEmail}
              autoCapitalize="none"
              keyboardType="email-address"
              placeholder="prenom.nom@tgr.ma"
              placeholderTextColor="#94a3b8"
            />

            <Text className="mb-1.5 mt-4 text-sm font-medium text-ink-700">Mot de passe</Text>
            <TextInput
              className="rounded-xl border border-ink-200 bg-white px-4 py-3 text-base text-ink-900"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              placeholder="••••••••"
              placeholderTextColor="#94a3b8"
            />

            {error && (
              <View className="mt-4 rounded-xl bg-rose-50 px-4 py-3">
                <Text className="text-sm text-rose-700">{error}</Text>
              </View>
            )}

            <Pressable
              onPress={submit}
              disabled={loading}
              className="mt-6 h-12 flex-row items-center justify-center rounded-xl bg-brand-600 active:bg-brand-700"
              style={{ opacity: loading ? 0.7 : 1 }}
            >
              {loading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text className="text-base font-semibold text-white">{isRegister ? 'Créer mon compte' : 'Se connecter'}</Text>
              )}
            </Pressable>

            {/* Toggle */}
            <View className="mt-5 flex-row justify-center">
              <Text className="text-sm text-ink-500">{isRegister ? 'Déjà un compte ? ' : 'Pas encore de compte ? '}</Text>
              <Pressable onPress={() => switchMode(isRegister ? 'login' : 'register')}>
                <Text className="text-sm font-semibold text-brand-600">
                  {isRegister ? 'Se connecter' : 'Créer un compte'}
                </Text>
              </Pressable>
            </View>

            {!isRegister && (
              <View className="mt-8 rounded-2xl bg-ink-50 p-4">
                <Text className="text-xs font-medium text-ink-500">Compte de démonstration</Text>
                <Text className="mt-1 text-xs text-ink-600">employe@adminai.ma — mot de passe : Password123!</Text>
              </View>
            )}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
