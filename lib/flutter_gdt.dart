import 'dart:async';
import 'package:flutter/services.dart';

export 'flutter_gdt_native_express_view.dart';
export 'flutter_gdt_splash_view.dart';
export 'flutter_gdt_banner_view.dart';

class FlutterGdt {
  static const MethodChannel _channel = MethodChannel('flutter_gdt');
  static Future<bool> checkPermissions() async {
    return await _channel.invokeMethod("checkPermissions");
  }

  static Future<void> preloadNativeExpressView(NativeExpressParam param) async {
    await _channel.invokeMethod("preloadNativeExpress", {
      "appId": param.appId,
      "positionId": param.positionId,
      "width": param.ptWidth,
      "height": param.ptHeight,
      "preloadCount": param.preloadCount ?? 1,
    });
  }

  static Future<void> loadSplash(NativeExpressParam param) async {
    await _channel.invokeMethod("loadActivitySplashAd", {
      "appId": param.appId,
      "positionId": param.positionId,
    });
  }
}

class NativeExpressParam {
  String appId;
  String positionId;
  int ptWidth;
  int ptHeight;
  int preloadCount;

  NativeExpressParam(
      {this.appId,
      this.positionId,
      this.ptWidth,
      this.ptHeight,
      this.preloadCount});
}
