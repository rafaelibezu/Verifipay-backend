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
