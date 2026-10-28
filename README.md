#  CalmaDetector

> **Sistema de Detección de Estrés y Ansiedad en Tiempo Real** para dispositivos **Android** y **Wear OS**.

**CalmaDetector** es una aplicación multiplataforma (Móvil + Reloj Inteligente) que analiza datos biométricos como Frecuencia Cardíaca (**BPM**), Variabilidad del Ritmo Cardíaco (**HRV**) y Movimiento en tiempo real. Mediante un algoritmo de **Línea Base Dinámica** y cálculo de puntuación Z (*Z-Score*), detecta episodios de estrés leve o ansiedad inminente antes de que se intensifiquen, ofreciendo pautas de respiración guiada (4-7-8) e interfaz de calibración.

---

##  Características Principales

###  Panel Móvil (Smartphone Dashboard)
- **Monitoreo en Tiempo Real**: Visualización dinámica de BPM, HRV (ms) e indicador de movimiento.
- **Indicador de Estado Animado**: Animaciones de pulso y estados diferenciados por color (Calma, Estrés Leve, Ansiedad Inminente).
- **Línea Base Adaptativa**: Entrenamiento dinámico de la frecuencia cardíaca de reposo del usuario.
- **Simulador de Escenarios**: Permite probar la respuesta del algoritmo ante reposo, ejercicio físico, estrés silencioso o recuperación.
- **Calibración y Retroalimentación**: Ajuste de sensibilidad del detector con base en la respuesta del usuario (falsa alarma / confirmación de ansiedad).

###  Aplicación Wear OS (Smartwatch)
- **Diseño Nativo Wear Compose Material 3**: Adaptado especialmente para pantallas redondas (**Wear OS XL Round**).
- **`ScreenScaffold` & `TimeText`**: Texto de hora curvado siguiendo el borde superior circular del reloj.
- **Desplazamiento fluido con `TransformingLazyColumn`**: Efecto de transformación y escalado al navegar por la lista o usar la corona rotatoria.
- **Diseño "De un vistazo"**: Métricas rápidas de BPM, HRV y movimiento organizadas en un panel horizontal fácil de leer mientras se usa el reloj.

### 🔐 Inicio de sesión con código (vinculación reloj ↔ teléfono)
Para no tener que escribir contraseñas en el reloj:

1. Al abrir la app en el reloj por primera vez aparece un **código de 6 dígitos** (por ejemplo `482 913`).
2. En el teléfono, en la tarjeta **⌚ Reloj vinculado**, se escribe ese código y se pulsa **Vincular**.
3. El reloj valida el código y abre el panel. Ambos dispositivos guardan la vinculación.

Detalles:
- La comunicación es directa por Bluetooth con la **Wearable Data Layer** (`MessageClient`), sin servidor ni internet.
- El código caduca a los **5 minutos**, es de **un solo uso** y se invalida tras **5 intentos fallidos**.
- Se puede **desvincular** desde cualquiera de los dos dispositivos; el otro se entera automáticamente.
- Requisito: el reloj debe estar emparejado con el teléfono (app Wear OS / Galaxy Wearable) y ambos deben tener instalada esta misma APK, firmada con la misma clave.

---

##  Arquitectura y Tecnologías

- **Lenguaje**: Kotlin 2.1.10
- **UI Framework**:
  - Jetpack Compose & Material 3 (Móvil)
  - Wear OS Compose Material 3 `1.0.0-alpha28` & Wear Foundation `1.5.0-alpha05` (Wear OS)
- **Arquitectura**: MVVM (Model-View-ViewModel) con `StateFlow` y `Kotlin Coroutines`.
- **Detección Algorítmica**:
  - `StressDetector`: Evaluación de ventanas móviles de datos biométricos.
  - `Baseline`: Estadísticas continuas de media y desviación estándar para normalización por Z-Score.

---

##  Estructura del Proyecto

```text
com.example.calma/
├── detection/
│   ├── StressDetector.kt    # Línea base (Welford), z-scores y clasificación de alertas
│   └── Main.kt              # Simulador de datos por consola
├── pairing/
│   ├── PairingCodeManager.kt   # Genera y valida códigos de un solo uso
│   ├── PairingProtocol.kt      # Mensajes reloj ↔ teléfono (Data Layer)
│   ├── PairingStore.kt         # Guarda el dispositivo vinculado
│   ├── WatchPairingViewModel.kt # Lado del reloj: muestra y valida el código
│   └── PhonePairingViewModel.kt # Lado del teléfono: envía el código
└── ui/
    ├── CalmaDashboardScreen.kt # Interfaz para Smartphones (Material 3)
    ├── WatchPairingCard.kt     # Tarjeta del teléfono para introducir el código
    ├── CalmaWearApp.kt         # Interfaz para Wear OS (Wear Compose M3)
    ├── WearPairingScreen.kt    # Pantalla del reloj que muestra el código
    ├── CalmaViewModel.kt       # ViewModel y gestión de estado (StateFlow)
    ├── MainActivity.kt         # Punto de entrada único (redirige al reloj si corresponde)
    ├── WearActivity.kt         # Inicio de sesión + panel en Wear OS
    └── theme/
        ├── Color.kt            # Paleta de colores (CyanPrimary, Slate, Status)
        └── Theme.kt            # Proveedor unificado de CalmaTheme (Phone + Wear)
```

---

##  Instalación y Ejecución

### Requisitos Previos
- **Android Studio** Ladybug (2024.2) o superior.
- **JDK 21** o compatible.
- Emulador de teléfono (**Android 8.0+ / API 26+**) o emulador de reloj (**Wear OS 4+ / API 30+**).

### Pasos
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Lhansqw/CalmaDetector-.git
   cd CalmaDetector-
   ```
2. Abrir el proyecto en **Android Studio**.
3. Sincronizar las dependencias con **Gradle Sync**.
4. Ejecutar la configuración `app` en el dispositivo deseado. Hay un solo icono: `MainActivity` detecta si corre en un reloj y abre la interfaz de Wear OS automáticamente.
5. Para probar la vinculación con emuladores, emparejar el emulador de reloj con el de teléfono desde **Tools → Device Manager → Pair Wearable** e instalar la app en ambos.
6. Tests unitarios: `./gradlew testDebugUnitTest`.

---

##  Licencia

Proyecto desarrollado con fines educativos y de demostración.
