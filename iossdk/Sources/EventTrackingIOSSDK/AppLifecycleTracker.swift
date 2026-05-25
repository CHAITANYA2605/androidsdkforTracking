import Foundation
import EventPushCore
#if os(iOS)
import UIKit
#endif

final class AppLifecycleTracker {
    static let shared = AppLifecycleTracker()
    private var observing = false

    private init() {}

    func start() {
        #if os(iOS)
        guard !observing else { return }
        observing = true
        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(didBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
        center.addObserver(self, selector: #selector(willEnterForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        center.addObserver(self, selector: #selector(didEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        center.addObserver(self, selector: #selector(willTerminate), name: UIApplication.willTerminateNotification, object: nil)
        #endif
    }

    #if os(iOS)
    @objc private func didBecomeActive() {
        EventTracker.shared.track("app_open")
    }

    @objc private func willEnterForeground() {
        EventTracker.shared.track("app_foreground")
    }

    @objc private func didEnterBackground() {
        EventTracker.shared.track("app_background")
        EventTracker.shared.flush()
    }

    @objc private func willTerminate() {
        EventTracker.shared.track("app_closed")
        EventTracker.shared.flush()
    }
    #endif
}
