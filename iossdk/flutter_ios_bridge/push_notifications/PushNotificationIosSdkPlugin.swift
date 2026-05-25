import Flutter
import PushNotificationIOSSDK
import UIKit

public final class PushNotificationIosSdkPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "push_notification_ios_sdk", binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(PushNotificationIosSdkPlugin(), channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let args = call.arguments as? [String: Any] ?? [:]
        switch call.method {
        case "initialize":
            let pushUrl = URL(string: args["pushApiBaseUrl"] as? String ?? "http://localhost:8082/api/v1")!
            PushNotifications.shared.initialize(pushConfig: PushConfig(pushApiBaseUrl: pushUrl))
            result(nil)

        case "setUserId":
            guard let userId = args["userId"] as? String else {
                result(FlutterError(code: "bad_args", message: "Missing userId", details: nil))
                return
            }
            PushNotifications.shared.setUserId(userId)
            result(nil)

        case "registerPushToken":
            guard let fcmToken = args["fcmToken"] as? String else {
                result(FlutterError(code: "bad_args", message: "Missing fcmToken", details: nil))
                return
            }
            Task {
                do {
                    try await PushNotifications.shared.registerDevice(pushToken: fcmToken)
                    result(nil)
                } catch {
                    result(FlutterError(code: "push_register_failed", message: String(describing: error), details: nil))
                }
            }

        case "handleRemoteNotification":
            let userInfo = args["userInfo"] as? [String: Any] ?? [:]
            let opened = args["opened"] as? Bool ?? false
            PushNotifications.shared.handleRemoteNotification(userInfo: userInfo, opened: opened)
            result(nil)

        default:
            result(FlutterMethodNotImplemented)
        }
    }
}
