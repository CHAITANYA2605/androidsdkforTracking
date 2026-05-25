import Foundation

public enum NotificationEventType: String, Sendable {
    case received
    case opened
    case dismissed
    case actionClicked = "action_clicked"
}

public struct PushPayload: Sendable {
    public let notificationId: String
    public let title: String?
    public let body: String?
    public let deeplink: String?
    public let campaign: String?
    public let category: String?
    public let customData: [String: String]

    public init(userInfo: [AnyHashable: Any]) {
        func string(_ key: String) -> String? {
            userInfo[key] as? String
        }
        self.notificationId = string("notification_id") ?? string("id") ?? UUID().uuidString
        self.title = string("title")
        self.body = string("body")
        self.deeplink = string("deeplink") ?? string("deep_link")
        self.campaign = string("campaign")
        self.category = string("category")
        self.customData = userInfo.reduce(into: [String: String]()) { result, entry in
            guard let key = entry.key as? String else { return }
            if let value = entry.value as? String {
                result[key] = value
            } else {
                result[key] = String(describing: entry.value)
            }
        }
    }
}

public enum PushNotificationIOSSDK {
    public static let version = "0.1.0"
}
