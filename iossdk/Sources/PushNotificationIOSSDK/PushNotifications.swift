import Foundation
import EventPushCore
#if os(iOS)
import UserNotifications
import UIKit
#endif

public final class PushNotifications: @unchecked Sendable {
    public static let shared = PushNotifications()

    private let storage = TrackerStorage()
    private let apiClient = APIClient()
    private weak var analyticsTracker: PushAnalyticsTracking?
    private var pushConfig = PushConfig()

    private init() {}

    public func initialize(pushConfig: PushConfig = PushConfig(), analyticsTracker: PushAnalyticsTracking? = nil) {
        self.pushConfig = pushConfig
        self.analyticsTracker = analyticsTracker
    }

    #if os(iOS)
    public func requestAuthorization(options: UNAuthorizationOptions = [.alert, .badge, .sound]) async throws -> Bool {
        try await UNUserNotificationCenter.current().requestAuthorization(options: options)
    }

    public func registerForRemoteNotifications() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
    #endif

    public func setUserId(_ userId: String) {
        storage.userId = userId
        if let token = storage.pushToken {
            Task { try? await registerDevice(pushToken: token) }
        }
    }

    public func clearUserId() {
        storage.userId = nil
    }

    public func registerDevice(pushToken: String) async throws {
        guard let userId = storage.userId, !userId.isEmpty else {
            if pushConfig.requireUserId {
                storage.pushToken = pushToken
                throw TrackerError.missingUserId
            }
            storage.pushToken = pushToken
            return
        }
        storage.pushToken = pushToken
        let url = pushConfig.pushApiBaseUrl.trackerAppendingPath("devices/register")
        let payload = DeviceRegistrationPayload(userId: userId, fcmToken: pushToken, platform: "ios")
        try await apiClient.sendJSON(payload, to: url)
    }

    public func handleRemoteNotification(userInfo: [AnyHashable: Any], opened: Bool = false) {
        guard pushConfig.autoTrackNotificationEvents else { return }
        let payload = PushPayload(userInfo: userInfo)
        trackNotificationEvent(payload: payload, type: opened ? .opened : .received)
    }

    public func trackNotificationOpened(userInfo: [AnyHashable: Any]) {
        handleRemoteNotification(userInfo: userInfo, opened: true)
    }

    public func trackNotificationEvent(payload: PushPayload, type: NotificationEventType, actionId: String? = nil) {
        let eventProperties: [String: JSONValue] = [
            "notification_id": .string(payload.notificationId),
            "event": .string(type.rawValue),
            "campaign": payload.campaign.map(JSONValue.string) ?? .null,
            "category": payload.category.map(JSONValue.string) ?? .null,
            "action_id": actionId.map(JSONValue.string) ?? .null,
            "source": .string("push")
        ]
        analyticsTracker?.track("push_notification_\(type.rawValue)", properties: eventProperties)
    }
}

private struct DeviceRegistrationPayload: Encodable {
    let userId: String
    let fcmToken: String
    let platform: String
}
