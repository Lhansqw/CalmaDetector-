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

### 🤖 Asistente con IA (Android)
Cuando salta una alerta, el asistente le escribe un mensaje corto a la persona y le propone una técnica concreta
(respiración 4-7-8, anclaje 5-4-3-2-1, soltar los hombros). En el teléfono se puede seguir la charla; en el reloj
se ve el último mensaje. No diagnostica ni reemplaza ayuda profesional, y ante una crisis indica llamar a emergencias.

- **Qué se envía**: solo los números de la alerta (pulso, HRV, movimiento, promedio de reposo y nivel) y lo que
  la persona escriba. No se manda ningún dato de identidad, y la charla no se guarda: se pierde al cerrar la app.
- **Modelo**: `openai/gpt-oss-120b` en [Groq](https://console.groq.com), por su API compatible con OpenAI (sin dependencias extra).
- **Clave**: va en `local.properties`. Ver "Instalación". Es una configuración **para pruebas**: la clave queda
  dentro de la APK, así que esa APK no se comparte. Para publicar la app habría que poner un servidor intermedio
  que guarde la clave.

---

##  Arquitectura y Tecnologías

### Android / Wear OS
- **Lenguaje**: Kotlin (integrado en AGP 9.5) y Gradle 9.9
- **UI**: Jetpack Compose + Material 3 (teléfono); Wear Compose Material 3 y Wear Foundation `1.7.2` (reloj)
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
│   ├── assistant/
│   │   ├── AssistantPrompts.kt     # Instrucciones del asistente y contexto de la alerta
│   │   └── CalmaAssistant.kt       # Llamada a la API de Groq
│   └── ui/
│       ├── CalmaDashboardScreen.kt # Interfaz del teléfono (Material 3)
│       ├── AssistantCard.kt        # Charla con el asistente en el teléfono
│       ├── AssistantViewModel.kt   # Estado de la charla
│       ├── CalmaWearApp.kt         # Interfaz de Wear OS (Wear Compose M3)
│       ├── WearDimens.kt           # Medidas adaptadas al tamaño de pantalla del reloj
│       ├── CalmaViewModel.kt       # ViewModel y gestión de estado (StateFlow)
│       ├── MainActivity.kt         # Punto de entrada único (redirige al reloj si corresponde)
│       ├── WearActivity.kt         # Panel en Wear OS
│       └── theme/                  # Colores y CalmaTheme (teléfono + reloj)
│
└── ios/                                     # iPhone + Apple Watch
    ├── project.yml                 # Definición del proyecto Xcode (XcodeGen)
    ├── Packages/CalmaCore/         # Detector y simulador (con tests)
    ├── Shared/                     # ViewModel y colores, comunes a ambas apps
    ├── Phone/                      # App de iPhone (panel)
    └── Watch/                      # App de Apple Watch (panel y sensores HealthKit)
```

---

##  Instalación y Ejecución

### Android / Wear OS

**Requisitos**: una versión reciente de Android Studio (compatible con AGP 9.5), el SDK de Android 37 y un teléfono o emulador
Android 8.0+ (API 26+) y/o un reloj o emulador Wear OS 4+ (API 30+).

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Lhansqw/CalmaDetector-.git
   cd CalmaDetector-
   ```
2. Abrir el proyecto en **Android Studio** y sincronizar con **Gradle Sync**.
3. (Opcional, para el asistente) Crear una clave en [console.groq.com](https://console.groq.com) → API Keys
   y agregarla a `local.properties`, en la raíz del proyecto:
   ```properties
   GROQ_API_KEY=gsk_...
   ```
   Sin la clave, la app funciona igual y la tarjeta del asistente avisa que está apagado.
4. Ejecutar la configuración `app`. Hay un solo APK e icono: `MainActivity` detecta si corre en un reloj
   y abre la interfaz de Wear OS automáticamente.
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

- **Android / Wear OS**: compila, los tests unitarios del detector pasan y lint no da errores.
- **iPhone / Apple Watch**: código completo, pendiente de compilar y probar por primera vez en Xcode.
