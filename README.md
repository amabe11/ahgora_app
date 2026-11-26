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
| **Alcoche por defecto** | La app viene con la comunidad piloto ya elegida: **Alcoche, municipio de Caranavi** (río Coroico). El primer uso es una sola pantalla: «Empezar». |
| **Cambiar comunidad** | Desde Inicio («Cambiar»): lista con las comunidades más cercanas a Alcoche primero (un toque guarda), buscador, o «Usar mi ubicación» (GPS sin datos, se guarda solo la **celda de 0,1° (~11 km)**; la coordenada exacta no se guarda ni se envía). |
| **Sincronización** | Al abrir la app, al volver la señal (WorkManager con restricción de red y reintentos) y cada 6 h. Sin duplicados, una versión vieja no reemplaza a una nueva y los avisos retirados salen de la lista vigente pero quedan en el historial con fecha y motivo. |
| **Notificaciones** | Mensaje genérico («Hay una actualización de Ahgora para tu zona») que abre el aviso vigente. Se piden después de explicar para qué sirven; la bandeja funciona igual sin el permiso. |
| **Inicio · Avisos · Mapa · Ayuda** | Inicio con el aviso de la comunidad y tres botones grandes (Mapa, Preguntar, Reportar); avisos de mi comunidad y de las vecinas (los antiguos a un toque); ayuda con contacto primero, estados y 7 preguntas cortas. |
| **Mapa en dos** | Mapa real (OpenStreetMap) con dos pestañas: **Mercurio** — lugares con minería de oro reportada (río Coroico junto a Alcoche, Guanay–Teoponte en el río Kaka, Tipuani, Mapiri) y los avisos de Ahgora; **Peces** — tramos de río (Coroico, Kaka, Alto Beni, Beni) con sus peces en verde / ámbar / rojo según cuánto mercurio suelen acumular. Cada pestaña tiene su **Lista** equivalente sin internet. Datos y fuentes en `data/mapa/ContextoAlcoche.kt`. |
| **Reportes voluntarios** | Con consentimiento, zona aproximada opcional y pantalla de revisión. Se guardan en el teléfono y se envían con conexión. |
| **Accesibilidad** | Estado = texto + ícono + color, áreas táctiles ≥ 48 dp, etiquetas para TalkBack y tamaño de texto propio (Normal / Grande / Muy grande). |
| **Modo demostración** | Avisos rotulados **EJEMPLO** generados alrededor de la zona elegida, para mostrar la app sin servidor ni señal. En *Preferencias → Simular siguiente actualización* el caso avanza: pendiente → en análisis → confirmado → aviso nuevo, con notificación. |

## Sobre los datos del mapa

Los círculos de la pestaña **Mercurio** marcan **minería de oro reportada**, no mediciones: es donde es más probable
encontrar mercurio. La confirmación llega solo como aviso aprobado con resultado de laboratorio. Las ubicaciones son
aproximadas y deben ajustarse con la comunidad antes del piloto. La clasificación de **Peces** viene del estudio
UMSA – WCS Bolivia en el mercado de Rurrenabaque (2024); la lista de peces de cada tramo debe confirmarse con los
pescadores de Alcoche. Las indicaciones de consumo las da la autoridad de salud (guía, sección 05).

## Servidor

Consume la API pública del Servidor Ahgora (repo `taku`, `server/`):

```
GET https://taku-server.vercel.app/api/alertas   →   { aviso, items: [{ matriz, zona{lat_min,lon_min,…}, nivel, tipo, mensaje, fecha, laboratorio, demostracion }] }
```

La app no necesita clave y solo lee avisos aprobados de muestras públicas. Usa el contrato de la guía (sección 09):

- `id` y `version` del servidor: una versión más antigua nunca reemplaza a una nueva. Si el servidor no los enviara, la app deriva un id estable del contenido.
- `certeza` (`cribado` | `en_verificacion` | `confirmado`) y `retirado: true` para quitar un aviso de la lista vigente.
- `POST /api/reportes` para los reportes comunitarios. Sin conexión, quedan guardados y se reintentan.

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

1. Primer uso: Alcoche ya está elegido → «Empezar» → permitir mensajes.
2. Inicio: aviso de ejemplo en tu zona, resumen de zonas vecinas, «Ver detalle y qué hacer».
3. Mapa → Mercurio: minería de oro cerca de Alcoche y río abajo; tocar un círculo. Peces: tocar «Río Coroico». «Lista»: lo mismo sin internet.
4. Activar **modo avión** y cerrar la app. Al abrirla: «Sin conexión · avisos guardados el …», todo legible.
5. Preferencias → «Simular siguiente actualización»: llega la notificación y al tocarla se abre la versión nueva («En análisis»).

## Estructura

```
app/src/main/java/bo/ahgora/comunidad/
  data/zona/        Celda (0,1°, igual que el servidor) y localidades de referencia (Alcoche primero)
  data/mapa/        Contexto de la zona piloto: lugares con minería, peces y tramos de río (con fuentes)
  data/local/       Room: avisos y reportes
  data/remote/      Cliente de /api/alertas y /api/reportes (OkHttp + kotlinx.serialization)
  data/sync/        Fusion: reglas de versiones, retiros y duplicados (con pruebas)
  data/demo/        Avisos de demostración
  data/             Repositorio, conexión, ubicación única
  sync/             SyncWorker (WorkManager)
  notif/            Notificaciones genéricas
  ui/               Tema, presentación de estados, ViewModel y pantallas
```
