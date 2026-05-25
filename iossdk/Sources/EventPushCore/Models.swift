import Foundation
#if os(iOS)
import UIKit
#endif

public struct TrackerEvent: Codable, Sendable {
    public let name: String
    public let properties: [String: JSONValue]
    public let occurredAt: Date

    public init(name: String, properties: [String: JSONValue] = [:], occurredAt: Date = Date()) {
        self.name = name
        self.properties = properties
        self.occurredAt = occurredAt
    }
}

public struct DeviceInfo: Codable, Sendable {
    public let platform: String
    public let osVersion: String
    public let model: String
    public let manufacturer: String
    public let sdkVersion: String
    public let locale: String
    public let timezone: String

    public static func current(sdkVersion: String) -> DeviceInfo {
        #if os(iOS)
        let device = UIDevice.current
        return DeviceInfo(
            platform: "iOS",
            osVersion: device.systemVersion,
            model: device.model,
            manufacturer: "Apple",
            sdkVersion: sdkVersion,
            locale: Locale.current.identifier,
            timezone: TimeZone.current.identifier
        )
        #else
        return DeviceInfo(
            platform: "iOS",
            osVersion: ProcessInfo.processInfo.operatingSystemVersionString,
            model: "unknown",
            manufacturer: "Apple",
            sdkVersion: sdkVersion,
            locale: Locale.current.identifier,
            timezone: TimeZone.current.identifier
        )
        #endif
    }
}

public enum JSONValue: Codable, Sendable, Equatable {
    case string(String)
    case number(Double)
    case bool(Bool)
    case object([String: JSONValue])
    case array([JSONValue])
    case null

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([String: JSONValue].self) {
            self = .object(value)
        } else {
            self = .array(try container.decode([JSONValue].self))
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .string(let value): try container.encode(value)
        case .number(let value): try container.encode(value)
        case .bool(let value): try container.encode(value)
        case .object(let value): try container.encode(value)
        case .array(let value): try container.encode(value)
        case .null: try container.encodeNil()
        }
    }
}

public protocol PushAnalyticsTracking: AnyObject, Sendable {
    func track(_ name: String, properties: [String: JSONValue])
}

public extension Dictionary where Key == String, Value == Any {
    func trackerJSONValues() -> [String: JSONValue] {
        reduce(into: [String: JSONValue]()) { result, pair in
            result[pair.key] = JSONValue.from(pair.value)
        }
    }
}

public extension JSONValue {
    static func from(_ value: Any?) -> JSONValue {
        switch value {
        case nil:
            return .null
        case let value as JSONValue:
            return value
        case let value as String:
            return .string(value)
        case let value as Bool:
            return .bool(value)
        case let value as Int:
            return .number(Double(value))
        case let value as Double:
            return .number(value)
        case let value as Float:
            return .number(Double(value))
        case let value as [String: Any]:
            return .object(value.trackerJSONValues())
        case let value as [Any]:
            return .array(value.map(JSONValue.from))
        default:
            return .string(String(describing: value!))
        }
    }
}
