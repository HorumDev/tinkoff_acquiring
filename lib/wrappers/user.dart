class User {
  final String email, phone, token;
  final String? merchant;

  User({this.merchant, required this.email, required this.phone, required this.token});
}
