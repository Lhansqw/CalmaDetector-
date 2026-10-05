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
│   ├── Baseline.kt          # Cálculo dinámico de media y desviación estándar
│   ├── StressDetector.kt    # Algoritmo de clasificación de niveles de alerta
│   └── Main.kt              # Simulador de datos por consola
└── ui/
    ├── CalmaDashboardScreen.kt # Interfaz para Smartphones (Material 3)
    ├── CalmaWearApp.kt         # Interfaz para Wear OS (Wear Compose M3)
    ├── CalmaViewModel.kt       # ViewModel y gestión de estado (StateFlow)
    ├── MainActivity.kt         # Actividad principal para Smartphone
    ├── WearActivity.kt         # Actividad principal para Wear OS
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
4. Seleccionar el objetivo de ejecución:
   - Para **Móvil**: Seleccionar el dispositivo/emulador móvil y ejecutar `MainActivity`.
   - Para **Wear OS**: Seleccionar el emulador de reloj (`Wear OS XL Round`) y ejecutar `WearActivity`.

---

##  Licencia

Proyecto desarrollado con fines educativos y de demostración.
