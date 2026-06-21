/*
 * Products endpoints — bound to docs/api/products-openapi.yaml.
 *
 * Catalogue read returns PagedResponseProductResponse:
 *   { content: ProductResponse[], page, size, totalElements, totalPages }
 * ProductResponse: { id, name, sku, description, categoryId(int), price, stock,
 *   weightKg, active }.  NOTE categoryId is an int FK — join getCategories() for
 *   a label (do NOT expect a category string).
 * CSV import: POST multipart → 202 { importId }; poll GET import/{id} →
 *   { importId, status(PROCESSING|COMPLETED), totalRows, imported, updated,
 *     skipped, errors[{ row, field, value, reason }], durationMs }.
 */
import { api } from "./client";

function toQuery(params = {}) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") q.append(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : "";
}

// Public catalogue: q, category, minPrice, maxPrice, inStock, page, size, sort.
export const getProducts = (params) => api.get(`/products${toQuery(params)}`, { auth: false });
export const getProduct = (id) => api.get(`/products/${id}`, { auth: false });
export const getCategories = () => api.get(`/categories`, { auth: false });

// Admin CRUD [ADMIN].
export const createProduct = (body) => api.post(`/products`, body);
export const replaceProduct = (id, body) => api.put(`/products/${id}`, body);
export const updateProduct = (id, body) => api.patch(`/products/${id}`, body);
export const deleteProduct = (id) => api.del(`/products/${id}`);

// CSV import [ADMIN]. `file` is a File/Blob; sent as multipart.
export function startImport(file) {
  const form = new FormData();
  form.append("file", file);
  return api.post(`/products/import`, form, { isForm: true });
}
export const getImport = (importId) => api.get(`/products/import/${importId}`);
