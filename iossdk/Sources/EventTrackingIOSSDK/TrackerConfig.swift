import Foundation
import EventPushCore

public struct TrackerConfig: Sendable {
    public let appId: String
    public let serverApiKey: String
    public let apiBaseUrl: URL
    public let maxBatchSize: Int
    public let flushIntervalSeconds: TimeInterval
    public let retryDelaySeconds: TimeInterval
    public let holdEventsUntilUserId: Bool

    public init(
        appId: String,
        serverApiKey: String,
        apiBaseUrl: URL = URL(string: "http://localhost:8080")!,
        maxBatchSize: Int = 100,
        flushIntervalSeconds: TimeInterval = 30,
        retryDelaySeconds: TimeInterval = 2,
        holdEventsUntilUserId: Bool = true
    ) {
        self.appId = appId
        self.serverApiKey = serverApiKey
        self.apiBaseUrl = apiBaseUrl
        self.maxBatchSize = max(1, maxBatchSize)
        self.flushIntervalSeconds = flushIntervalSeconds
        self.retryDelaySeconds = retryDelaySeconds
        self.holdEventsUntilUserId = holdEventsUntilUserId
    }
}
