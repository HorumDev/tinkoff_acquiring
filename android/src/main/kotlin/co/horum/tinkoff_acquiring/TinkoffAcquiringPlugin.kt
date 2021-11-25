package co.horum.tinkoff_acquiring

import androidx.annotation.NonNull
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.PaymentListener
import ru.tinkoff.acquiring.sdk.payment.PaymentListenerAdapter
import ru.tinkoff.acquiring.sdk.utils.Money
import java.util.*
import kotlin.math.abs

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess
import ru.tinkoff.acquiring.sdk.payment.PaymentState
import ru.tinkoff.acquiring.sdk.utils.GooglePayHelper

import ru.tinkoff.acquiring.sdk.models.GooglePayParams

/** TinkoffAcquiringPlugin */
class TinkoffAcquiringPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private lateinit var googleParams: GooglePayParams;
    private lateinit var tinkoffAcquiring: TinkoffAcquiring;

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "tinkoff_acquiring")
        channel.setMethodCallHandler(this)
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {

        when (call.method) {
            "getPlatformVersion"->
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "initSdk" -> {
                val params = call.arguments
                var env = WalletConstants.ENVIRONMENT_TEST
                if(params["env"]==1) env = WalletConstants.ENVIRONMENT_PROD

                googleParams = GooglePayParams(
                    params["terminalKey"], //ключ терминала
                        false, //запрашивать адрес доставки у покупателя
                        false, //запрашивать телефон у покупателя
                    env //режим работы (test/prod)
                )
                tinkoffAcquiring = TinkoffAcquiring(params["terminalKey"], params["terminalPassword"], params["publicKey"])
            }
            
            "pay" -> {

                val params = call.arguments
                val paymentOptions = PaymentOptions().setOptions {
                    orderOptions { // данные заказа
                        orderId = "ORDER-ID"
                        amount = params["Amount"]
                      //  title = "НАЗВАНИЕ ПЛАТЕЖА"
                        description = params["description"]
                        recurrentPayment = false
                    }
                    customerOptions { // данные покупателя
                        customerKey =
                            params["customerKey"]
//                        email =
//                            params["description"]
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

                when(params["payMethod"]) {

                    1->
                    tinkoffAcquiring.initPayment(token, paymentOptions)
                            .subscribe(createPaymentListener()) // подписываемся на события в процессе оплаты
                            .start()
                    2 -> tinkoffAcquiring.initPayment(token, paymentOptions)
                        .subscribe(createPaymentListener()) // подписываемся на события в процессе оплаты
                        .start()
                }// запуск процесса оплаты
            }
            else ->
                result
                .notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }



    fun createPaymentListener(): PaymentListener {
        return object : PaymentListenerAdapter() {
            override fun onSuccess(paymentId: Long, cardId: String?) {
                result.success(smth)
//                hideProgressDialog()
//                showSuccessDialog()
            }

            override fun onUiNeeded(state: AsdkState) {
                //hideProgressDialog()
                tinkoffAcquiring.openPaymentScreen(
                        this@MainActivity,
                        paymentOptions,
                        PAYMENT_REQUEST_CODE,
                        state)
            }

            override fun onError(throwable: Throwable) {
                result.error(smth)
//                hideProgressDialog()
//                showErrorDialog()
            }
        }
    }

    //var paymentOptions =
}
