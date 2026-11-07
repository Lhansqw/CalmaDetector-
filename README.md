#  CalmaDetector

> **Sistema de Detección de Estrés y Ansiedad en Tiempo Real** para **Android + Wear OS** y **iPhone + Apple Watch**.

**CalmaDetector** analiza la frecuencia cardíaca (**BPM**), la variabilidad del ritmo cardíaco (**HRV**) y el movimiento.
Con una **línea base personal** y la puntuación Z (*Z-Score*), detecta estrés leve o ansiedad inminente en reposo antes
de que se intensifique, y recomienda la respiración guiada 4-7-8. El movimiento sirve para no confundir ejercicio con estrés.

| Plataforma | Teléfono | Reloj | Código |
| --- | --- | --- | --- |
| Android | Android 8.0+ | Wear OS 4+ | Kotlin + Jetpack Compose (`src/`) |
| Apple | iOS 17+ | watchOS 10+ | Swift + SwiftUI ([`ios/`](ios/README.md)) |

> Un Apple Watch solo se empareja con un iPhone, y un reloj Wear OS con un Android.
> Por eso cada plataforma tiene su par de apps (teléfono + reloj), con la misma lógica y el mismo diseño.

---

##  Características

###  Panel del teléfono
- **Monitoreo en tiempo real**: BPM, HRV (ms) e indicador de movimiento.
- **Indicador de estado animado**: pulso y colores según el estado (Calma, Estrés Leve, Ansiedad Inminente).
- **Línea base adaptativa**: aprende tu frecuencia cardíaca de reposo, solo con muestras sin movimiento.
- **Simulador de escenarios**: reposo, ejercicio físico, estrés en reposo y recuperación.
- **Calibración**: ajusta la sensibilidad según tu respuesta ("Sí, sentí ansiedad" / "Fue falsa alarma").
- **Registro de alertas** de la sesión.

###  App del reloj
- Estado actual, métricas rápidas (BPM, HRV, movimiento) y progreso de la línea base de un vistazo.
- Simulador y calibración desde el propio reloj.
- **Wear OS**: diseño Wear Compose Material 3 con `TimeText` curvado y `TransformingLazyColumn`.
  Los márgenes y textos se calculan según el tamaño real de la pantalla (`WearDimens`), así se ve bien
  tanto en relojes pequeños (~192 dp) como en grandes (240 dp o más).
- **Apple Watch**: interfaz SwiftUI nativa. Además del simulador, puede usar **sensores reales**:
  pulso en vivo con HealthKit, HRV reciente y acelerómetro. Vibra cuando se dispara una alerta.

### 🔐 Inicio de sesión con código (vinculación reloj ↔ teléfono)
Para no tener que escribir contraseñas en el reloj:

1. Al abrir la app en el reloj aparece un **código de 6 dígitos** (por ejemplo `482 913`).
2. En el teléfono, en la tarjeta **⌚ Reloj vinculado**, se escribe ese código y se pulsa **Vincular**.
3. El reloj valida el código y abre el panel. Ambos dispositivos guardan la vinculación.

Detalles:
- Comunicación directa entre los dispositivos, sin servidor ni internet:
  **Wearable Data Layer** (`MessageClient`) en Android y **WatchConnectivity** (`WCSession`) en Apple.
- El código caduca a los **5 minutos**, es de **un solo uso** y se invalida tras **5 intentos fallidos**.
- Se puede **desvincular** desde cualquiera de los dos dispositivos; el otro se entera automáticamente.
- Si algo falla, el teléfono explica qué hacer (por ejemplo, emparejar el reloj o abrir la app en él).

---

##  Arquitectura y Tecnologías

### Android / Wear OS
- **Lenguaje**: Kotlin 2.1.10
- **UI**: Jetpack Compose + Material 3 (teléfono); Wear Compose Material 3 `1.0.0-alpha28` y Wear Foundation `1.5.0-alpha05` (reloj)
- **Arquitectura**: MVVM con `StateFlow` y Kotlin Coroutines

### iPhone / Apple Watch
- **Lenguaje**: Swift 5.9+ con SwiftUI
- **Lógica común**: paquete Swift `CalmaCore`, sin dependencias de UI y con tests (`swift test`)
- **Arquitectura**: MVVM con `ObservableObject` y `@Published`
- **Sensores (reloj)**: HealthKit (`HKWorkoutSession` para el pulso en vivo) y CoreMotion
- **Proyecto Xcode**: generado con [XcodeGen](https://github.com/yonaskolb/XcodeGen) desde `ios/project.yml`

### Detección (igual en ambas plataformas)
- `Baseline`: media y desviación estándar incrementales (Welford) de BPM y HRV en reposo.
- `StressDetector`: ventana móvil de 3 minutos, z-scores con histéresis, descarte por movimiento,
  refuerzo con HRV baja y tiempos de espera entre alertas (10 min nivel 2, 20 min nivel 1).

---

##  Estructura del Proyecto

```text
CalmaDetector/
├── src/main/kotlin/com/example/calma/      # Android + Wear OS
│   ├── detection/
│   │   ├── StressDetector.kt       # Línea base (Welford), z-scores y clasificación de alertas
│   │   └── Main.kt                 # Simulador de datos por consola
│   ├── pairing/
│   │   ├── PairingCodeManager.kt   # Genera y valida códigos de un solo uso
│   │   ├── PairingProtocol.kt      # Mensajes reloj ↔ teléfono (Data Layer)
│   │   ├── PairingStore.kt         # Guarda el dispositivo vinculado
│   │   ├── WatchPairingViewModel.kt # Lado del reloj: muestra y valida el código
│   │   └── PhonePairingViewModel.kt # Lado del teléfono: envía el código
│   └── ui/
│       ├── CalmaDashboardScreen.kt # Interfaz del teléfono (Material 3)
│       ├── WatchPairingCard.kt     # Tarjeta del teléfono para introducir el código
│       ├── CalmaWearApp.kt         # Interfaz de Wear OS (Wear Compose M3)
│       ├── WearPairingScreen.kt    # Pantalla del reloj que muestra el código
│       ├── WearDimens.kt           # Medidas adaptadas al tamaño de pantalla del reloj
│       ├── CalmaViewModel.kt       # ViewModel y gestión de estado (StateFlow)
│       ├── MainActivity.kt         # Punto de entrada único (redirige al reloj si corresponde)
│       ├── WearActivity.kt         # Inicio de sesión + panel en Wear OS
│       └── theme/                  # Colores y CalmaTheme (teléfono + reloj)
│
└── ios/                                     # iPhone + Apple Watch
    ├── project.yml                 # Definición del proyecto Xcode (XcodeGen)
    ├── Packages/CalmaCore/         # Detector, códigos, protocolo y simulador (con tests)
    ├── Shared/                     # ViewModel, WatchConnectivity y colores, comunes a ambas apps
    ├── Phone/                      # App de iPhone (panel + tarjeta de vinculación)
    └── Watch/                      # App de Apple Watch (código, panel y sensores HealthKit)
```

---

##  Instalación y Ejecución

### Android / Wear OS

**Requisitos**: Android Studio Ladybug (2024.2) o superior, JDK 21 y un teléfono o emulador
Android 8.0+ (API 26+) y/o un reloj o emulador Wear OS 4+ (API 30+).

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Lhansqw/CalmaDetector-.git
   cd CalmaDetector-
   ```
2. Abrir el proyecto en **Android Studio** y sincronizar con **Gradle Sync**.
3. Ejecutar la configuración `app`. Hay un solo APK e icono: `MainActivity` detecta si corre en un reloj
   y abre la interfaz de Wear OS automáticamente.
4. Para probar la vinculación con emuladores: **Tools → Device Manager → Pair Wearable** e instalar la app en ambos.
   En dispositivos reales, el reloj debe estar emparejado con la app Wear OS / Galaxy Wearable y ambos deben
   tener el mismo APK, firmado con la misma clave.
5. Tests unitarios: `./gradlew testDebugUnitTest`.

### iPhone / Apple Watch

**Requisitos**: una Mac con **Xcode 15** o superior (no se puede compilar desde Windows), iPhone con iOS 17+
y Apple Watch con watchOS 10+ (o sus simuladores), y XcodeGen (`brew install xcodegen`).

1. Generar y abrir el proyecto:
   ```bash
   cd ios
   xcodegen
   open CalmaDetector.xcodeproj
   ```
2. En **Signing & Capabilities** de `CalmaPhone` y `CalmaWatch`, elegir tu equipo (Team).
3. Ejecutar el esquema **CalmaPhone** en el iPhone: la app del reloj se instala sola en el Apple Watch emparejado.
4. Tests de la lógica (macOS o Linux, sin Xcode): `cd ios/Packages/CalmaCore && swift test`.

Más detalles (sensores, permisos de Salud, equivalencias con Android) en [`ios/README.md`](ios/README.md).

---

##  Estado

- **Android / Wear OS**: compila y los tests unitarios pasan (detector y códigos de vinculación).
- **iPhone / Apple Watch**: código completo, pendiente de compilar y probar por primera vez en Xcode.
