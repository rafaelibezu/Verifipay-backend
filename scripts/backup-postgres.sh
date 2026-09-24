#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
backup_dir=${BACKUP_DIR:-"$project_dir/backups"}
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="$backup_dir/verifipay_${timestamp}.dump"
temporary_file="$backup_file.tmp.$$"

mkdir -p "$backup_dir"
trap 'rm -f "$temporary_file"' EXIT HUP INT TERM
cd "$project_dir"

if ! docker compose exec -T postgres sh -ec '
  : "${POSTGRES_USER:?POSTGRES_USER is required}"
  : "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
  : "${POSTGRES_DB:?POSTGRES_DB is required}"
  exec env PGPASSWORD="$POSTGRES_PASSWORD" pg_dump \
    -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc
' > "$temporary_file"; then
  echo "Backup failed; no dump was created." >&2
  exit 1
fi

mv "$temporary_file" "$backup_file"
trap - EXIT HUP INT TERM
printf 'Backup created: %s\n' "$backup_file"
