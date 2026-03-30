#!/bin/bash
set -euo pipefail

DB_NAME="areaservicedb"
COLLECTION_NAME="areaservicedbcollection"
SEED_FILE="/docker-entrypoint-initdb.d/areas.seed.json"

existing_count="$(mongosh --quiet --eval "db.getSiblingDB('${DB_NAME}').getCollection('${COLLECTION_NAME}').countDocuments({})")"

if [ "${existing_count}" = "0" ]; then
  echo "[mongo-area-init] Importing seed data from ${SEED_FILE}"
  mongoimport \
    --db "${DB_NAME}" \
    --collection "${COLLECTION_NAME}" \
    --jsonArray \
    --file "${SEED_FILE}"
  echo "[mongo-area-init] Seed import completed"
else
  echo "[mongo-area-init] Collection ${DB_NAME}.${COLLECTION_NAME} already has data, skipping seed"
fi

