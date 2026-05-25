import Flutter
import EventTrackingIOSSDK
import UIKit

public final class TrackerIosSdkPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "event_tracking_ios_sdk", binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(TrackerIosSdkPlugin(), channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let args = call.arguments as? [String: Any] ?? [:]
        switch call.method {
        case "initialize":
            guard
                let appId = args["appId"] as? String,
                let serverApiKey = args["serverApiKey"] as? String,
                let apiBase = args["apiBaseUrl"] as? String,
                let apiBaseUrl = URL(string: apiBase)
            else {
                result(FlutterError(code: "bad_args", message: "Missing tracker config", details: nil))
                return
            }
            let trackerConfig = TrackerConfig(appId: appId, serverApiKey: serverApiKey, apiBaseUrl: apiBaseUrl)
            EventTracker.shared.initialize(config: trackerConfig)
            result(nil)

        case "setUserId":
            guard let userId = args["userId"] as? String else {
                result(FlutterError(code: "bad_args", message: "Missing userId", details: nil))
                return
            }
            EventTracker.shared.setUserId(userId)
            result(nil)

        case "track":
            guard let name = args["name"] as? String else {
                result(FlutterError(code: "bad_args", message: "Missing event name", details: nil))
                return
            }
            EventTracker.shared.track(name, properties: args["properties"] as? [String: Any] ?? [:])
            result(nil)

        case "flush":
            EventTracker.shared.flush()
            result(nil)

        default:
            result(FlutterMethodNotImplemented)
        }
    }
}
