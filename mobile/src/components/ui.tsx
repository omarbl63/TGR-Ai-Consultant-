import React from 'react';
import { Text, View } from 'react-native';

/** Renders text with inline **bold** support and preserved line breaks. */
export function MarkdownText({ text, className }: { text: string; className?: string }) {
  const cleaned = text.replace(/^#{1,6}\s+/gm, ''); // drop markdown heading hashes
  const parts = cleaned.split(/(\*\*[^*]+\*\*)/g);
  return (
    <Text className={className}>
      {parts.map((p, i) =>
        p.startsWith('**') && p.endsWith('**') ? (
          <Text key={i} className="font-semibold">
            {p.slice(2, -2)}
          </Text>
        ) : (
          p
        ),
      )}
    </Text>
  );
}

/** Small pill badge. `tone` carries both bg and text color classes. */
export function Badge({ label, tone }: { label: string; tone: string }) {
  return (
    <View className={`self-start rounded-full px-2.5 py-1 ${tone}`}>
      <Text className={`text-xs font-medium ${tone}`}>{label}</Text>
    </View>
  );
}
