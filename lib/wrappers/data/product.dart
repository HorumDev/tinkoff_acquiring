class Product{
  final int  amount;
  final double price;
  final String name;

  Product({required this.amount, required this.price, required this.name});



  toMap()=>{'amount':amount,'price':price};
}