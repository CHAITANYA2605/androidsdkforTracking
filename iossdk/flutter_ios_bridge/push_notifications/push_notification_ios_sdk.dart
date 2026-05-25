import 'package:flutter/services.dart';

class PushNotificationIosSdk {
  static const MethodChannel _channel = MethodChannel('push_notification_ios_sdk');

  static Future<void> initialize({
    String pushApiBaseUrl = 'http://localhost:8082/api/v1',
  }) {
    return _channel.invokeMethod('initialize', {
      'pushApiBaseUrl': pushApiBaseUrl,
    });
  }

  static Future<void> setUserId(String userId) {
    return _channel.invokeMethod('setUserId', {'userId': userId});
  }

  static Future<void> registerPushToken(String fcmToken) {
    return _channel.invokeMethod('registerPushToken', {'fcmToken': fcmToken});
  }

  static Future<void> handleRemoteNotification(Map<String, Object?> userInfo, {bool opened = false}) {
    return _channel.invokeMethod('handleRemoteNotification', {
      'userInfo': userInfo,
      'opened': opened,
    });
  }
}
