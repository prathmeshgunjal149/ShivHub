import { API_BASE_URL as API_ORIGIN } from "../services/api";

export const resolveImageUrl = (value) => {
  if (!value) return "";
  const raw = typeof value === "string" ? value : value.imageUrl || "";
  if (!raw) return "";
  if (/^https?:\/\//i.test(raw)) return raw;
  return `${API_ORIGIN}/${raw.replace(/^\/+/, "")}`;
};

export const getPrimaryProductImage = (product) => {
  if (!product) return "";
  const images = Array.isArray(product.images) ? product.images : [];
  const primary = images.find((image) => image?.primaryImage || image?.primary);
  return resolveImageUrl(primary || images[0] || product.imageUrl);
};
