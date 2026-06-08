export function getBackendBaseUrl() {
  return (
    process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    process.env.BACKEND_BASE_URL ||
    "http://localhost:8080"
  ).replace(/\/$/, "");
}
