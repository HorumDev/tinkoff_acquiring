import Flutter
import UIKit
import TinkoffASDKCore
import TinkoffASDKUI

public class SwiftTinkoffAcquiringPlugin: NSObject, FlutterPlugin {
    
    
    var window: UIWindow?
    var controller: UIViewController!
    
    lazy var paymentApplePayConfiguration = AcquiringUISDK.ApplePayConfiguration()
    //var products: [Product] = []
    var amount: Double?
    var sdk: AcquiringUISDK!
    var customerEmail: String?
    var customerPhone: String?
    var customerKey: String?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "tinkoff_acquiring", binaryMessenger: registrar.messenger())
        let instance = SwiftTinkoffAcquiringPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        //registrar.addApplicationDelegate(instance)
//        window = UIApplication.shared.window
//        controller = UIApplication.shared.keyWindow!.rootViewController as! UIViewController
    }
    
   
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        //window = UIApplication.shared.window
        controller = UIApplication.shared.keyWindow!.rootViewController as! UIViewController
   
        switch call.method {
        case "getPlatformVersion":
            result("iOS " + UIDevice.current.systemVersion)
        case "canMakePayments":
            result(sdk.canMakePaymentsApplePay(with: paymentApplePayConfiguration))
        case "pay":
            print("pay swift")
            if(sdk.canMakePaymentsApplePay(with: paymentApplePayConfiguration)){
                let params = call.arguments as! [String: Any]
                
                amount=params["Amount"] as! Double
                //  customerEmail = call.arguments["customerEmail"]
                // customerPhone = call.arguments["customerPhone"]
                customerKey = params["customerKey"] as! String
                
                print(params["payMethod"])

                 var orderId = String(Int64(arc4random()))
                 if(params["orderId"] != nil){
                 orderId = params["orderId"] as! String
                 }
                
                var paymentData = PaymentInitData(amount: NSDecimalNumber(value: amount ?? 0), orderId: orderId, customerKey: customerKey)
                
                paymentData.description = params["description"] as? String
                
                switch params["payMethod"] as? NSNumber{
                case 0:
                    paymentApplePayConfiguration = AcquiringUISDK.ApplePayConfiguration()
                    paymentApplePayConfiguration.merchantIdentifier =   params["merchant"] as! String
                    sdk.presentPaymentApplePay(
                        on: UIApplication.shared.keyWindow!.rootViewController!,
                        paymentData: paymentData,
                        viewConfiguration: AcquiringViewConfiguration(),
                        paymentConfiguration: paymentApplePayConfiguration)
                    {  response in
                        //result(response)
                        self.responseReviewing(response,result)
                    }
                    
                case 2:
                    
                    //paymentData.savingAsParentPayment = true
                    
                    
                    sdk.presentPaymentView(
                        on: UIApplication.shared.keyWindow!.rootViewController!,
                        paymentData: paymentData,
                        configuration: AcquiringViewConfiguration()
                    )
                    {  response in
                        //result(response)
                        self.responseReviewing(response,result)
                    }
                default:
                    print("default")
                }
            }
            else{
                result("ERROR")
            }
            
        case "initSdk":
            print("initSdk swift")
            let params = call.arguments as! [String: Any]
            print(params)
            // терминал и пароль
            let credentional = AcquiringSdkCredential(terminalKey: params["terminalKey"] as! String, password: params["terminalPassword"] as! String, publicKey: params["publicKey"] as! String)
            // конфигурация для старта sdk
            var id = params["env"] as! Int
            var env: AcquiringSdkEnvironment
            
            if( id == 0){
                env = AcquiringSdkEnvironment.test
            }else{
                env = AcquiringSdkEnvironment.prod}
            let acquiringSDKConfiguration = AcquiringSdkConfiguration(credential: credentional, server: env)
            // включаем логи, результаты работы запросов пишутся в консоль
            acquiringSDKConfiguration.logger = AcquiringLoggerDefault()
            sdk =  try? AcquiringUISDK(configuration: acquiringSDKConfiguration )
            print("initSdk done")
            print(sdk)
            result(true)
            
            
        default:
            print("default")
        }
    }
    
    
    
    
    
    
//
//     private func createPaymentData() -> PaymentInitData {
//         //  let amount = productsAmount()
//         let randomOrderId = String(Int64(arc4random()))
//         var paymentData = PaymentInitData(amount: NSDecimalNumber(value: amount ?? 0), orderId: randomOrderId, customerKey: customerKey)
//
//
//         //          var receiptItems: [Item] = []
//         //          products.forEach { product in
//         //              let item = Item(amount: product.price.int64Value * 100,
//         //                              price: product.price.int64Value * 100,
//         //                              name: product.name,
//         //                              tax: .vat10)
//         //              receiptItems.append(item)
//         //          }
//         //
//         //          paymentData.receipt = Receipt(shopCode: nil,
//         //                                        email: customerEmail,
//         //                                        taxation: .osn,
//         //                                        phone: customerPhone,
//         //                                        items: receiptItems,
//         //                                        agentData: nil,
//         //                                        supplierInfo: nil,
//         //                                        customer: nil,
//         //                                        customerInn: nil)
//
//         return paymentData
//     }
    
    
    private func responseReviewing(_ response: Result<PaymentStatusResponse, Error>,_ flutterResult: @escaping FlutterResult) {
        
        UIApplication.shared.keyWindow!.rootViewController = controller as! UIViewController
        UIApplication.shared.keyWindow!.makeKeyAndVisible()
        
        switch response {
        case let .success(result):
            print("result")
            print(result)
            flutterResult( try?String(data: JSONEncoder().encode(result), encoding: String.Encoding.utf8) )
            //flutterResult(try? result.encode(to: JSONEncoder() as! Encoder) )
        case let .failure(error):
            print("error")
            print(error)
            flutterResult(error.localizedDescription)
            //flutterResult( try?String(data: JSONEncoder().encode(result), encoding: String.Encoding.utf8)  )
        }
    }
    
}
