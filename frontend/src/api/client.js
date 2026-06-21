/*
 * Core API client — the single fetch wrapper every endpoint module uses.
 *
 * Binds to the frozen Gateway REST surface (docs/api/*-openapi.yaml). Behaviours
 * required by the FE specs + journeys doc:
 *  - Bearer access token from the auth store on every request.
 *  - 401  → silently refresh once (POST /auth/refresh, httpOnly cookie) and retry;
 *           if refresh fails, clear the session and surface an auth error.
 *  - 429  → throw ApiError(429); UI shows a friendly rate-limit toast (don't retry).
 *  - Other non-2xx → throw ApiError with parsed body for field/status handling.
 *
 * Refresh-token cookie is httpOnly and browser-managed, so requests must send
 * credentials. Never log tokens or credentials.
 */
import { useAuthStore } from "@/store/auth";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";

export class ApiError extends Error {
  constructor(status, body, message) {
    super(message || (body && body.message) || `HTTP ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function parse(res) {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

let refreshInFlight = null;

// De-duped refresh: concurrent 401s share one refresh call.
function refreshOnce() {
  if (!refreshInFlight) {
    refreshInFlight = fetch(`${BASE_URL}/auth/refresh`, {
      method: "POST",
      credentials: "include",
    })
      .then(async (res) => {
        if (!res.ok) throw new ApiError(res.status, await parse(res));
        const data = await parse(res);
        useAuthStore.getState().setAccessToken(data?.accessToken ?? null);
        return data?.accessToken ?? null;
      })
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

/**
 * @param {string} path  e.g. "/products?page=0"
 * @param {object} opts  { method, body, headers, auth=true, isForm }
 */
export async function request(path, opts = {}) {
  const { method = "GET", body, headers = {}, auth = true, isForm = false } = opts;

  const send = async (token) => {
    const h = { ...headers };
    if (auth && token) h.Authorization = `Bearer ${token}`;
    let payload = body;
    if (body != null && !isForm) {
      h["Content-Type"] = "application/json";
      payload = JSON.stringify(body);
    }
    return fetch(`${BASE_URL}${path}`, {
      method,
      headers: h,
      body: payload,
      credentials: "include",
    });
  };

  const token = useAuthStore.getState().accessToken;
  let res = await send(token);

  // 401 → refresh once → retry.
  if (res.status === 401 && auth) {
    try {
      const newToken = await refreshOnce();
      res = await send(newToken);
    } catch {
      useAuthStore.getState().logout();
      throw new ApiError(401, null, "Session expired");
    }
  }

  if (res.status === 429) {
    throw new ApiError(429, await parse(res), "Too many requests");
  }
  if (!res.ok) {
    throw new ApiError(res.status, await parse(res));
  }
  if (res.status === 204) return null;
  return parse(res);
}

export const api = {
  get: (path, opts) => request(path, { ...opts, method: "GET" }),
  post: (path, body, opts) => request(path, { ...opts, method: "POST", body }),
  put: (path, body, opts) => request(path, { ...opts, method: "PUT", body }),
  patch: (path, body, opts) => request(path, { ...opts, method: "PATCH", body }),
  del: (path, opts) => request(path, { ...opts, method: "DELETE" }),
};
