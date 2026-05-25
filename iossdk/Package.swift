// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "EventPushIOSSDK",
    platforms: [
        .iOS(.v13),
        .macOS(.v12)
    ],
    products: [
        .library(name: "EventTrackingIOSSDK", targets: ["EventTrackingIOSSDK"]),
        .library(name: "PushNotificationIOSSDK", targets: ["PushNotificationIOSSDK"])
    ],
    targets: [
        .target(
            name: "EventPushCore",
            path: "Sources/EventPushCore"
        ),
        .target(
            name: "EventTrackingIOSSDK",
            dependencies: ["EventPushCore"],
            path: "Sources/EventTrackingIOSSDK"
        ),
        .target(
            name: "PushNotificationIOSSDK",
            dependencies: ["EventPushCore"],
            path: "Sources/PushNotificationIOSSDK"
        )
    ]
)
