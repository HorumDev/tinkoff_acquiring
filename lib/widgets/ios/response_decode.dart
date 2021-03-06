import 'dart:convert';
import 'dart:ffi';

import 'package:tinkoff_acquiring/widgets/android/info_toast.dart';
import 'package:tinkoff_acquiring/wrappers/data/coding_keys.dart';
import 'package:tinkoff_acquiring/wrappers/data/payment_status.dart';

PaymentStatus? iosResponseDecode(result){

  if(result is Array) {
    final decoded = jsonDecode(result.toString());
    final success = decoded[code_success];
    final status = decoded[code_status];
    return decodingMap[status];
  }
  //костыль
  //TODO return error from swift as JSON
  if(result.contains(code_success) || result.contains(code_success.toLowerCase())) {
    print(jsonDecode(result.toString()));

    final decoded = jsonDecode(result.toString());


    final success = decoded[code_success];
    final status = decoded[code_status];


    return decodingMap[status];
  }
  else {
    return PaymentStatus.rejected;
  }
}