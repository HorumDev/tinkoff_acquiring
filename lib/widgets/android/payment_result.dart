import 'package:tinkoff_acquiring/wrappers/data/payment_status.dart';

PaymentStatus checkAndroidPayResult(result){
  if(result == 'seccess')
    return PaymentStatus.completed;
  else
    return PaymentStatus.cancelled;
}