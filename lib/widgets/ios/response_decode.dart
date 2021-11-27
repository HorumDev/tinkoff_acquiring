import 'dart:convert';

import 'package:tinkoff_acquiring/wrappers/data/coding_keys.dart';
import 'package:tinkoff_acquiring/wrappers/data/payment_status.dart';

String iosResponseDecode(result){
  print(jsonDecode(result.toString()));

  final decoded = jsonDecode(result.toString());

  final success = decoded[code_success];
  final status = decoded[code_status];

  if(success)
    return "Успешная оплата";
  else
    return decodingMap[status].toString();
}