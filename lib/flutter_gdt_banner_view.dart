import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FlutterGdtBannerView extends StatefulWidget {
  final String appId;
  final String positionId;
  final int width;
  final int height;
  final Function? onLoaded;
  final Function? onError;
  final Function? onClick;
  final Function? onExposure;

  FlutterGdtBannerView(
    this.appId,
    this.positionId, {
    required this.width,
    required this.height,
    this.onLoaded,
    this.onError,
    this.onClick,
    this.onExposure,
  });

  @override
  _FlutterGdtBannerViewState createState() => _FlutterGdtBannerViewState();
}

class _FlutterGdtBannerViewState extends State<FlutterGdtBannerView> {
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

    print('GDT Banner 不支持的平台');
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
      case 'adLoaded':
        {
          widget.onLoaded?.call(() {
            _loadView();
          });
          break;
        }
      case 'adExposure':
        {
          widget.onExposure?.call(() {
            _loadView();
          });
          break;
        }
      case 'adError':
        {
          widget.onError?.call(() {
            _loadView();
          });
          break;
        }
      default:
        break;
    }
  }

  _loadView() async {
    if (_channel == null) {
      _channel =
          MethodChannel("flutter_gdt_banner_ad_view_" + _channelId.toString());
      _channel.setMethodCallHandler(_onMethodCall);
    }

    final result = await _channel.invokeMethod("renderBannerAd", {
      "appId": widget.appId,
      "positionId": widget.positionId,
    });

    if (mounted && loaded != result) {
      setState(() {
        loaded = result;
      });
    }

    if (result != true) {
      widget.onError?.call(() {
        _loadView();
      });
    }
  }

  Widget _androidView() {
    return Container(
      height: loaded ? widget.height.toDouble() : 1,
      width: loaded ? widget.width.toDouble() : 1,
      child: AndroidView(
        viewType: "flutter_gdt_banner_ad_view",
        onPlatformViewCreated: (id) async {
          _channelId = id;
          _loadView();
        },
      ),
    );
  }

  Widget _iosView() {
    return Container(
      height: loaded ? widget.height.toDouble() : 1,
      width: loaded ? widget.width.toDouble() : 1,
      child: UiKitView(
        viewType: "flutter_gdt_banner_ad_view",
        creationParams: <String, dynamic>{
          "appId": widget.appId,
          "positionId": widget.positionId,
        },
        creationParamsCodec: new StandardMessageCodec(),
        onPlatformViewCreated: (int id) async {
          _channelId = id;
          _loadView();
        },
      ),
    );
  }
}
