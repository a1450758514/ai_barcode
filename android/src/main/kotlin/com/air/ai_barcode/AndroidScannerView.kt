package com.air.ai_barcode

import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.*
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


/**
 * <p>
 * Created by air on 2019-12-02.
 * </p>
 */
class AndroidScannerView(
    binaryMessenger: BinaryMessenger,
    context: Context?,
    viewid: Int,
    args: Any?
) : PlatformView, MethodChannel.MethodCallHandler, EventChannel.StreamHandler, BarcodeCallback,
    DecoratedBarcodeView.TorchListener {

    enum Protos_BarcodeFormat {
        unknown = 0;
        aztec = 1;
        code39 = 2;
        code93 = 3;
        ean8 = 4;
        ean13 = 5;
        code128 = 6;
        dataMatrix = 7;
        qr = 8;
        interleaved2of5 = 9;
        upce = 10;
        pdf417 = 11;
        upca = 12;
        codebar= 13;
        rss_14= 14;
    }

    companion object {
        const val TOGGLE_FLASH = 200
        const val CANCEL = 300
        const val EXTRA_CONFIG = "config"
        const val EXTRA_RESULT = "scan_result"
        const val EXTRA_ERROR_CODE = "error_code"

        private val formatMap: Map<Protos_BarcodeFormat, BarcodeFormat> = mapOf(
                Protos_BarcodeFormat.aztec to BarcodeFormat.AZTEC,
                Protos_BarcodeFormat.code39 to BarcodeFormat.CODE_39,
                Protos_BarcodeFormat.code93 to BarcodeFormat.CODE_93,
                Protos_BarcodeFormat.code128 to BarcodeFormat.CODE_128,
                Protos_BarcodeFormat.dataMatrix to BarcodeFormat.DATA_MATRIX,
                Protos_BarcodeFormat.ean8 to BarcodeFormat.EAN_8,
                Protos_BarcodeFormat.ean13 to BarcodeFormat.EAN_13,
                Protos_BarcodeFormat.interleaved2of5 to BarcodeFormat.ITF,
                Protos_BarcodeFormat.pdf417 to BarcodeFormat.PDF_417,
                Protos_BarcodeFormat.qr to BarcodeFormat.QR_CODE,
                Protos_BarcodeFormat.upce to BarcodeFormat.UPC_E,
                Protos_BarcodeFormat.upca to BarcodeFormat.UPC_A
                Protos_BarcodeFormat.codebar to BarcodeFormat.CODABAR
                Protos_BarcodeFormat.rss_14 to BarcodeFormat.RSS_14
        )

    }

    /**
     * 用于向Flutter发送数据
     */
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        this.mEventChannelSink = events;
    }

    override fun onCancel(arguments: Any?) {
        this.mEventChannelSink?.endOfStream();
    }


    override fun barcodeResult(result: BarcodeResult?) {

        if (result == null) {
            return;
        }
        if (result.text == null || result.text == mLastText) {
            // Prevent duplicate scans
            return
        }

        mLastText = result.text

        val stringValue = result.text.toString();
        val format = (formatMap.filterValues { it == result.barcodeFormat }.keys.firstOrNull()
                    ?: Protos_BarcodeFormat.unknown)

        let str = "{\"rawContent\":$stringValue,\"format\":$format}";

        this.mEventChannelSink?.success(str.toString());
    }

    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
        super.possibleResultPoints(resultPoints)
    }

    /**
     * 接收Flutter传递过来的数据据
     */
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

        this.channelResult = result;
        when (call.method) {
            "startCamera" -> startCamera()
            "stopCamera" -> stopCamera()
            "resumeCameraPreview" -> resumeCameraPreview()
            "stopCameraPreview" -> stopCameraPreview()
            "openFlash" -> openFlash()
            "closeFlash" -> closeFlash()
            "toggleFlash" -> toggleFlash()
            else -> result.notImplemented()
        }
    }

    /**
     * 二维码扫描组件
     */
    var mContext: Context? = context;
    var mZXingBarcode: DecoratedBarcodeView = DecoratedBarcodeView(context);


    var mTextView: TextView = TextView(context);
    var mLastText: String = "";


    lateinit var channelResult: MethodChannel.Result;
    var mEventChannelSink: EventChannel.EventSink? = null;

    var mTorchOn: Boolean = false

    init {
        mTextView.text = "Scanner view";
        /*
        MethodChannel
         */
        val methodChannel: MethodChannel =
            MethodChannel(binaryMessenger, "view_type_id_scanner_view_method_channel");
        methodChannel.setMethodCallHandler(this);
        /*
        EventChannel
         */
        val eventChannel: EventChannel =
            EventChannel(binaryMessenger, "view_type_id_scanner_view_event_channel");
        eventChannel.setStreamHandler(this);
    }

    override fun getView(): View {

        val formats: Collection<BarcodeFormat> =
            listOf(
                BarcodeFormat.AZTEC,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_8,
                BarcodeFormat.EAN_13,
                BarcodeFormat.RSS_14,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.RSS_EXPANDED,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.PDF_417,
                BarcodeFormat.CODABAR,
            )
        mZXingBarcode.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        mZXingBarcode.setStatusText("")
        mZXingBarcode.decodeContinuous(this)
        mZXingBarcode.setTorchListener(this)


        return mZXingBarcode;
    }

    override fun dispose() {
    }

    override fun onFlutterViewDetached() {
    }

    override fun onFlutterViewAttached(flutterView: View) {
    }

    private fun startCamera() {
        mZXingBarcode.pauseAndWait();
    }

    private fun stopCamera() {
        mZXingBarcode.pause();
    }

    private fun resumeCameraPreview() {
        mZXingBarcode.resume()
    }

    private fun stopCameraPreview() {
        mZXingBarcode.pauseAndWait();
    }

    private fun openFlash() {
        mZXingBarcode.setTorchOn()
    }

    private fun closeFlash() {
        mZXingBarcode.setTorchOff()
    }

    private fun toggleFlash() {
        if (mTorchOn) {
            closeFlash()
        } else {
            openFlash()
        }
    }

    override fun onTorchOff() {
        mTorchOn = false
    }

    override fun onTorchOn() {
        mTorchOn = true
    }
}