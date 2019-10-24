package com.example.flutter_gdt.views;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.os.Build;

import com.example.flutter_gdt.Consts;
import com.example.flutter_gdt.LogUtils;
import com.example.flutter_gdt.managers.NativeExpressManager;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;

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
 * @author luopeng Created at 2019/6/19 18:03
 */
public class FlutterNativeExpressView implements PlatformView, MethodChannel.MethodCallHandler {
  private HashMap<String, NativeExpressAD> mNativeExpressAdMap;
  private NativeExpressADView mNativeExpressAdView;
  private LinearLayout mLinearLayout;
  private Activity mActivity;
  private MethodChannel methodChannel;
  private String mChannelId;

  FlutterNativeExpressView(Activity activity, BinaryMessenger messenger, int id) {
    mChannelId = "flutter_gdt_native_express_ad_view_" + id;
    methodChannel = new MethodChannel(messenger, mChannelId);
    methodChannel.setMethodCallHandler(this);
    this.mActivity = activity;
    if (mLinearLayout == null) {
      mLinearLayout = new LinearLayout(activity);
    }
    if (mNativeExpressAdMap == null) {
      mNativeExpressAdMap = new HashMap<>();
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

    if (mNativeExpressAdView != null) {
      mNativeExpressAdView.destroy();
      mNativeExpressAdView = null;
    }
  }

  @Override
  public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
    if ("showNativeExpressAd".equals(methodCall.method)) {
      showNativeExpressAd(methodCall, result);
    }
  }

  private void showNativeExpressAd(final MethodCall call, final MethodChannel.Result result) {
    String appId = call.argument("appId");
    String positionId = call.argument("positionId");
    Object width = call.argument("width");
    if (width == null) {
      LogUtils.e(Consts.TAG, "no ad width");
      return;
    }

    Object height = call.argument("height");
    if (height == null) {
      LogUtils.e(Consts.TAG, "no ad height");
      return;
    }

    int expressWidth = (int) width;
    int expressHeight = (int) height;

    int preloadCount = Consts.DEFAULT_PRELOAD_COUNT;
    Object preload = call.argument("preloadCount");
    if (preload != null) {
      preloadCount = (int) preload;
    }

    ViewGroup.LayoutParams layoutParams = mLinearLayout.getLayoutParams();
    if (expressWidth > 0) {
      layoutParams.width = Consts.dp2px(mActivity, expressWidth);
    }
    if (expressHeight > 0) {
      layoutParams.height = Consts.dp2px(mActivity, expressHeight);
    }
    mLinearLayout.setLayoutParams(layoutParams);

    NativeExpressManager.getInstance().getNativeExpressView(mActivity, appId, positionId,
        new ADSize(expressWidth, expressHeight), preloadCount, result, methodChannel, mChannelId,
        new NativeExpressManager.NativeExpressViewGetCallback() {
          @Override
          public void viewGet(NativeExpressADView view) {
            if (mNativeExpressAdView != null) {
              mNativeExpressAdView.destroy();
            }
            mNativeExpressAdView = view;

            mLinearLayout.removeAllViews();
            mLinearLayout.addView(view,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            view.render();

            try {
              result.success(true);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          @Override
          public void viewGetError(String reason) {
            LogUtils.e(Consts.TAG, "error, reason: " + reason);

            try {
              result.success(false);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
  }
}
