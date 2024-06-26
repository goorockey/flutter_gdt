package com.example.flutter_gdt.views;

import android.util.Log;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.os.Build;

import com.example.flutter_gdt.Consts;
import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADListener;
import com.qq.e.comm.util.AdError;

import java.lang.reflect.Field;
import java.util.HashMap;

import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.BinaryMessenger;

/**
 * @author luopeng Created at 2019-08-19 23:23
 */
public class FlutterSplashAdView implements PlatformView, MethodChannel.MethodCallHandler {
    private LinearLayout mLinearLayout;
    private Activity mActivity;
    private String mAppId;
    private String mPositionId;
    private MethodChannel methodChannel;

    FlutterSplashAdView(Activity activity, BinaryMessenger messenger, int id) {
        methodChannel = new MethodChannel(messenger, "flutter_gdt_splash_ad_view_" + id);
        methodChannel.setMethodCallHandler(this);
        this.mActivity = activity;
        if (mLinearLayout == null) {
            mLinearLayout = new LinearLayout(activity);
        }
    }

    @Override
    public View getView() {
        if (mActivity != null && mLinearLayout == null) {
            mLinearLayout = new LinearLayout(mActivity);
        }

        // 为了让platformView的背景透明
        if (mLinearLayout != null) {
            mLinearLayout.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        ViewParent parent = mLinearLayout.getParent();
                        if (parent == null) {
                            return;
                        }
                        while (parent.getParent() != null) {
                            parent = parent.getParent();
                        }
                        Object decorView = parent.getClass().getDeclaredMethod("getView").invoke(parent);
                        final Field windowField = decorView.getClass().getDeclaredField("mWindow");
                        windowField.setAccessible(true);
                        final Window window = (Window) windowField.get(decorView);
                        windowField.setAccessible(false);
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE);
                            window.setLocalFocus(true, true);
                        }
                    } catch (Exception e) {
                        // log the exception
                    }
                }
            });
        }
        return mLinearLayout;
    }

    @Override
    public void dispose() {
        methodChannel.setMethodCallHandler(null);
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        if (Consts.FunctionName.RENDER_SPLASH_AD.equals(methodCall.method)) {
            renderSplashAd(methodCall, result);
        }
    }

    private void renderSplashAd(MethodCall call, final MethodChannel.Result result) {
        try {
            String appId = (String) call.argument(Consts.ParamKey.APP_ID);
            String positionId = (String) call.argument(Consts.ParamKey.POSITION_ID);
            if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(positionId)) {
                return;
            }

            int timeout = 3000;
            Object timeoutVal = call.argument(Consts.ParamKey.TIMEOUT);
            if (timeoutVal != null) {
                timeout = (int) timeoutVal;
            }

            SplashAD splashAD = new SplashAD(mActivity, mLinearLayout, appId, positionId, new SplashADListener() {
                @Override
                public void onADDismissed() {
                    Log.i(Consts.TAG, "Adnet splash ad dismissed");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adDismissed", null);
                        }
                    });
                }

                @Override
                public void onNoAD(AdError adError) {
                    Log.i(Consts.TAG, "Adnet splash ad noad");
                    try {
                        result.success(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onADPresent() {
                    Log.i(Consts.TAG, "Adnet splash ad present");
                }

                @Override
                public void onADClicked() {
                    Log.i(Consts.TAG, "Adnet splash ad clicked");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adClicked", null);
                        }
                    });
                }

                @Override
                public void onADTick(final long l) {
                    Log.i(Consts.TAG, "Adnet splash ad tick");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adTick", l);
                        }
                    });
                }

                @Override
                public void onADExposure() {
                    Log.i(Consts.TAG, "Adnet splash ad showed");
                    try {
                        result.success(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, timeout);
            splashAD.fetchAndShowIn(mLinearLayout);

        } catch (Exception e) {
            try {
                result.success(false);
            } catch (Exception e1) {
                e.printStackTrace();
            }
        }
    }
}
