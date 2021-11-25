import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';

showInfoDialog(context, resultString) {
  // showDialog(
  //     context: context,
  //     builder: (context) => Dialog(child: Container(
  //         padding: EdgeInsets.all(16),
  //         decoration: ShapeDecoration(
  //           shape: RoundedRectangleBorder(
  //             borderRadius: BorderRadius.circular(26)
  //           )
  //         ),
  //         child: Text('OK'))));
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
