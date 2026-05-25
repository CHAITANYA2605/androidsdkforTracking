import Foundation

public struct PushConfig: Sendable {
    public let pushApiBaseUrl: URL
    public let requireUserId: Bool
    public let autoTrackNotificationEvents: Bool

    public init(
        pushApiBaseUrl: URL = URL(string: "http://localhost:8082/api/v1")!,
        requireUserId: Bool = true,
        autoTrackNotificationEvents: Bool = true
    ) {
        self.pushApiBaseUrl = pushApiBaseUrl
        self.requireUserId = requireUserId
        self.autoTrackNotificationEvents = autoTrackNotificationEvents
    }
}
