#!/bin/sh
# Generates a self-signed TLS certificate for local development.
# Output: nginx/certs/server.crt and nginx/certs/server.key

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CERTS_DIR="${SCRIPT_DIR}/../nginx/certs"

mkdir -p "${CERTS_DIR}"

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "${CERTS_DIR}/server.key" \
    -out "${CERTS_DIR}/server.crt" \
    -subj "/CN=localhost/O=Meridian/C=US" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
    2>/dev/null

echo "Certificate generated:"
echo "  ${CERTS_DIR}/server.crt"
echo "  ${CERTS_DIR}/server.key"
