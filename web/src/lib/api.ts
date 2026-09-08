export const apiBase = (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), path.endsWith('/train') ? 180_000 : 45_000);
  const abort = () => controller.abort();
  init.signal?.addEventListener('abort', abort, { once: true });
  if (init.signal?.aborted) controller.abort();
  try {
    const response = await fetch(`${apiBase}${path}`, { ...init, headers: { ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...init.headers }, signal: controller.signal });
    if (!response.ok) {
      const body = await response.json().catch(() => null);
      throw new Error(body?.message || `Não foi possível concluir a solicitação (${response.status}).`);
    }
    return response.status === 204 ? undefined as T : await response.json() as T;
  } catch (error) {
    if (init.signal?.aborted) throw error;
    if (controller.signal.aborted) throw new Error('A conexão demorou demais. Tente novamente.');
    if (error instanceof TypeError) throw new Error('Não foi possível conectar ao servidor. Verifique sua rede e se o computador está ligado.');
    throw error;
  } finally { clearTimeout(timeout); init.signal?.removeEventListener('abort', abort); }
}
export const jsonBody = (body: unknown, method = 'PUT'): RequestInit => ({ method, body: JSON.stringify(body) });
