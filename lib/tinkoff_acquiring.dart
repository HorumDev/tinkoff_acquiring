import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:tinkoff_acquiring/wrappers/product.dart';
import 'package:tinkoff_acquiring/wrappers/user.dart';

class TinkoffAcquiring {
  static late User _currentUser;

  static const MethodChannel _channel = MethodChannel('tinkoff_acquiring');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static initSdk(String terminalKey, String publicKey,
      [String? terminalPassword]) {
    if (defaultTargetPlatform == TargetPlatform.iOS &&
        terminalPassword == null) {
      return false;
    }
    print('initSdk from dart');
    return _channel.invokeMethod('initSdk', <String, dynamic>{
      'terminalKey': terminalKey,
      'publicKey': publicKey,
      'terminalPassword': terminalPassword
    });
  }

  static Future<bool> googlePay(String token) async {
    if (defaultTargetPlatform == TargetPlatform.iOS) return false;
    return true;
  }

  static set updateUser(User user) => _currentUser = user;

  static Future applePay(List<Product> products) async {
    if (defaultTargetPlatform == TargetPlatform.android) return false;

    double totalAmount = 0.0;
    for(Product p in products) {
      totalAmount+=p.price*p.amount*100;
    }

    return _channel.invokeMethod('payByApplePay', <String, dynamic>{
      'customerEmail': _currentUser.email,
      'customerPhone': _currentUser.phone,
      'customerKey': _currentUser.token,
      'amount': totalAmount
    });
  }
}
