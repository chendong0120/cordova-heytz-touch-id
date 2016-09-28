package com.heytz.touchid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Log;
import android.widget.Toast;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by chendongdong on 16/9/19.
 */

public class HeytzTouchid extends CordovaPlugin {
    private Handler handler;
    private Context context;
    private static final String IS_AVAILABLE = "isAvailable";
    private static final String VERIFY_FINGER_PRINT = "verifyFingerprint";
    private static final String VERIFY_FINGER_WCPF = "verifyFingerprintWithCustomPasswordFallback";
    private static final String VERIFY_FINGER_WCPFAEPL = "verifyFingerprintWithCustomPasswordFallbackAndEnterPasswordLabel";
    private final static String TAG = "HeytzTouchid:";
    private FingerprintManagerCompat manager;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getActivity().getApplicationContext();
        manager = FingerprintManagerCompat.from(context);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals(IS_AVAILABLE)) {
            isAvailable(callbackContext);
            return true;
        }
        if (action.equals(VERIFY_FINGER_PRINT)) {
            /**
             * 开始验证，什么时候停止由系统来确定，如果验证成功，那么系统会关系sensor，如果失败，则允许
             * 多次尝试，如果依旧失败，则会拒绝一段时间，然后关闭sensor，过一段时候之后再重新允许尝试
             *
             * 第四个参数为重点，需要传入一个FingerprintManagerCompat.AuthenticationCallback的子类
             * 并重写一些方法，不同的情况回调不同的函数
             */
            manager.authenticate(null, 0, null, new MyCallBack(), null);
            return true;
        }
        if (action.equals(VERIFY_FINGER_WCPF)) {
            return true;
        }
        if (action.equals(VERIFY_FINGER_WCPFAEPL)) {
            return true;
        }
        return false;
    }


    public void isAvailable(CallbackContext callbackContext) {
        String errorMessage = null;
        int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            errorMessage = isFinger();
        } else {
            errorMessage = "Android版本太低!";
        }
        if (errorMessage == null) {
            callbackContext.success();
        } else {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            callbackContext.error(errorMessage);
        }
    }

    public String isFinger() {
        String str = null;
        //android studio 上，没有这个会报错
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            str = "没有指纹识别权限";
        }
        Log(TAG, "有指纹权限");
        //判断硬件是否支持指纹识别
        if (!manager.isHardwareDetected()) {
            str = "没有指纹识别模块";
        }
        Log(TAG, "有指纹模块");
        //判断是否有指纹录入
        if (!manager.hasEnrolledFingerprints()) {
            str = "没有录入指纹";
        }
        return str;
    }

    public class MyCallBack extends FingerprintManagerCompat.AuthenticationCallback {
        private static final String TAG = "MyCallBack";

        // 当出现错误的时候回调此函数，比如多次尝试都失败了的时候，errString是错误信息
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Log.d(TAG, "onAuthenticationError: " + errString);
        }

        // 当指纹验证失败的时候会回调此函数，失败之后允许多次尝试，失败次数过多会停止响应一段时间然后再停止sensor的工作
        @Override
        public void onAuthenticationFailed() {
            Log.d(TAG, "onAuthenticationFailed: " + "验证失败");
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Log.d(TAG, "onAuthenticationHelp: " + helpString);
        }

        // 当验证的指纹成功时会回调此函数，然后不再监听指纹sensor
        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult
                                                      result) {
            Log.d(TAG, "onAuthenticationSucceeded: " + "验证成功");
        }
    }

    private void Log(String tag, String msg) {
        Log.d(tag, msg);
    }
}
