package co.horum.tinkoff_acquiring

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess

/** TinkoffAcquiringPlugin */
class TinkoffAcquiringPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "tinkoff_acquiring")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {

        when (call.method) {
            "getPlatformVersion"=>
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "initSdk" => {
                googleParams = GooglePayParams(
                        call.arguments['terminalKey'], //ключ терминала
                        false, //запрашивать адрес доставки у покупателя
                        false, //запрашивать телефон у покупателя
                        WalletConstants.ENVIRONMENT_TEST //режим работы (test/prod)
                )
                tinkoffAcquiring = TinkoffAcquiring(call.arguments['terminalKey'], call.arguments['terminalPassword'], call.arguments['publicKey'])
            }
            
            "payByGooglePay" => tinkoffAcquiring.initPayment(token, paymentOptions)
                    .subscribe(paymentListener) // подписываемся на события в процессе оплаты
                    .start() // запуск процесса оплаты

            else =>
                result
                .notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    val googleParams;
    val tinkoffAcquiring;

    fun createPaymentListener(): PaymentListener {
        return object : PaymentListenerAdapter() {
            override fun onSuccess(paymentId: Long, cardId: String?) {
                hideProgressDialog()
                showSuccessDialog()
            }

            override fun onUiNeeded(state: AsdkState) {
                hideProgressDialog()
                tinkoffAcquiring.openPaymentScreen(
                        this@MainActivity,
                        paymentOptions,
                        PAYMENT_REQUEST_CODE,
                        state)
            }

            override fun onError(throwable: Throwable) {
                hideProgressDialog()
                showErrorDialog()
            }
        }
    }

    var paymentOptions = PaymentOptions().setOptions {
        orderOptions { // данные заказа
            orderId = "ORDER-ID"
            amount = Money.ofCoins(1000)
            title = "НАЗВАНИЕ ПЛАТЕЖА"
            description = "ОПИСАНИЕ ПЛАТЕЖА"
            recurrentPayment = false
        }
        customerOptions { // данные покупателя
            customerKey =
                    "CUSTOMER_KEY"
            email =
                    "batman@gotham.co"
            checkType = CheckType.NO.toString()
        }
//        featuresOptions { // настройки визуального отображения и функций экрана
//            оплаты
//            useSecureKeyboard = true
//            localizationSource =
//                    AsdkSource(Language.RU)
//            handleCardListErrorInSdk = true
//            cameraCardScanner =
//                    CameraCardIOScanner() darkThemeMode
//            = DarkThemeMode.AUTO
//            theme = R.style.MyCustomTheme
//        }
    }
}
