#!/usr/bin/env sh
set -eu

usage() {
  echo "Usage: $0 [--yes] <backup-file>" >&2
  exit 2
}

assume_yes=false
case "${1:-}" in
  --yes) assume_yes=true; shift ;;
esac
[ "$#" -eq 1 ] || usage
backup_file=$1
[ -f "$backup_file" ] || { echo "Backup file does not exist: $backup_file" >&2; exit 1; }

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
cd "$project_dir"

if ! docker compose exec -T postgres pg_restore --list < "$backup_file" > /dev/null; then
  echo "Backup archive is invalid or cannot be read by pg_restore." >&2
  exit 1
fi

if [ "$assume_yes" != true ]; then
  printf 'Restore %s into the existing PostgreSQL database? This replaces existing objects. [y/N] ' "$backup_file" >&2
  read -r answer
  case "$answer" in
    y|Y|yes|YES) ;;
    *) echo "Restore cancelled." >&2; exit 0 ;;
  esac
fi

docker compose exec -T postgres sh -ec '
  : "${POSTGRES_USER:?POSTGRES_USER is required}"
  : "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
  : "${POSTGRES_DB:?POSTGRES_DB is required}"
  exec env PGPASSWORD="$POSTGRES_PASSWORD" pg_restore \
    --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"
' < "$backup_file"
printf 'Restore completed from: %s\n' "$backup_file"
