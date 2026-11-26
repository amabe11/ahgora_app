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

## Modo técnico (segundo usuario)

Una sola app con dos perfiles (guía de la solución, secciones 6.2 y 7.6). Se entra desde la bienvenida («Soy técnico
de campo») o desde **Ayuda → Modo técnico**.

| Parte | Detalle |
|---|---|
| **Ingreso** | Usuario (nombre del técnico, queda como `responsable`) + **clave del equipo** (la `x-ahg-key` del Servidor Ahgora). La primera vez se comprueba con `GET /api/datos`; luego se puede entrar sin señal. La clave se guarda cifrada con el Android Keystore. «Probar sin clave» abre una sesión de demostración: sus muestras nunca salen del teléfono. |
| **Nueva muestra (5 pasos)** | 1. Identificar: matriz (sedimento, cola, pescado), código automático para la etiqueta (`ALC-SED-261126-AM01`), GPS exacto (por defecto Alcoche), río (por defecto río Coroico) y foto. 2. Muestreo: porciones, profundidad, % fino; en pescado especie, talla y peso. 3. Procedimiento guiado de la sección 4.3 con temporizadores (15 min, 5 min, 2 min, 30 s). 4. Controles (blanco, cloro libre = 0, referencia blanca, pH) y lectura del lector: **Bluetooth**, escrita a mano o de demostración. 5. Resultado: semáforo (prioridad alta / verificar / prioridad baja / inválido), acción sugerida, etiqueta «CRIBADO — NO ES DIAGNÓSTICO NI EVIDENCIA LEGAL» y mensaje para la comunidad. |
| **Muestras** | Historial con estado de envío, ficha completa, **cadena de custodia** (registrada → enviada al laboratorio), mapa de muestras y exportación **CSV** para el laboratorio. |
| **Envío** | Todo se guarda primero en el teléfono (Room) y se envía a `POST /api/sync` con `x-ahg-key` al haber señal (también en segundo plano con WorkManager). Repetir no duplica: el servidor actualiza por `codigo`. Siempre `visibilidad: restringido`; publicar un aviso lo decide un responsable en Ahgora Web. |

**Semáforo provisional.** ΔE = distancia entre la casilla de Hg sin usar y la reaccionada, normalizadas con la tarjeta
blanca (reflectancia %). Umbrales 4 y 8 (bajos a propósito, sección 6.3). Mientras la curva no esté validada con el
laboratorio (`Cribado.CALIBRACION_VALIDADA = false`) todo resultado va como demostración y no se convierte a mg/kg.

**Lector por Bluetooth.** La app usa Bluetooth clásico (SPP) y espera, por línea, el mismo JSON que hoy sale por USB
(`{"node_id":"AHG-SED-01","r":…,"g":…,"b":…,"c":…}`), promediando 10 lecturas. Al firmware le falta solo
`BluetoothSerial SerialBT; SerialBT.begin("AHG-SED-01");` y `SerialBT.println(json)`. El lector se empareja antes
desde los ajustes de Bluetooth del teléfono.

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
  data/tecnico/     Modo técnico: muestras (Room), sesión cifrada, lector Bluetooth, cribado y envío a /api/sync
  data/             Repositorio, conexión, ubicación única
  sync/             SyncWorker (WorkManager)
  notif/            Notificaciones genéricas
  ui/               Tema, presentación de estados, ViewModel y pantallas
  ui/tecnico/       Ingreso, nueva muestra (5 pasos), muestras, mapa de muestras y cuenta
```
