# CalmaDetector para iPhone + Apple Watch

Versión nativa en **Swift / SwiftUI** de CalmaDetector. Hace lo mismo que la app Android / Wear OS:
mismo detector de estrés (línea base + z-score), mismo simulador de escenarios y el mismo inicio de
sesión con código de 6 dígitos entre reloj y teléfono.

> El Apple Watch solo se empareja con un **iPhone**, así que esta versión incluye las dos apps.
> Un Apple Watch no puede conectarse con la app Android, ni un reloj Wear OS con un iPhone.

## Requisitos

- Una **Mac** con **Xcode 15** o superior (Apple no permite compilar apps de iOS/watchOS en Windows).
- iPhone con iOS 17+ y Apple Watch con watchOS 10+ (o los simuladores de Xcode).
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) para generar el proyecto: `brew install xcodegen`.
- Para instalar en dispositivos reales: una cuenta de Apple (gratuita sirve para pruebas).

## Compilar y ejecutar

```bash
cd ios
xcodegen            # crea CalmaDetector.xcodeproj a partir de project.yml
open CalmaDetector.xcodeproj
```

1. En Xcode, en **Signing & Capabilities** de los targets `CalmaPhone` y `CalmaWatch`, elige tu equipo (Team).
   Si Xcode dice que el bundle id ya existe, cambia `com.example.calma` por uno tuyo en `project.yml`
   (el del reloj debe empezar igual: `tu.bundle.id.watchkitapp`) y vuelve a ejecutar `xcodegen`.
2. Ejecuta el esquema **CalmaPhone** en el iPhone: la app del reloj se instala sola en el Apple Watch emparejado.
   En simuladores, elige un par iPhone + Apple Watch emparejado.
3. Para probar solo el reloj, ejecuta el esquema **CalmaWatch**.

### Probar la lógica sin Xcode

El detector, los códigos de vinculación y el protocolo están en el paquete `Packages/CalmaCore`,
que no depende de iOS. Sus tests corren en macOS o Linux:

```bash
cd ios/Packages/CalmaCore
swift test
```

## Inicio de sesión con código

1. Abre CalmaDetector en el **Apple Watch**: aparece un código de 6 dígitos.
2. En el **iPhone**, en la tarjeta **⌚ Reloj vinculado**, escribe el código y pulsa **Vincular**.
3. El reloj lo valida, vibra y abre el panel.

Igual que en Android: el código caduca a los 5 minutos, es de un solo uso y se invalida tras 5 intentos
fallidos. Se puede desvincular desde cualquiera de los dos; el otro se entera (si no está accesible en ese
momento, el aviso le llega después). La comunicación usa **WatchConnectivity**, sin servidor.

Para enviar el código, la app del reloj tiene que estar **abierta en pantalla**.

## Datos de pulso en el reloj

En el panel del reloj, **Fuente de datos** permite elegir:

- **Simulador**: los 4 escenarios de la versión Android (reposo, ejercicio, estrés en reposo, recuperación).
- **Sensores**: datos reales del Apple Watch.
  - **Pulso**: HealthKit en tiempo real. Para ello la app abre una sesión de "Mente y cuerpo"
    (aparece el icono de entrenamiento) y la descarta al salir, así que no guarda entrenamientos en Salud.
  - **HRV**: el Apple Watch la mide solo unas pocas veces al día. Se usa si hay una medición de los
    últimos 15 minutos; si no, el detector funciona solo con el pulso.
  - **Movimiento**: acelerómetro, con la misma unidad que en Android (desviación de la magnitud en m/s²).
  - La línea base empieza de cero con datos reales: necesita unas 60 muestras en reposo (~5 minutos).

Al cambiar de fuente, la línea base y el registro se reinician para no mezclar datos simulados y reales.
Cuando se dispara una alerta nueva, el reloj vibra.

La app del iPhone usa el simulador (como la versión Android del teléfono).

## Estructura

```text
ios/
├── project.yml                 # Definición del proyecto Xcode (XcodeGen)
├── Packages/CalmaCore/         # Lógica común, sin UI (con tests)
│   ├── StressDetector.swift    # Port de StressDetector.kt
│   ├── PairingCodeManager.swift
│   ├── PairingProtocol.swift   # Mensajes reloj ↔ iPhone (JSON)
│   ├── PairingStore.swift      # Vinculación guardada en UserDefaults
│   └── SimulationScenario.swift
├── Shared/                     # Código SwiftUI usado por las dos apps
│   ├── CalmaViewModel.swift    # Port de CalmaViewModel.kt (+ fuente de sensores)
│   ├── ConnectivityService.swift # WatchConnectivity (equivale a la Wearable Data Layer)
│   └── Theme.swift             # Misma paleta de colores
├── Phone/                      # App de iPhone
│   ├── DashboardView.swift
│   ├── WatchPairingCard.swift
│   └── PhonePairingViewModel.swift
└── Watch/                      # App de Apple Watch
    ├── CalmaWatchApp.swift
    ├── WatchDashboardView.swift
    ├── WatchPairingView.swift
    ├── WatchPairingViewModel.swift
    └── HealthSensorProvider.swift # HealthKit + acelerómetro
```

## Equivalencias con Android

| Android / Wear OS | iPhone / Apple Watch |
| --- | --- |
| Jetpack Compose / Wear Compose | SwiftUI |
| `StateFlow` + `ViewModel` | `ObservableObject` + `@Published` |
| Wearable Data Layer (`MessageClient`) | WatchConnectivity (`WCSession`) |
| `SharedPreferences` | `UserDefaults` |
| Health Services | HealthKit (`HKWorkoutSession`) |
