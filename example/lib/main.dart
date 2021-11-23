import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:tinkoff_acquiring/tinkoff_acquiring.dart';
import 'package:tinkoff_acquiring/wrappers/product.dart';
import 'package:tinkoff_acquiring/wrappers/user.dart';

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

  String errorMessage = 'error';

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
          await TinkoffAcquiring.platformVersion ?? 'Unknown platform version';
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
          title: const Text('Plugin example app'),
        ),
        body: Column(children: [
          Text('Running on: $_platformVersion\n'),
          ElevatedButton(
              onPressed: () => TinkoffAcquiring.initSdk(
                  '1585906902098',
                  'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv5yse9ka3ZQE0feuGtemYv3IqOlLck8zHUM7lTr0za6lXTszRSXfUO7jMb+L5C7e2QNFs+7sIX2OQJ6a+HG8kr+jwJ4tS3cVsWtd9NXpsU40PE4MeNr5RqiNXjcDxA+L4OsEm/BlyFOEOh2epGyYUd5/iO3OiQFRNicomT2saQYAeqIwuELPs1XpLk9HLx5qPbm8fRrQhjeUD5TLO8b+4yCnObe8vy/BMUwBfq+ieWADIjwWCMp2KTpMGLz48qnaD9kdrYJ0iyHqzb2mkDhdIzkim24A3lWoYitJCBrrB2xM05sm9+OdCI1f7nPNJbl5URHobSwR94IRGT7CJcUjvwIDAQAB',
                  '2gat6poci9q3edms'),
              child: const Text('init Tinkoff')),
          ElevatedButton(
              onPressed: () {
                TinkoffAcquiring.updateUser = User(
                    token: 'fgergreg',
                    email: 'example.com',
                    phone: '+79217483843',
                    merchant: 'merchant.ru.mandarineda.mandarinapp.pay');

                TinkoffAcquiring.pay([
                  Product(name: 'pizza', amount: 2, price: 213.11),
                  Product(name: 'pasta', amount: 1, price: 210),
                  Product(name: 'sushi', amount: 4, price: 99.99),
                ], PaymentMetod.applePay);
              },
              child: const Text('pay apple pay')),
          ElevatedButton(
              onPressed: () {
                TinkoffAcquiring.updateUser = User(
                    token: 'fgergreg',
                    email: 'example.com',
                    phone: '+79217483843');

                TinkoffAcquiring.pay([
                  Product(name: 'pizza', amount: 2, price: 213.11),
                  Product(name: 'pasta', amount: 1, price: 210),
                  Product(name: 'sushi', amount: 4, price: 99.99),
                ], PaymentMetod.card)
                    .then((value) => setState(() => errorMessage = value));
              },
              child: const Text('pay card')),
          Text(errorMessage)
        ]),
      ),
    );
  }
}
