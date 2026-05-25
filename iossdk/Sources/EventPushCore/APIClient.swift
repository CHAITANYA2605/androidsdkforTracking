import Foundation

public final class APIClient: Sendable {
    private let session: URLSession
    private let encoder: JSONEncoder

    public init(session: URLSession = .shared) {
        self.session = session
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        self.encoder = encoder
    }

    public func sendJSON<T: Encodable>(_ payload: T, to url: URL, authorization: String? = nil) async throws {
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let authorization {
            request.setValue(authorization, forHTTPHeaderField: "Authorization")
        }
        request.httpBody = try encoder.encode(payload)

        let (_, response) = try await send(request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw TrackerError.httpFailure
        }
    }

    private func send(_ request: URLRequest) async throws -> (Data, URLResponse) {
        try await withCheckedThrowingContinuation { continuation in
            let task = session.dataTask(with: request) { data, response, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard let data, let response else {
                    continuation.resume(throwing: TrackerError.httpFailure)
                    return
                }
                continuation.resume(returning: (data, response))
            }
            task.resume()
        }
    }
}

public enum TrackerError: Error {
    case notConfigured
    case httpFailure
    case missingUserId
}

public extension URL {
    func trackerAppendingPath(_ path: String) -> URL {
        var result = self
        path.split(separator: "/").forEach { result.appendPathComponent(String($0)) }
        return result
    }
}
