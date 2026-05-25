import Foundation
#if os(iOS)
import UIKit
#endif

public final class TrackerStorage: @unchecked Sendable {
    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    private enum Key {
        static let userId = "tracker_ios_user_id"
        static let deviceId = "tracker_ios_device_id"
        static let events = "tracker_ios_events"
        static let pushToken = "tracker_ios_push_token"
    }

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        encoder.dateEncodingStrategy = .iso8601
        decoder.dateDecodingStrategy = .iso8601
    }

    public var userId: String? {
        get { defaults.string(forKey: Key.userId) }
        set { defaults.set(newValue, forKey: Key.userId) }
    }

    public var pushToken: String? {
        get { defaults.string(forKey: Key.pushToken) }
        set { defaults.set(newValue, forKey: Key.pushToken) }
    }

    public func deviceId() -> String {
        if let saved = defaults.string(forKey: Key.deviceId) {
            return saved
        }
        let created: String
        #if os(iOS)
        created = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        #else
        created = UUID().uuidString
        #endif
        defaults.set(created, forKey: Key.deviceId)
        return created
    }

    public func saveEvents(_ events: [TrackerEvent]) {
        guard let data = try? encoder.encode(events) else { return }
        defaults.set(data, forKey: Key.events)
    }

    public func loadEvents() -> [TrackerEvent] {
        guard let data = defaults.data(forKey: Key.events) else { return [] }
        return (try? decoder.decode([TrackerEvent].self, from: data)) ?? []
    }
}
