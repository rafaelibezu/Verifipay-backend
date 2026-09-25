# Despliegue local tipo producción

Este Compose levanta PostgreSQL y el backend con el perfil `prod`. PostgreSQL
permanece únicamente en la red interna de Docker; para las pruebas locales, el
backend publica el puerto `8080` en el host.

## Preparación

En PowerShell:

```powershell
copy .env.example .env
```

Edite `.env` y asigne valores locales seguros a `POSTGRES_PASSWORD`,
`DB_PASSWORD` y `JWT_SECRET`. `DB_URL` debe conservar el host `postgres`, no
`localhost`, porque el backend se ejecuta dentro de un contenedor.

## Uso

Validar la interpolación y la configuración:

```powershell
docker compose config
```

Construir e iniciar:

```powershell
docker compose up -d --build
```

Ver estado:

```powershell
docker compose ps
```

Ver logs del backend:

```powershell
docker compose logs -f backend
```

Consultar health:

```text
http://localhost:8080/actuator/health
```

## Bootstrap inicial de PLATFORM_ADMIN

Use este procedimiento solo durante el primer despliegue de producción, cuando
todavía no existe ningún `PLATFORM_ADMIN`. No hay endpoint HTTP de bootstrap ni
credenciales por defecto.

1. Configure temporalmente en el `.env` de producción:

```env
PLATFORM_BOOTSTRAP_ENABLED=true
PLATFORM_BOOTSTRAP_EMAIL=
PLATFORM_BOOTSTRAP_NAME=
PLATFORM_BOOTSTRAP_PASSWORD=
```

Use un email real, un nombre operativo y una contraseña fuerte. No deje valores
vacíos cuando `PLATFORM_BOOTSTRAP_ENABLED=true`.

2. Arranque o recree el backend:

```powershell
docker compose up -d --build backend
```

3. Revise los logs del backend y confirme únicamente el mensaje de creación del
`PLATFORM_ADMIN`. Los logs no deben imprimir la contraseña ni su hash:

```powershell
docker compose logs --tail=100 backend
```

4. Verifique login con el `PLATFORM_ADMIN` creado.

5. Cambie inmediatamente el `.env` de producción:

```env
PLATFORM_BOOTSTRAP_ENABLED=false
```

6. Elimine `PLATFORM_BOOTSTRAP_PASSWORD` del `.env` de producción.

7. Recree o reinicie el backend:

```powershell
docker compose up -d --build backend
```

Recrear el contenedor es importante para que `PLATFORM_BOOTSTRAP_PASSWORD`
también desaparezca del environment del contenedor anterior.

8. Confirme que el bootstrap ya no ejecuta.

Advertencia: NO deje `PLATFORM_BOOTSTRAP_ENABLED=true` después del primer
provisionamiento.

Detener sin borrar datos:

```powershell
docker compose down
```

El volumen nombrado `verifipay_postgres_data` sobrevive a `docker compose down`.
Para reiniciar completamente el entorno local, incluida la base de datos:

```powershell
docker compose down -v
```

Flyway se ejecuta automáticamente al iniciar el backend y JPA conserva
`ddl-auto=validate`; Compose no ejecuta migraciones manuales.

El healthcheck del backend usa `bash` y el runtime de Java ya presentes en la
imagen Temurin para realizar una petición HTTP a `/actuator/health`, evitando
instalar `curl` o `wget` solo para esta comprobación.

Para una futura VPS, PostgreSQL seguirá sin exponerse; un reverse proxy será el
único componente público por los puertos 80/443.

## Operación y recuperación

El perfil `prod` usa cierre ordenado de Spring (`server.shutdown=graceful`) y
espera hasta 25 segundos por cada fase de cierre. Docker da al backend 30
segundos antes de forzar la detención; el `ENTRYPOINT` sigue en formato exec,
por lo que Java recibe la señal de parada directamente.

Los límites se definen con `cpus` y `mem_limit`, opciones aplicadas por Docker
Compose fuera de Swarm: backend 1.5 CPU/2 GB y PostgreSQL 1 CPU/2 GB. Se deja
margen en el VPS de 3 vCPU/8 GB para SO, Docker y el proxy futuro. Ambos
servicios conservan stdout/stderr y rotan `json-file` a cinco archivos de 10 MB.

Java detecta el límite del contenedor. `JAVA_TOOL_OPTIONS` configura un heap
inicial de 25% y máximo de 70%; puede ajustarse en `.env`, manteniéndolo por
debajo de los 2 GB del backend para reservar memoria no-heap.

### Backup

Desde la raíz del proyecto en Linux/Ubuntu:

```sh
sh ./scripts/backup-postgres.sh
```

El archivo custom de `pg_dump` queda en `./backups/` con timestamp UTC. Este
directorio está ignorado por Git. Para revisar que el dump no esté vacío y
listar su contenido:

```sh
ls -lh ./backups/
docker compose exec -T postgres pg_restore --list < ./backups/ARCHIVO.dump
```

En Windows se puede ejecutar el equivalente con PowerShell 7:

```powershell
.\scripts\backup-postgres.ps1
```

### Restore

La restauración es destructiva para los objetos existentes de la base destino:
usa `pg_restore --clean --if-exists --no-owner`, no ejecuta `DROP DATABASE` y
pide confirmación. Detenga el backend para evitar escrituras concurrentes:

```sh
docker compose stop backend
sh ./scripts/restore-postgres.sh ./backups/ARCHIVO.dump
docker compose start backend
curl http://localhost:8080/actuator/health
docker compose exec -T postgres sh -ec \
  'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

Use `--yes` solo para automatización ya revisada. En PowerShell 7:

```powershell
.\scripts\restore-postgres.ps1 -BackupPath .\backups\ARCHIVO.dump
```

### Persistencia, recreación y logs

`docker compose down` no borra la base: el volumen nombrado permanece. Nunca
use `docker compose down -v` salvo que quiera eliminar explícitamente todos los
datos. Para recrear contenedores conservando el volumen:

```sh
docker compose up -d --build
docker compose logs --tail=100 backend
docker compose logs -f backend
```

### Runbook corto

Si el backend cae: (1) `docker compose ps`; (2) `docker compose logs --tail=100 backend`;
(3) `docker compose restart backend`; (4) consulte `/actuator/health`.

Si PostgreSQL cae: (1) revise `docker compose logs --tail=100 postgres`; (2) no
borre el volumen; (3) `docker compose restart postgres`; (4) confirme el
health; (5) reinicie el backend si no recupera la conexión.

Si el volumen se pierde: cree el stack limpio con `docker compose up -d --build`,
restaure el último backup, revise `flyway_schema_history`, confirme health y
ejecute smoke tests básicos.

Actuator expone solamente `/actuator/health` en producción y no revela detalles
sensibles. Los grupos de liveness/readiness de Spring Boot no están habilitados
ni visibles actualmente; no se añadieron endpoints ni probes nuevos en esta
etapa.
