/*
 * Auth endpoints — bound to docs/api/users-openapi.yaml.
 *
 * register → 201 AuthResponse; login → 200 AuthResponse:
 *   { accessToken, tokenType, user{ id, email, firstName, lastName, role } }.
 * Refresh token is set as an httpOnly cookie by the server (browser-managed);
 * the client never reads it. login 401 → ONE generic message (no enumeration);
 * register 409 → duplicate email. All calls send credentials (cookie).
 */
import { api } from "./client";

export const register = (body) => api.post(`/auth/register`, body, { auth: false });
export const login = (body) => api.post(`/auth/login`, body, { auth: false });
export const logout = () => api.post(`/auth/logout`, null, { auth: false });

// Profile [BUYER].
export const getMe = () => api.get(`/users/me`);
export const updateMe = (body) => api.patch(`/users/me`, body);
