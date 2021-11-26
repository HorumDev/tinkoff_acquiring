import 'package:flutter/material.dart';

showInfoDialog(context, resultString) {
  return ScaffoldMessenger.of(context).showSnackBar(SnackBar(
    content: Text(resultString),
  ));
}