# Guía para agentes — VerifiPay Backend

## Producto
VerifiPay es una solución a medida para una sola distribuidora.
No es inicialmente un SaaS multiempresa.

Una sola aplicación Flutter tiene dos roles humanos:
- OWNER: dueño del negocio. Su celular tiene Yape y VerifiPay,
  que escucha las notificaciones reales de Yape.
- CASHIER: crea cobros esperados y consulta su estado.
  No necesita acceso al Yape del dueño.

Todos los pagos llegan a un único Yape central del OWNER.
El cliente no usa VerifiPay: yapea normalmente al dueño.

## Modelo y flujo objetivo
- PaymentRequest: cobro esperado creado por un CASHIER antes del pago.
- PaymentEvent: pago detectado mediante una notificación del dispositivo OWNER.
  No representa una confirmación independiente de una API bancaria.

Flujo:
1. El CASHIER crea un PaymentRequest en WAITING.
2. El dispositivo OWNER envía el evento al backend.
3. DevicePaymentEventService registra el PaymentEvent.
4. El backend busca automáticamente cobros WAITING compatibles
   por negocio, monto exacto y ventana de tiempo.
5. Una coincidencia: evento MATCHED y cobro PAID, relacionados.
6. Varias coincidencias: evento y cobros candidatos AMBIGUOUS.
7. Ninguna coincidencia: evento UNMATCHED.

Estados principales:
- PaymentRequest: WAITING, PAID, AMBIGUOUS, EXPIRED, CANCELLED.
- PaymentEvent: UNMATCHED, MATCHED, AMBIGUOUS.

Este es el modelo objetivo: revisar el código para determinar
qué está implementado y qué sigue siendo legado.

## Invariantes de negocio
- Un evento nunca puede pagar dos cobros.
- Un cobro nunca puede recibir dos eventos asignados.
- No elegir automáticamente el candidato más antiguo o cercano
  cuando haya varias coincidencias.
- Conservar los candidatos de una ambigüedad para revisión.
- Excluir AMBIGUOUS del matching automático.
- No resolver ambigüedades por expiración o cancelación de candidatos.
- No inventar reglas de revisión manual, ventanas o tolerancias:
  aclararlas si no están definidas.
- El local del cajero no delimita el matching del Yape central.

## Stack y arquitectura
Stack actual: Java 21, Spring Boot 4.1.1, Maven,
Spring MVC, Spring Security/JWT, JPA/Hibernate, PostgreSQL,
Flyway, Jakarta Validation, Lombok y JUnit.

- Mantener el monolito organizado por funcionalidad.
- Controllers para HTTP; Services para negocio y transacciones;
  Repositories para persistencia; DTOs y mappers para contratos.
- Aplicar KISS: cambios pequeños, explícitos y compatibles.
- Reutilizar el código existente antes de crear abstracciones.
- Conservar Company y Branch mientras existan dependencias.
- No añadir dependencias sin justificar su necesidad y alternativas.

## Seguridad y consistencia
- Obtener usuario y negocio desde la autenticación, no del body.
- CASHIER solo opera sobre sus cobros; OWNER administra su negocio.
- Comprobar permisos y estados activos en el backend.
- Mantener autenticación por token del dispositivo; guardar su hash.
- No incluir secretos en código, logs ni respuestas.
- Garantizar idempotencia en recepción de eventos y creación de cobros.
- Proteger matching con transacciones, bloqueos de base de datos
  y restricciones de unicidad; no depender de bloqueos en memoria.
- Coordinar creación, matching, cancelación, expiración y revisión.
- Usar BigDecimal, validar precisión y manejar tiempos consistentemente.
- Notificar al cajero únicamente después del commit.
- No habilitar pagos simulados en producción.

## Flyway
- Nunca editar ni borrar migraciones ya aplicadas.
- Añadir migraciones nuevas y consecutivas.
- Revisar datos existentes antes de imponer restricciones.
- No inventar relaciones para pagos históricos.
- Mantener ddl-auto=validate; no sustituir Flyway por update/create.
- No ejecutar migraciones contra bases reales sin autorización explícita.

## Testing
- Ejecutar los tests pertinentes antes de dar un cambio por terminado.
- Para cambios de negocio o persistencia, ejecutar la suite Maven.
- Cubrir coincidencia única, ninguna y múltiples coincidencias,
  reintentos, concurrencia, permisos y límites temporales.
- Verificar bloqueos y restricciones con PostgreSQL de pruebas.
- No usar datos ni bases de producción para tests.
- Si los tests no pueden ejecutarse, explicar el impedimento.
  Nunca afirmar que pasaron sin haberlos ejecutado.

## Antes de modificar código
1. Leer este archivo y revisar código, configuración, migraciones
   y tests relacionados.
2. Identificar comportamiento actual, diferencias con el objetivo
   y posibles conflictos con el flujo antiguo de Verification.
3. Proponer el cambio mínimo, sus riesgos y cómo validarlo.
4. Aclarar reglas de negocio faltantes antes de implementarlas.
5. Respetar el alcance solicitado: si es solo análisis, no modificar.
6. Implementar incrementalmente, ejecutar tests y revisar el diff.
7. Informar qué cambió, qué se verificó y qué quedó pendiente.

## No hacer
- No rehacer la arquitectura ni realizar refactors ajenos a la tarea.
- No introducir Mercado Pago, POS, tarjetas, Plin, microservicios,
  colas ni WebSockets.
- No ampliar el producto hacia SaaS o múltiples Yapes.
- No eliminar historial ni componentes legados sin revisar su uso.
- No permitir que el flujo antiguo y el nuevo consuman el mismo pago.
- No modificar archivos, desplegar ni ejecutar migraciones cuando
  el usuario haya solicitado únicamente análisis o un plan.
