import 'package:tinkoff_acquiring/wrappers/data/payment_status.dart';

PaymentStatus checkAndroidPayResult(result){
  if(result == 'success')
    return PaymentStatus.completed;
  else
    return PaymentStatus.rejected;
}