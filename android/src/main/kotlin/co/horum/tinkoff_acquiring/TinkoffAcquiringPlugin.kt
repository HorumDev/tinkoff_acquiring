package co.horum.tinkoff_acquiring

import android.app.Activity
import android.content.Context
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.annotation.NonNull
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.Wallet
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
import io.flutter.Log;
import org.json.JSONArray
import org.json.JSONObject
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.EXTRA_CARD_ID
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.EXTRA_ERROR
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess
import ru.tinkoff.acquiring.sdk.payment.PaymentState
import ru.tinkoff.acquiring.sdk.utils.GooglePayHelper

import ru.tinkoff.acquiring.sdk.models.GooglePayParams
import ru.tinkoff.acquiring.sdk.models.enums.CheckType
import java.math.BigDecimal
import kotlin.collections.HashMap

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
            PAYMENT_REQUEST_CODE, DYNAMIC_QR_PAYMENT_REQUEST_CODE -> handlePaymentResult(resultCode, data)
            GOOGLE_PAY_REQUEST_CODE -> handleGooglePayResult(resultCode, data)
            //else -> super.onActivityResult(requestCode, resultCode, data)
        }
        return true
    }


    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }

    // Входная точка всех запросов от flutter к нативному
    // Проверить текущую работу init sdk, pay by card
    // Добавить проверку оплачивать google.pay
    // разобраться с ответами resulta (библ acquiring)
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {

        android.util.Log.d("APP_TAG", "onMethodCall: result = $result")
        globalResult = result
        when (call.method) {
            "getPlatformVersion" ->
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "initSdk" -> {
                val params = call.arguments as Map<*, *>
                android.util.Log.d("TAG", "onMethodCall: params = $params")
                var env = WalletConstants.ENVIRONMENT_TEST
                android.util.Log.d("TAG", "onMethodCall: env = $env")
                if ((params["env"] as Int) == 1) {
                    env = WalletConstants.ENVIRONMENT_PRODUCTION
                } else {
                    AcquiringSdk.isDebug = true
                    AcquiringSdk.isDeveloperMode = true
                }



                terminalKey = params["terminalKey"] as String
                android.util.Log.d("APP_TAG", "onMethodCall: terminalKey = ${params["terminalKey"]}")
                googleParams = GooglePayParams(
                    params["terminalKey"] as String, //ключ терминала
                    false, //запрашивать адрес доставки у покупателя
                    false, //запрашивать телефон у покупателя
                    env //режим работы (test/prod)
                )
                android.util.Log.d("APP_TAG", "onMethodCall: googleParams = $googleParams")
                tinkoffAcquiring = TinkoffAcquiring(
                    terminalKey,
                    params["publicKey"] as String
                )
                android.util.Log.d("APP_TAG", "onMethodCall: tinkoffAcquiring = $tinkoffAcquiring")
                result.success(true)
            }
            "canMakePayments" -> {
                //onGooglePayReady - коллбек, оповещающий о доступности Google Pay на устройстве
                val googlePayHelper = GooglePayHelper(googleParams)
                googlePayHelper.initGooglePay(applicationContext) { onGooglePayReady ->
                    result.success(onGooglePayReady)
                }
            }
            "pay" -> {
                paymentListener = createPaymentListener(result)
                val params = call.arguments as Map<*, *>
                android.util.Log.d("APP_TAG", "onMethodCall: amount = ${Money.ofRubles(params["Amount"] as Double)}")
                currentPaymentOptions = PaymentOptions().setOptions {
                    orderOptions { // данные заказа
                        orderId = params["orderId"] as String
                        amount = Money.ofRubles(params["Amount"] as Double) /// Check
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
                        //onGooglePayReady - коллбек, оповещающий о доступности Google Pay на устройстве
                        val googlePayHelper = GooglePayHelper(googleParams)
                        googlePayHelper.initGooglePay(applicationContext) { ready ->
                            android.util.Log.d("APP_TAG", "onMethodCall: payMethod = 1, initGooglePay.ready = $ready")
                            if (ready) {
                                googlePayHelper.openGooglePay(
                                    activity,
                                    currentPaymentOptions.order.amount,
                                    GOOGLE_PAY_REQUEST_CODE
                                )
                                result.success(ready)
                                android.util.Log.d("APP_TAG", "onMethodCall: result = $result")
                                android.util.Log.d("APP_TAG", "googlePayHelper: $googlePayHelper")
//                  tinkoffAcquiring.initPayment(token, currentPaymentOptions)
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
                //      .subscribe(createPaymentListener()) // подписываемся на события в процессе оплаты
//                      .start()
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
                android.util.Log.d("APP_TAG", "onStatusChanged: state =  ")
                if (state == PaymentState.STARTED) {
                    //showProgressDialog()
                    //TODO notify about it
                }
            }

            override fun onSuccess(paymentId: Long, cardId: String?, rebillId: String?) {
                  android.util.Log.d("APP_TAG", "onSuccess: {\"paymentId\": $paymentId,\"cardId\": $cardId, \"rebillId\": $rebillId}")
                  result.success("{\"paymentId\": $paymentId,\"cardId\": $cardId, \"rebillId\": $rebillId}")

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
                android.util.Log.d("APP_TAG", "onError: ")
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


    private fun handlePaymentResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
            val paymentId = data?.extras?.getLong(TinkoffAcquiring.EXTRA_PAYMENT_ID, -1)
                val cardId = data?.extras?.getString(TinkoffAcquiring.EXTRA_CARD_ID, "null")

                android.util.Log.d(
                    "APP_TAG",
                    "success: EXTRA_PAYMENT_ID = ${paymentId}, EXTRA_CARD_ID = $cardId"
                )
                //globalResult.success("{\"paymentId\": $paymentId,\"cardId\": $cardId}")
                globalResult.success("success")
            }
            Activity.RESULT_CANCELED -> globalResult.success("cancelled")
            RESULT_ERROR -> {
                val error = data?.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR) as Throwable
                android.util.Log.d("APP_TAG", "EXTRA_ERROR: $error")
                globalResult.error(TinkoffAcquiring.EXTRA_ERROR, error.message,
                    error.localizedMessage
                )
            }
        }
    }

    private fun handleGooglePayResult(resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK) {
            val token = GooglePayHelper.getGooglePayToken(data)
            if (token == null) {
                //showErrorDialog()
            } else {
                val response = tinkoffAcquiring
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

