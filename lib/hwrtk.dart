import 'dart:async';
import 'dart:isolate';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class Hwrtk {
  static const MethodChannel _channel = MethodChannel('hwrtk');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static final _onNmeaChange = StreamController<String>.broadcast();

  static Stream<String> get onNmeaChange => _onNmeaChange.stream;

  static Future _handleMessages(MethodCall call) async {
    switch (call.method) {
      case 'onNmeaChange':
        _onNmeaChange.add(call.arguments);
    }
  }

  static connect(String address) {
    _channel.setMethodCallHandler(_handleMessages);
    Isolate.current.addErrorListener(RawReceivePort((dynamic pair) {
      var isolateError = pair as List<dynamic>;
      var _error = isolateError.first;
      var _stackTrace = isolateError.last;
      Zone.current.handleUncaughtError(_error, _stackTrace);
    }).sendPort);
    runZonedGuarded(() {
      _channel.invokeMethod('connect');
    }, (error, stack) {
      debugPrint("RTK:${error.toString()}");
    });
    FlutterError.onError = (details) {
      if (details.stack == null) {
        FlutterError.presentError(details);
      }
      Zone.current.handleUncaughtError(details.exception, details.stack!);
    };
  }

  static Future<bool> close() async {
    bool result = await _channel.invokeMethod("close");
    return result;
  }

  static Future<String> status() async {
    dynamic status;

    runZonedGuarded(() async {
      status = await _channel.invokeMethod("status");
    },(error, stack) {
      debugPrint(error.toString());
    });

    switch (status) {
      case 0:
        return "disconnected";
      case 1:
        return "connecting";
      case 2:
        return "connected";
      default:
        return "unknown";
    }

  }
}
