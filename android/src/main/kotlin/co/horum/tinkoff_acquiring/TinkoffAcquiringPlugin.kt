package co.horum.tinkoff_acquiring

import android.app.Activity
import android.content.Context
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.annotation.NonNull
import com.google.android.gms.wallet.WalletConstants
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.PaymentListener
import ru.tinkoff.acquiring.sdk.payment.PaymentListenerAdapter
import ru.tinkoff.acquiring.sdk.utils.Money
import java.util.*
import kotlin.math.abs
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.RESULT_ERROR
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.PluginRegistry
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
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
import ru.tinkoff.acquiring.sdk.models.enums.CheckType

/** TinkoffAcquiringPlugin */
class TinkoffAcquiringPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private lateinit var googleParams: GooglePayParams;
    private lateinit var tinkoffAcquiring: TinkoffAcquiring;
    private lateinit var terminalKey: String;
    private lateinit var currentPaymentOptions: PaymentOptions;
    private lateinit var applicationContext: Context;
    private lateinit var activity: Activity
    private lateinit var paymentListener: PaymentListener
    private lateinit var globalResult: Result


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "tinkoff_acquiring")
        channel.setMethodCallHandler(this)
        applicationContext = flutterPluginBinding.applicationContext
    }

    override fun onDetachedFromActivity() {
        TODO("Not yet implemented")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        TODO("Not yet implemented")
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity;
        binding.addActivityResultListener { requestCode, resultCode, data ->
            onActivityResult(
                requestCode,
                resultCode,
                data
            )
        }

    }


    private fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (requestCode) {
            PAYMENT_REQUEST_CODE, DYNAMIC_QR_PAYMENT_REQUEST_CODE -> handlePaymentResult(resultCode)
            GOOGLE_PAY_REQUEST_CODE -> handleGooglePayResult(resultCode, data)
            //else -> super.onActivityResult(requestCode, resultCode, data)
        }
        return true
    }

    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        globalResult = result
        when (call.method) {
            "getPlatformVersion" ->
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "initSdk" -> {
                val params = call.arguments as Map<*, *>
                var env = WalletConstants.ENVIRONMENT_TEST
                if ((params["env"] as Int) == 1) env = WalletConstants.ENVIRONMENT_PRODUCTION

                terminalKey = params["terminalKey"] as String

                googleParams = GooglePayParams(
                    params["terminalKey"] as String, //ключ терминала
                    false, //запрашивать адрес доставки у покупателя
                    false, //запрашивать телефон у покупателя
                    env //режим работы (test/prod)
                )
                tinkoffAcquiring = TinkoffAcquiring(
                    terminalKey,
                    params["publicKey"] as String
                )
            }

            "pay" -> {
                paymentListener = createPaymentListener(result)
                val params = call.arguments as Map<*, *>
                currentPaymentOptions = PaymentOptions().setOptions {
                    orderOptions { // данные заказа
                        orderId = "ORDER-ID"
                        amount = Money.ofCoins(params["Amount"] as Long)
                        //title = "Оплата"
                        description = params["description"] as String
                        recurrentPayment = false
                    }
                    customerOptions { // данные покупателя
                        customerKey =
                            params["customerKey"] as String
                        email =
                            params["customerEmail"] as String
                        checkType = CheckType.HOLD.toString()
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

                when (params["payMethod"]) {

                    1 -> {
                        val googlePayHelper = GooglePayHelper(googleParams)
                        googlePayHelper.initGooglePay(applicationContext) { ready ->
                            if (ready) {
                                googlePayHelper.openGooglePay(
                                    activity,
                                    currentPaymentOptions.order.amount,
                                    GOOGLE_PAY_REQUEST_CODE
                                )
//                    tinkoffAcquiring.initPayment(token, currentPaymentOptions)
//                            .subscribe(createPaymentListener()) // подписываемся на события в процессе оплаты
//                            .start()
                            } else {
                                result.error(
                                    "1",
                                    "initGooglePay failed",
                                    googlePayHelper.toString()
                                )
                            }
                        }
                    }
                    2 -> tinkoffAcquiring.openPaymentScreen(
                        activity,
                        currentPaymentOptions,
                        PAYMENT_REQUEST_CODE
                    )
//                        .subscribe(createPaymentListener()) // подписываемся на события в процессе оплаты
//                        .start()
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

//TODO make result stream to flutter
    fun createPaymentListener(result: Result): PaymentListener {
        return object : PaymentListenerAdapter() {

            override fun onStatusChanged(state: PaymentState?) {
                if (state == PaymentState.STARTED) {
                    //showProgressDialog()
                    //TODO notify about it
                }
            }

            override fun onSuccess(paymentId: Long, cardId: String?, rebillId: String?) {
                result.success("success")
//                hideProgressDialog()
//                showSuccessDialog()
            }

            override fun onUiNeeded(state: AsdkState) {
                //hideProgressDialog()
                tinkoffAcquiring.openPaymentScreen(
                    activity,
                    currentPaymentOptions,
                    PAYMENT_REQUEST_CODE,
                    state
                )
            }

            override fun onError(throwable: Throwable) {
                result.error("2", throwable.message, throwable.localizedMessage)
//                hideProgressDialog()
//                showErrorDialog()
            }
        }
    }

//    private fun showErrorDialog() {
//        errorDialog = AlertDialog.Builder(this).apply {
//            setTitle(R.string.error_title)
//            setMessage(getString(R.string.error_message))
//            setNeutralButton("OK") { dialog, _ ->
//                dialog.dismiss()
//                isErrorShowing = false
//            }
//        }.show()
//        isErrorShowing = true
//    }
//
//    private fun initDialogs() {
//        progressDialog = AlertDialog.Builder(this).apply {
//            setCancelable(false)
//            setView(layoutInflater.inflate(R.layout.loading_view, null))
//        }.create()
//
//        if (isProgressShowing) {
//            showProgressDialog()
//        }
//        if (isErrorShowing) {
//            showErrorDialog()
//        }
//    }
//
//
//    private fun showProgressDialog() {
//        progressDialog.show()
//        isProgressShowing = true
//    }
//
//    private fun hideProgressDialog() {
//        progressDialog.dismiss()
//        isProgressShowing = false
//    }


    private fun handlePaymentResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> globalResult.success("success")
            Activity.RESULT_CANCELED -> globalResult.success("cancelled")
            RESULT_ERROR -> globalResult.error("500", "handlePaymentResult", "")
        }
    }

    private fun handleGooglePayResult(resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK) {
            val token = GooglePayHelper.getGooglePayToken(data)
            if (token == null) {
                //showErrorDialog()
                //TODO resultadd
            } else {
                tinkoffAcquiring
                    .initPayment(token, currentPaymentOptions)
                    .subscribe(paymentListener)
                    .start()
            }
        } else if (resultCode != Activity.RESULT_CANCELED) {
            //showErrorDialog()
            globalResult.error("500", "handlePaymentResult", "")
        }
    }


    companion object {

        const val PAYMENT_REQUEST_CODE = 1
        const val DYNAMIC_QR_PAYMENT_REQUEST_CODE = 2
        const val GOOGLE_PAY_REQUEST_CODE = 5

        private const val STATE_PAYMENT_AMOUNT = "payment_amount"
        private const val STATE_LOADING_SHOW = "loading_show"
        private const val STATE_ERROR_SHOW = "error_show"


    }
}

