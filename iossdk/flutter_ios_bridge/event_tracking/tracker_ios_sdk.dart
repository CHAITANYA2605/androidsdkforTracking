import 'package:flutter/services.dart';

class TrackerIosSdk {
  static const MethodChannel _channel = MethodChannel('event_tracking_ios_sdk');

  static Future<void> initialize({
    required String appId,
    required String serverApiKey,
    required String apiBaseUrl,
  }) {
    return _channel.invokeMethod('initialize', {
      'appId': appId,
      'serverApiKey': serverApiKey,
      'apiBaseUrl': apiBaseUrl,
    });
  }

  static Future<void> setUserId(String userId) {
    return _channel.invokeMethod('setUserId', {'userId': userId});
  }

  static Future<void> track(String name, [Map<String, Object?> properties = const {}]) {
    return _channel.invokeMethod('track', {
      'name': name,
      'properties': properties,
    });
  }

  static Future<void> flush() {
    return _channel.invokeMethod('flush');
  }

}
