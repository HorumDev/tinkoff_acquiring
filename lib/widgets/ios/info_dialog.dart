import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';

showInfoDialog(context, resultString) {
  showCupertinoDialog(
      context: context,
      builder: (context) => CupertinoAlertDialog(
            title: Text(resultString),
            actions: [
              CupertinoDialogAction(
                  child: Text('OK'),
                  onPressed: () => Navigator.of(context).pop())
            ],
          ));
}
