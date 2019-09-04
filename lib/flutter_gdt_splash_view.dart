import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FlutterGdtSplashView extends StatefulWidget {
  final String appId;
  final String positionId;
  final Function onLoaded;
  final Function onError;
  final Function onClick;
  final Function onTick;
  final Function onFinish;

  FlutterGdtSplashView(
    this.appId,
    this.positionId, {
    this.onLoaded,
    this.onError,
    this.onClick,
    this.onTick,
    this.onFinish,
  });

  @override
  _FlutterGdtSplashViewState createState() => _FlutterGdtSplashViewState();
}

class _FlutterGdtSplashViewState extends State<FlutterGdtSplashView> {
  MethodChannel _channel;
  int _channelId;
  bool loaded = false;

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return _androidView();
    }

    if (defaultTargetPlatform == TargetPlatform.iOS) {
      return _iosView();
    }

    print('GDT Splash 不支持的平台');
    return Container(width: 0, height: 0);
  }

  Future<dynamic> _onMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'adClicked':
        {
          widget.onClick?.call(() {
            _loadView();
          });
          break;
        }
      case 'adTick':
        {
          widget.onTick?.call(() {
            _loadView();
          });
          break;
        }
      case 'adDismissed':
        {
          widget.onFinish?.call();
          break;
        }
      default:
        break;
    }
  }

  _loadView() async {
    if (_channel == null) {
      _channel =
          MethodChannel("flutter_gdt_splash_ad_view_" + _channelId.toString());
      _channel.setMethodCallHandler(_onMethodCall);
    }

    final result = await _channel.invokeMethod("renderSplashAd", {
      "appId": widget.appId,
      "positionId": widget.positionId,
    });

    if (mounted && loaded != result) {
      setState(() {
        loaded = result;
      });
    }

    if (result == true) {
      widget.onLoaded?.call(() {
        _loadView();
      });
    } else {
      widget.onError?.call(() {
        _loadView();
      });
    }
  }

  Widget _androidView() {
    return AndroidView(
      viewType: "flutter_gdt_splash_ad_view",
      onPlatformViewCreated: (id) async {
        _channelId = id;
        _loadView();
      },
    );
  }

  Widget _iosView() {
    return UiKitView(
      viewType: "flutter_gdt_splash_ad_view",
      creationParams: <String, dynamic>{
        "appId": widget.appId,
        "positionId": widget.positionId,
      },
      creationParamsCodec: new StandardMessageCodec(),
      onPlatformViewCreated: (int id) async {
        _channelId = id;
        _loadView();
      },
    );
  }
}
