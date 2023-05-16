import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:hwrtk/hwrtk.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  String status = "";

  String hwrtkAddress = "";

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion =
          await Hwrtk.platformVersion ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Running on: $_platformVersion'),
        ),
        body: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              InkWell(
                  onTap: () {
                    Hwrtk.connect(hwrtkAddress);
                  },
                  child: const Text('连接设备', style: TextStyle(fontSize: 40))),
              InkWell(
                  onTap: () async {
                    var result = await Hwrtk.status();
                    setState(() {
                      status = result;
                    });
                  }, child: const Text('查询状态', style: TextStyle(fontSize: 40))
              ),
              Text.rich(TextSpan(children: [
                const TextSpan(text: "rtk状态：", style: TextStyle(fontSize: 40)),
                TextSpan(text: status,style: const TextStyle(fontSize: 40,color: Colors.cyan))
              ]))
            ]),
      ),
    );
  }
}
