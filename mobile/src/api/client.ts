import { API_BASE } from '../config';

let authToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      ...(options.headers ?? {}),
    },
  });

  if (!res.ok) {
    let message = 'Une erreur est survenue.';
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      // non-JSON error body
    }
    throw new ApiError(res.status, message);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

/** Multipart upload (e.g. a supporting document). Lets fetch set the boundary. */
export async function uploadFile<T>(
  path: string,
  file: {
    uri: string;
    name: string;
    type?: string;
    file?: File;
  },
  label?: string,
): Promise<T> {
  const form = new FormData();

  if (file.file) {
  // Expo Web
  form.append('file', file.file);
} else {
  // Android / iOS
  form.append('file', {
    uri: file.uri,
    name: file.name,
    type: file.type ?? 'application/octet-stream',
  } as any);
}

  if (label) {
    form.append('label', label);
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    },
    body: form,
  });

  if (!res.ok) {
    let message = 'Échec du téléversement.';
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      // ignore
    }
    throw new ApiError(res.status, message);
  }

  return (await res.json()) as T;
}