class User {
  final String email, phone, token;
  final String? merchant, description,notificationUrl;

  User({this.notificationUrl,
      this.description,
      this.merchant,
      required this.email,
      required this.phone,
      required this.token});
}
