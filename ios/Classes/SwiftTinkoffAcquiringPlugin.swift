import Flutter
import UIKit
import TinkoffASDKCore
import TinkoffASDKUI

public class SwiftTinkoffAcquiringPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "tinkoff_acquiring", binaryMessenger: registrar.messenger())
    let instance = SwiftTinkoffAcquiringPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    registrar.addApplicationDelegate(instance)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      if (call.method == "getPlatformVersion") {
                 result("iOS " + UIDevice.current.systemVersion)
             }
             else {if (call.method == "payByApplePay") {
                 print("payByApplePay swift")
                 if(sdk.canMakePaymentsApplePay(with: paymentApplePayConfiguration)){
                     let params = call.arguments as! [String: Any]
                // products = call.arguments["products"].map
                     amount=params["amount"] as? Double
               //  customerEmail = call.arguments["customerEmail"]
                // customerPhone = call.arguments["customerPhone"]
                 customerKey = params["customerKey"] as? String
                    self.payByApplePay(result)
                    }
                else{
                    result("ERROR")
                    }
                 
             } else{ if(call.method == "initSdk"){
                 print("initSdk swift")
                 let params = call.arguments as! [String: Any]
                 print(params)
                 // терминал и пароль
                 let credentional = AcquiringSdkCredential(terminalKey: params["terminalKey"] as! String, password: params["terminalPassword"] as! String, publicKey: params["publicKey"] as! String)
                 // конфигурация для старта sdk
                 let acquiringSDKConfiguration = AcquiringSdkConfiguration(credential: credentional)
                 // включаем логи, результаты работы запросов пишутся в консоль
                 acquiringSDKConfiguration.logger = AcquiringLoggerDefault()
                     sdk =  try? AcquiringUISDK(configuration: acquiringSDKConfiguration)
                 print("initSdk done")
                 print(sdk)
                 result(true)
             }}}

  }

  lazy var paymentApplePayConfiguration = AcquiringUISDK.ApplePayConfiguration()
  //var products: [Product] = []
  var amount: Double?
  var sdk: AcquiringUISDK!
  var customerEmail: String?
  var customerPhone: String?
  var customerKey: String?
    var window: UIWindow?



  func payByApplePay(_ result: @escaping FlutterResult) {
          sdk.presentPaymentApplePay(
            //on: self.window!.rootViewController!,
            on: UIApplication.shared.keyWindow!.rootViewController!,
            //on: FlutterViewController(),
            paymentData: createPaymentData(),
            viewConfiguration: AcquiringViewConfiguration(),
            paymentConfiguration: paymentApplePayConfiguration)
            { [weak self] response in result(Any?.self)
                  //self?.responseReviewing(response)
          }
  }
  private func createPaymentData() -> PaymentInitData {
        //  let amount = productsAmount()
          let randomOrderId = String(Int64(arc4random()))
          var paymentData = PaymentInitData(amount: NSDecimalNumber(value: amount ?? 0), orderId: randomOrderId, customerKey: customerKey)
          paymentData.description = "мандарин фудс"

//          var receiptItems: [Item] = []
//          products.forEach { product in
//              let item = Item(amount: product.price.int64Value * 100,
//                              price: product.price.int64Value * 100,
//                              name: product.name,
//                              tax: .vat10)
//              receiptItems.append(item)
//          }
//
//          paymentData.receipt = Receipt(shopCode: nil,
//                                        email: customerEmail,
//                                        taxation: .osn,
//                                        phone: customerPhone,
//                                        items: receiptItems,
//                                        agentData: nil,
//                                        supplierInfo: nil,
//                                        customer: nil,
//                                        customerInn: nil)

          return paymentData
      }
//     private func productsAmount() -> Double {
//             var amount: Double = 0
//
//             products.forEach { product in
//                 amount += product.price.doubleValue
//             }
//
//             return amount
//         }



}
