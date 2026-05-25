import Foundation
import EventPushCore
#if os(iOS)
import UIKit
#endif

public final class EventTracker: PushAnalyticsTracking, @unchecked Sendable {
    public static let shared = EventTracker()

    private let queue = DispatchQueue(label: "com.tracker.iossdk.events")
    private let storage = TrackerStorage()
    private let apiClient = APIClient()

    private var config: TrackerConfig?
    private var events: [TrackerEvent] = []
    private var timer: Timer?
    private var flushing = false

    private init() {}

    public var isConfigured: Bool {
        queue.sync { config != nil }
    }

    public func initialize(config: TrackerConfig) {
        queue.sync {
            self.config = config
            self.events = storage.loadEvents()
            if let userId = config.holdEventsUntilUserId ? storage.userId : nil, !userId.isEmpty {
                storage.userId = userId
            }
        }
        startFlushTimer(interval: config.flushIntervalSeconds)
        AppLifecycleTracker.shared.start()
        track("app_opened")
    }

    public func setUserId(_ userId: String) {
        storage.userId = userId
        flush()
    }

    public func clearUserId() {
        storage.userId = nil
    }

    public func track(_ name: String, properties: [String: JSONValue] = [:]) {
        queue.sync {
            events.append(TrackerEvent(name: name, properties: properties))
            storage.saveEvents(events)
            if let config, events.count >= config.maxBatchSize {
                Task { await self.flushAsync() }
            }
        }
    }

    public func track(_ name: String, properties: [String: Any]) {
        track(name, properties: properties.trackerJSONValues())
    }

    public func flush() {
        Task { await flushAsync() }
    }

    public func shutdown() {
        timer?.invalidate()
        timer = nil
        flush()
    }

    private func startFlushTimer(interval: TimeInterval) {
        DispatchQueue.main.async {
            self.timer?.invalidate()
            self.timer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] _ in
                self?.flush()
            }
        }
    }

    private func flushAsync() async {
        var snapshot: (TrackerConfig, String, String, DeviceInfo, [TrackerEvent])?
        queue.sync {
            guard let config, !flushing, !events.isEmpty else {
                snapshot = nil
                return
            }
            guard let userId = storage.userId, !userId.isEmpty else {
                if config.holdEventsUntilUserId {
                    storage.saveEvents(events)
                    snapshot = nil
                    return
                }
                snapshot = (config, "", storage.deviceId(), DeviceInfo.current(sdkVersion: EventTrackingIOSSDK.version), events)
                flushing = true
                return
            }
            snapshot = (config, userId, storage.deviceId(), DeviceInfo.current(sdkVersion: EventTrackingIOSSDK.version), events)
            flushing = true
        }

        guard let (config, userId, deviceId, deviceInfo, batch) = snapshot else { return }
        do {
            let url = config.apiBaseUrl.trackerAppendingPath("api/v1/events/track")
            let payload = EventBatchPayload(userId: userId, deviceId: deviceId, deviceInfo: deviceInfo, events: batch)
            try await apiClient.sendJSON(payload, to: url, authorization: "Bearer \(config.serverApiKey)")
            queue.sync {
                events.removeFirst(min(batch.count, events.count))
                storage.saveEvents(events)
                flushing = false
            }
        } catch {
            queue.sync {
                storage.saveEvents(events)
                flushing = false
            }
            try? await Task.sleep(nanoseconds: UInt64(config.retryDelaySeconds * 1_000_000_000))
        }
    }
}

private struct EventBatchPayload: Encodable {
    let userId: String
    let deviceId: String
    let deviceInfo: DeviceInfo
    let events: [TrackerEvent]
}

public enum EventTrackingIOSSDK {
    public static let version = "0.1.0"
}
