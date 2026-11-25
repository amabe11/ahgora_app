# Ahgora Comunidad · Android

App Android (Kotlin + Jetpack Compose) para **comunarios, promotores de salud y cooperativas**. Muestra los avisos
**aprobados** sobre mercurio cerca de su zona: qué pasó, en qué estado está el caso y a quién consultar.
Gold4Good Bolivia 2026 · Desafío ORO-4.1. Implementa la sección «Android Comunidad» de la *Guía definitiva de Ahgora*.

> Un aviso es una **primera señal, no un diagnóstico ni una prueba legal**. «Cribado» y «confirmado por laboratorio»
> nunca se mezclan.

## Qué hace

| Función | Detalle |
|---|---|
| **Funciona sin internet** | Room es la única fuente de verdad: avisos, ayuda y contactos se leen siempre del teléfono. Se indica «Sin conexión · avisos guardados el …» y la fecha de la última actualización. |
| **Ubicación una sola vez** | En el primer uso se pide el GPS (funciona sin datos) y se convierte al instante en la **celda de 0,1° (~11 km)**, la misma zona pública que usa el servidor. La coordenada exacta no se guarda ni se envía. Alternativa: elegir la localidad de una lista incluida en la app. |
| **Cambiar zona** | Desde Inicio o Preferencias, con GPS o lista, y con nombre opcional para la comunidad. |
| **Sincronización** | Al abrir la app, al volver la señal (WorkManager con restricción de red y reintentos) y cada 6 h. Sin duplicados, una versión vieja no reemplaza a una nueva y los avisos retirados salen de la lista vigente pero quedan en el historial con fecha y motivo. |
| **Notificaciones** | Mensaje genérico («Hay una actualización de Ahgora para tu zona») que abre el aviso vigente. Se piden después de explicar para qué sirven; la bandeja funciona igual sin el permiso. |
| **Inicio · Avisos · Mapa · Ayuda** | Aviso destacado de la zona; historial con filtros (mi zona / vecinas / todos); mapa esquemático **offline** (Canvas), mapa con fondo OpenStreetMap si hay señal y **lista equivalente**; ayuda con estados, preguntas frecuentes y contactos. |
| **Reportes voluntarios** | Con consentimiento, zona aproximada opcional y pantalla de revisión. Se guardan en el teléfono y se envían con conexión. |
| **Accesibilidad** | Estado = texto + ícono + color, áreas táctiles ≥ 48 dp, etiquetas para TalkBack y tamaño de texto propio (Normal / Grande / Muy grande). |
| **Modo demostración** | Avisos rotulados **EJEMPLO** generados alrededor de la zona elegida, para mostrar la app sin servidor ni señal. En *Preferencias → Simular siguiente actualización* el caso avanza: pendiente → en análisis → confirmado → aviso nuevo, con notificación. |

## Servidor

Consume la API pública del Servidor Ahgora (repo `taku`, `server/`):

```
GET https://taku-server.vercel.app/api/alertas   →   { aviso, items: [{ matriz, zona{lat_min,lon_min,…}, nivel, tipo, mensaje, fecha, laboratorio, demostracion }] }
```

La app no necesita clave y solo lee avisos aprobados de muestras públicas. Acepta además campos opcionales que
el servidor puede agregar sin romper nada (contrato de la guía, sección 09):

- `id` y `version`: hoy la app deriva un id estable del contenido. Con un id del servidor, las rectificaciones se enlazan con exactitud.
- `certeza` (`cribado` | `en_verificacion` | `confirmado`) y `retirado: true`.
- `POST /api/reportes` para los reportes comunitarios. Mientras no exista, los reportes quedan guardados y se reintentan.

## Compilar y ejecutar

Requisitos: Android Studio reciente (AGP 9.5, Gradle 9.6, JDK 17+), SDK 37. `minSdk` 26.

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # APK de ~2 MB con R8, firmado con la clave de depuración para instalarlo directo
./gradlew testDebugUnitTest      # pruebas de zona y de reglas de sincronización
```

Opcional en `local.properties`:

```properties
ahgora.apiBaseUrl=https://taku-server.vercel.app/
ahgora.demoMode=true
```

La dirección del servidor también se puede cambiar en la app (*Preferencias → Opciones avanzadas*).

## Guion de demo (2 minutos)

1. Primer uso: «Empezar» → «Usar mi ubicación» → «Solo esta vez» → zona detectada → «Sí, avisarme».
2. Inicio: aviso de ejemplo en tu zona, resumen de zonas vecinas, «Ver detalle y qué hacer».
3. Mapa → Esquema: tu zona y las vecinas por estado, sin internet. Lista: lo mismo en texto.
4. Activar **modo avión** y cerrar la app. Al abrirla: «Sin conexión · avisos guardados el …», todo legible.
5. Preferencias → «Simular siguiente actualización»: llega la notificación y al tocarla se abre la versión nueva («En análisis»).

## Estructura

```
app/src/main/java/bo/ahgora/comunidad/
  data/zona/        Celda (0,1°, igual que el servidor) y localidades de referencia
  data/local/       Room: avisos y reportes
  data/remote/      Cliente de /api/alertas y /api/reportes (OkHttp + kotlinx.serialization)
  data/sync/        Fusion: reglas de versiones, retiros y duplicados (con pruebas)
  data/demo/        Avisos de demostración
  data/             Repositorio, conexión, ubicación única
  sync/             SyncWorker (WorkManager)
  notif/            Notificaciones genéricas
  ui/               Tema, presentación de estados, ViewModel y pantallas
```
