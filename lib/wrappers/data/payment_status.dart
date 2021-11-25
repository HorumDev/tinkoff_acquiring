enum PaymentStatus {
  /// Платёж создан
  created,

  /// Отмена платежа
  cancelled,
  preauthorizing,

  /// Покупатель перенаправлен на страницу оплаты
  formshowed,

  /// Система начала обработку оплаты платежа
  authorizing,

  /// Средства заблокированы, но не списаны
  authorized,

  /// Покупатель начал аутентификацию по протоколу `3DSecure`. Статус может быть конечным, если клиент закрыл страницу ACS или не ввел код подтверждения 3Ds
  checking3ds,

  /// Покупатель завершил проверку 3DSecure
  checked3ds,

  /// Начало отмены блокировки средств
  reversing,

  /// Денежные средства разблокированы
  reversed,

  /// Начало списания денежных средств
  confirming,

  /// Денежные средства успешно списаны
  confirmed,

  /// Начало возврата денежных средств
  refunding,

  /// Произведен возврат денежных средств
  refunded,

  /// Произведен частичный возврат денежных средств
  refundedPartial,

  /// Ошибка платежа. Истекли попытки оплаты
  rejected,
  completed,
  hold,
  hold3ds,
  loop,
  unknown,

  /// Ожидаем оплату по QR-коду
  formShowed
}

Map<String, PaymentStatus> decodingMap = {
  "NEW": PaymentStatus.created,
  "CANCELLED": PaymentStatus.cancelled,
  "PREAUTHORIZING": PaymentStatus.preauthorizing,
  "FORMSHOWED": PaymentStatus.formShowed,
  "AUTHORIZING": PaymentStatus.authorizing,
  "AUTHORIZED": PaymentStatus.authorized,
  "3DS_CHECKING": PaymentStatus.checking3ds,
  "3DS_CHECKED": PaymentStatus.checked3ds,
  "REVERSING": PaymentStatus.reversing,
  "REVERSED": PaymentStatus.reversed,
  "CONFIRMING": PaymentStatus.confirming,
  "CONFIRMED": PaymentStatus.confirmed,
  "REFUNDING": PaymentStatus.refunding,
  "REFUNDED": PaymentStatus.refunded,
  "PARTIAL_REFUNDED": PaymentStatus.refundedPartial,
  "REJECTED": PaymentStatus.rejected,
  "COMPLETED": PaymentStatus.completed,
  "HOLD": PaymentStatus.hold,
  "3DSHOLD": PaymentStatus.hold3ds,
  "LOOP_CHECKING": PaymentStatus.loop,
  "UNKNOWN": PaymentStatus.unknown,
  "FORM_SHOWED": PaymentStatus.formshowed
};
