package com.example.flutter_gdt.views;

import android.view.ViewGroup;
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
import com.qq.e.ads.banner2.UnifiedBannerADListener;
import com.qq.e.ads.banner2.UnifiedBannerView;
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

public class FlutterBannerAdView implements PlatformView, MethodChannel.MethodCallHandler {
    private LinearLayout mLinearLayout;
    private Activity mActivity;
    private MethodChannel methodChannel;
    private UnifiedBannerView mBannerView;

    FlutterBannerAdView(Activity activity, BinaryMessenger messenger, int id) {
        methodChannel = new MethodChannel(messenger, "flutter_gdt_banner_ad_view_" + id);
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

        if (mBannerView != null) {
            mBannerView.destroy();
            mBannerView = null;
        }
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        if (Consts.FunctionName.RENDER_BANNER_AD.equals(methodCall.method)) {
            renderBannerAd(methodCall, result);
        }
    }

    private void renderBannerAd(MethodCall call, final MethodChannel.Result result) {
        try {
            String appId = (String) call.argument(Consts.ParamKey.APP_ID);
            String positionId = (String) call.argument(Consts.ParamKey.POSITION_ID);

            if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(positionId)) {
                Log.i(Consts.TAG, "Adnet banner empty appId or positionId");
                return;
            }
            if (mBannerView != null) {
                mBannerView.destroy();
                mBannerView = null;
            }

            mBannerView = new UnifiedBannerView(mActivity, appId, positionId, new UnifiedBannerADListener() {
                @Override
                public void onADReceive() {
                    Log.i(Consts.TAG, "Adnet banner ad received");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adLoaded", null);
                        }
                    });
                }

                @Override
                public void onNoAD(AdError adError) {
                    Log.i(Consts.TAG, "Adnet banner ad noad");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adError", null);
                        }
                    });
                }

                @Override
                public void onADExposure() {
                    Log.i(Consts.TAG, "Adnet banner ad exposured");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adExposure", null);
                        }
                    });
                }

                @Override
                public void onADClicked() {
                    Log.i(Consts.TAG, "Adnet banner ad clicked");

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("adClicked", null);
                        }
                    });
                }

                @Override
                public void onADClosed() {
                    Log.i(Consts.TAG, "Adnet banner ad closed");
                }

                @Override
                public void onADLeftApplication() {
                    Log.i(Consts.TAG, "Adnet banner ad left app");
                }

                @Override
                public void onADOpenOverlay() {
                    Log.i(Consts.TAG, "Adnet banner ad open overlay");
                }

                @Override
                public void onADCloseOverlay() {
                    Log.i(Consts.TAG, "Adnet banner ad close overlay");
                }
            });

            mLinearLayout.removeAllViews();
            mLinearLayout.addView(mBannerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            mBannerView.loadAD();

            try {
                result.success(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();

            try {
                result.success(false);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
