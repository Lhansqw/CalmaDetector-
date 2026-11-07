// swift-tools-version:5.9
import PackageDescription

// Lógica común de CalmaDetector para iPhone y Apple Watch (sin dependencias de UI).
// Se puede probar con `swift test` en macOS o Linux.
let package = Package(
    name: "CalmaCore",
    platforms: [.iOS(.v17), .watchOS(.v10), .macOS(.v13)],
    products: [
        .library(name: "CalmaCore", targets: ["CalmaCore"])
    ],
    targets: [
        .target(name: "CalmaCore"),
        .testTarget(name: "CalmaCoreTests", dependencies: ["CalmaCore"])
    ]
)
