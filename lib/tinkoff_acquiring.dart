import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:tinkoff_acquiring/widgets/android/payment_result.dart';
import 'package:tinkoff_acquiring/widgets/ios/response_decode.dart';
import 'package:tinkoff_acquiring/wrappers/data/coding_keys.dart';
import 'package:tinkoff_acquiring/wrappers/data/payment_status.dart';
import 'package:tinkoff_acquiring/wrappers/data/product.dart';
import 'package:tinkoff_acquiring/wrappers/data/user.dart';

class TinkoffAcquiring {
  static late User _currentUser;
  static bool _sdkInited = false;

  static Stream? paymentProcessStream;

  static const MethodChannel _channel = MethodChannel('tinkoff_acquiring'); // установка связи с нативкой

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static initSdk(
      {required String terminalKey,
      required String publicKey,
      //only required for ios
      String? terminalPassword,
      ServerEnvironment env = ServerEnvironment.test}) async {
    if (defaultTargetPlatform == TargetPlatform.iOS &&
        terminalPassword == null) {
      return false;
    }
    //костыль
    if(_sdkInited) return _sdkInited;
    print('initSdk from dart');
    _sdkInited = await _channel.invokeMethod('initSdk', <String, dynamic>{
      'terminalKey': terminalKey,
      'publicKey': publicKey,
      'terminalPassword': terminalPassword,
      'env': env.index
    });
    return _sdkInited;
  }

  //TODO google pay
  static Future<bool> canUseAppleOrGooglePay() async {
    if (!_sdkInited) throw AssertionError('sdk not inited');
    return (await _channel.invokeMethod('canMakePayments')) as bool;
  }

  // static Future googlePay(String token) async {
  //   //TODO catch error
  //   if (defaultTargetPlatform == TargetPlatform.iOS) return false;
  //   return _channel.invokeMethod('payByGooglePay', <String, dynamic>{
  //     'customerEmail': _currentUser.email,
  //     'customerPhone': _currentUser.phone,
  //     'customerKey': _currentUser.token,
  //     'token': token
  //   });
  // }

  static set updateUser(User user) => _currentUser = user;

  static Future pay(double totalAmount, PaymentMetod method) async {
    //TODO catch errors
    //if (defaultTargetPlatform == TargetPlatform.android) return false;
    if (!_sdkInited) throw AssertionError('sdk not inited');
    if (_currentUser == null) throw AssertionError('user not inited');
    if (method == PaymentMetod.applePay && _currentUser.merchant == null)
      throw AssertionError('merchant has not set');

    // double totalAmount = 0.0;
    // for (Product p in products) {
    //   totalAmount += p.price * p.amount;
    // }

    return _pay(method, totalAmount);
  }

  static Future<PaymentStatus?> _pay(PaymentMetod method, totalAmount) async {
    final payResult = await _channel.invokeMethod('pay', <String, dynamic>{
      'customerEmail': _currentUser.email,
      'customerPhone': _currentUser.phone,
      'customerKey': _currentUser.token,
      code_amount: totalAmount,
      'description': _currentUser.description,
      'payMethod': method.index,
      'merchant': _currentUser.merchant
    });

    print('payResult');
    print(payResult);
    if (defaultTargetPlatform == TargetPlatform.android)
      return checkAndroidPayResult(payResult);
    if (defaultTargetPlatform == TargetPlatform.iOS)
      return iosResponseDecode(payResult);

   // return payResult is String ? payResult : payResult.toString();
  }

  static Future<bool> canUseAppleOrGooglePay(PaymentMetod method) async {
    //TODO catch errors
    //if (defaultTargetPlatform == TargetPlatform.android) return false;
    if (!_sdkInited) throw AssertionError('sdk not inited');
    if (_currentUser == null) throw AssertionError('user not inited');


    return _checkGooglePay(method);
  }

  static _checkGooglePay(PaymentMetod method) async {
    final bool resultAvailability = await _channel.invokeMethod('checkGooglePay', <String, dynamic>{});

    print('resultAvailability');
    print(resultAvailability);

    return resultAvailability;
  }
}

enum PaymentMetod { applePay, googlePay, card }
enum ServerEnvironment { test, secure }
//enum Tax {}
