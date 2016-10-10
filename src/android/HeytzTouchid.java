package com.heytz.touchid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by chendongdong on 16/9/19.
 */

public class HeytzTouchid extends CordovaPlugin {
    private Context context;
    private static final String IS_AVAILABLE = "isAvailable";
    private static final String VERIFY_FINGER_PRINT = "verifyFingerprint";
    private static final String CANCEL_VERIFY_FINGER_PRINT = "cancelVerifyFingerprint";
    private static final String VERIFY_FINGER_WCPF = "verifyFingerprintWithCustomPasswordFallback";
    private static final String VERIFY_FINGER_WCPFAEPL = "verifyFingerprintWithCustomPasswordFallbackAndEnterPasswordLabel";
    private final static String TAG = "HeytzTouchid:";
    private final static boolean DEBUG = true;
    private FingerprintManagerCompat manager;
    private CallbackContext _callbackContext;
    private PackageManager packageManager;
    private TOUCHSDK _currentTouchSdk;
    protected final static String[] permissions = {Manifest.permission.USE_FINGERPRINT};
    private CancellationSignal cancellationSignal = null;
    ;

    private enum TOUCHSDK {
        NOT_SUPPORTED, ANDROID, SAMSUNG
    }


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getActivity().getApplicationContext();
        manager = FingerprintManagerCompat.from(context);
        packageManager = cordova.getActivity().getPackageManager();

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals(IS_AVAILABLE)) {
            _currentTouchSdk = getTocuhSDK();
            isAvailable(callbackContext);
            return true;
        }
        if (action.equals(VERIFY_FINGER_PRINT)) {
            _callbackContext = callbackContext;
            /**
             * 开始验证，什么时候停止由系统来确定，如果验证成功，那么系统会关系sensor，如果失败，则允许
             * 多次尝试，如果依旧失败，则会拒绝一段时间，然后关闭sensor，过一段时候之后再重新允许尝试
             *
             * 第四个参数为重点，需要传入一个FingerprintManagerCompat.AuthenticationCallback的子类
             * 并重写一些方法，不同的情况回调不同的函数
             */
            cancellationSignal = new CancellationSignal();
            manager.authenticate(null, 0, cancellationSignal, new HeytzAuthenticationCallback(), null);
            return true;
        }
        if (action.equals(CANCEL_VERIFY_FINGER_PRINT)) {
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
                callbackContext.success();
            } else {
                callbackContext.error("没有开启验证指纹验证!");
            }
            return true;
        }
        if (action.equals(VERIFY_FINGER_WCPF)) {
            callbackContext.error("暂未开放");
            return true;
        }
        if (action.equals(VERIFY_FINGER_WCPFAEPL)) {
            callbackContext.error("暂未开放");
            return true;
        }
        return false;
    }

    /**
     * 判断是否有指纹解锁的功能
     *
     * @return
     */
    private TOUCHSDK getTocuhSDK() {
        ArrayList<String> libraries = new ArrayList<String>();
        for (String i : packageManager.getSystemSharedLibraryNames()) {
            libraries.add(i);
        }
//        if (android.os.Build.VERSION.SDK_INT >= 17) {
//            if (libraries.contains("com.samsung.android.sdk.pass")) {
//                return TOUCHSDK.SAMSUNG;
//            }
//        }

        // android 6.0 (API 23)
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return TOUCHSDK.ANDROID;
        }
        return TOUCHSDK.NOT_SUPPORTED;

    }

    /**
     * 验证是否有指纹解锁的权限
     *
     * @param callbackContext
     */
    public void isAvailable(CallbackContext callbackContext) throws JSONException {
        String errorMessage = null;
        JSONObject dataJson = new JSONObject();
        int version = Build.VERSION.SDK_INT;
        dataJson.put("SDK_Int", version);
        switch (_currentTouchSdk) {
            case NOT_SUPPORTED:
                errorMessage = "不支持";
                dataJson.put("errorMessage", "不支持");
                break;
            case SAMSUNG:
                dataJson.put("phoneType", "samsung");
                break;
            case ANDROID:
                dataJson.put("phoneType", "android");
                break;
        }
        //验证是否有权限
        if (errorMessage == null) {
            errorMessage = isFinger();
        }
        if (errorMessage == null) {
            callbackContext.success(dataJson);
        } else {
            dataJson.put("errorMessage", errorMessage);
            callbackContext.error(errorMessage);

        }
    }

    /**
     * 指纹权限验证和指纹是否录入
     *
     * @return
     */
    public String isFinger() {
        //android studio 上，没有这个会报错
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return "没有指纹识别权限";
        } else {
            Log(TAG, "有指纹权限");
            //判断硬件是否支持指纹识别
            if (!manager.isHardwareDetected()) {
                return "没有指纹识别模块";
            }
            Log(TAG, "有指纹模块");
            //判断是否有指纹录入
            if (!manager.hasEnrolledFingerprints()) {
                return "没有录入指纹";
            }
        }
        return null;
    }

    /**
     * android AuthenticationCallback 回调
     */
    public class HeytzAuthenticationCallback extends FingerprintManagerCompat.AuthenticationCallback {
        private static final String TAG = "HeytzAuthenticationCallback";

        // 当出现错误的时候回调此函数，比如多次尝试都失败了的时候，errString是错误信息
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Log(TAG, "onAuthenticationError: " + errString);
            JSONObject dataJson = new JSONObject();
            try {
                dataJson.put("errMsgId", errMsgId);
                dataJson.put("CharSequence", errString.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AuthenticationPluginCallback(dataJson);
        }

        // 当指纹验证失败的时候会回调此函数，失败之后允许多次尝试，失败次数过多会停止响应一段时间然后再停止sensor的工作
        @Override
        public void onAuthenticationFailed() {
            JSONObject dataJson = new JSONObject();
            Log(TAG, "onAuthenticationFailed: " + "验证失败");
            try {
                dataJson.put("type", "onAuthenticationFailed");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AuthenticationPluginCallback(dataJson);
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Log(TAG, "onAuthenticationHelp: " + helpString);
            JSONObject dataJson = new JSONObject();
            try {
                dataJson.put("type", "onAuthenticationHelp");
                dataJson.put("helpMsgId", helpMsgId);
                dataJson.put("helpString", helpString.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AuthenticationPluginCallback(dataJson);

        }

        // 当验证的指纹成功时会回调此函数，然后不再监听指纹sensor
        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult
                                                      result) {
            Log(TAG, "onAuthenticationSucceeded: " + "验证成功");
            JSONObject dataJson = new JSONObject();
            try {
                dataJson.put("type", "onAuthenticationSucceeded");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AuthenticationPluginCallback(dataJson);
        }

        /**
         * 回调
         *
         * @param jsonObject
         */
        private void AuthenticationPluginCallback(JSONObject jsonObject) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
            pluginResult.setKeepCallback(true);
            _callbackContext.sendPluginResult(pluginResult);
        }
    }

    /**
     * 打印日志
     *
     * @param tag
     * @param msg
     */
    private void Log(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }
}
