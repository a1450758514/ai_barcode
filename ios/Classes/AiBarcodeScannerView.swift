//
//  AiBarcodeScannerView.swift
//  ai_barcode
//
//  Created by JamesAir on 2020/1/19.
//

import Foundation
import Flutter
import MTBBarcodeScanner


class AiBarcodeScannerView:NSObject,FlutterPlatformView{

    enum BarcodeFormat {
        typealias RawValue = Int
        case unknown // = 0
        case aztec // = 1
        case code39 // = 2
        case code93 // = 3
        case ean8 // = 4
        case ean13 // = 5
        case code128 // = 6
        case dataMatrix // = 7
        case qr // = 8
        case interleaved2Of5 // = 9
        case upce // = 10
        case pdf417 // = 11
        case UNRECOGNIZED(Int)

        init() {
            self = .unknown
        }

        init?(rawValue: Int) {
            switch rawValue {
            case 0: self = .unknown
            case 1: self = .aztec
            case 2: self = .code39
            case 3: self = .code93
            case 4: self = .ean8
            case 5: self = .ean13
            case 6: self = .code128
            case 7: self = .dataMatrix
            case 8: self = .qr
            case 9: self = .interleaved2Of5
            case 10: self = .upce
            case 11: self = .pdf417
            default: self = .UNRECOGNIZED(rawValue)
            }
        }

        var rawValue: Int {
            switch self {
            case .unknown: return 0
            case .aztec: return 1
            case .code39: return 2
            case .code93: return 3
            case .ean8: return 4
            case .ean13: return 5
            case .code128: return 6
            case .dataMatrix: return 7
            case .qr: return 8
            case .interleaved2Of5: return 9
            case .upce: return 10
            case .pdf417: return 11
            case .UNRECOGNIZED(let i): return i
            }
        }

    }
    
    private let formatMap = [
        BarcodeFormat.aztec : AVMetadataObject.ObjectType.aztec,
        BarcodeFormat.code39 : AVMetadataObject.ObjectType.code39,
        BarcodeFormat.code93 : AVMetadataObject.ObjectType.code93,
        BarcodeFormat.code128 : AVMetadataObject.ObjectType.code128,
        BarcodeFormat.dataMatrix : AVMetadataObject.ObjectType.dataMatrix,
        BarcodeFormat.ean8 : AVMetadataObject.ObjectType.ean8,
        BarcodeFormat.ean13 : AVMetadataObject.ObjectType.ean13,
        BarcodeFormat.interleaved2Of5 : AVMetadataObject.ObjectType.interleaved2of5,
        BarcodeFormat.pdf417 : AVMetadataObject.ObjectType.pdf417,
        BarcodeFormat.qr : AVMetadataObject.ObjectType.qr,
        BarcodeFormat.upce : AVMetadataObject.ObjectType.upce,
    ]

    var scannerView: UIView!
    var scanner:MTBBarcodeScanner!
    var methodChannel:FlutterMethodChannel?;
    var flutterResult:FlutterResult?;
    var binaryMessenger:FlutterBinaryMessenger!;
    /*
     Constructor.
     */
    init(binaryMessenger: FlutterBinaryMessenger) {
        //Call parent init constructor.
        super.init();
        self.binaryMessenger = binaryMessenger;
        /*
         Method Channel
         */
        initMethodChannel();
        /*
         Scanner
         */
        scannerView = UIView();
        //        scannerView.frame(forAlignmentRect: CGRect.init(x: 0, y: 0, width: 100, height: 150))
        scanner = MTBBarcodeScanner(previewView: scannerView);
    }
    
    
    
    func view() -> UIView {
        
        return scannerView;
    }
    
    func initMethodChannel(){
        /*
         MethodChannel.
         */
        methodChannel = FlutterMethodChannel.init(name: "view_type_id_scanner_view_method_channel", binaryMessenger: binaryMessenger)
        methodChannel?.setMethodCallHandler { (call :FlutterMethodCall, result:@escaping FlutterResult)  in
            /*
             Save flutter result.
             */
            self.flutterResult = result;
            
            switch(call.method){
            case "startCamera":
                /*
                 打开相机
                 */
                self.startCamera();
                break;
            case "stopCamera":
                /*
                 关闭相机
                 */
                self.stopCamera();
                break;
                /*
                 预览相机
                 */
            case "resumeCameraPreview":
                self.resumeCameraPreview();
                break;
                /*
                 停止预览
                 */
            case "stopCameraPreview":
                self.stopCameraPreview();
                break;
                /*
                 打开手电筒
                 */
            case "openFlash":
                self.openFlash();
                break;
                /*
                 关闭手电筒
                 */
            case "closeFlash":
                self.closeFlash();
                break;
                /*
                 切换手电筒
                 */
            case "toggleFlash":
                self.toggleFlash();
                break;
            default:
                self.flutterResult?("method:\(call.method) not implement");
            }
        }
    }
    
    
    
    func startCamera(){
        
    }
    func stopCamera(){
        //        self.scanner?.stopScanning()
    }
    func resumeCameraPreview(){
        if(self.scanner.isScanning()){
            return;
        }
        MTBBarcodeScanner.requestCameraPermission(success: { success in
            if success {
                do {
                    try self.scanner.startScanning(resultBlock: { codes in
                        if let codes = codes {
                            for code in codes {
                                let stringValue = code.stringValue!
                                let codeType = self.formatMap.first(where: { $0.value == code.type });
                                let format = codeType?.key ?? .unknown
                                let str = "{\"rawContent\":\(stringValue),\"format\":\(format)}";
                                if(self.flutterResult != nil){
                                    self.flutterResult?("\(str)");
                                }
                                
                                print("Found code: \(stringValue)")
                            }
                        }
                    })
                } catch {
                    NSLog("Unable to start scanning error:\(error)")
                    self.flutterResult?("Unable to start scanning error:\(error)");
                }
            } else {
                self.flutterResult?("Unable to start scanning This app does not have permission to access the camera");
            }
        })
    }
    
    func stopCameraPreview(){
        if(self.scanner.isScanning()){
            self.scanner.stopScanning()
            
        }
        
    }
    func openFlash(){
        do{
            try scanner?.setTorchMode(MTBTorchMode.on, error: ())
            
            self.flutterResult?(true);
        }catch{
            self.flutterResult?(false);
        }
    }
    func closeFlash(){
        do{
            try scanner?.setTorchMode(MTBTorchMode.off, error: ())
            self.flutterResult?(true);
        }catch{
            self.flutterResult?(false);
        }
    }
    func toggleFlash(){
        scanner?.toggleTorch();
        self.flutterResult?(true);
    }
}
