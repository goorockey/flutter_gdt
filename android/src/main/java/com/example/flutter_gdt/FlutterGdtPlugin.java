package com.example.flutter_gdt;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import com.example.flutter_gdt.managers.NativeExpressManager;
import com.example.flutter_gdt.views.FlutterNativeExpressViewFactory;
import com.example.flutter_gdt.views.FlutterSplashAdViewFactory;
import com.example.flutter_gdt.views.FlutterBannerAdViewFactory;
import com.qq.e.ads.nativ.ADSize;

import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;

/** FlutterGdtPlugin */
public class FlutterGdtPlugin implements MethodCallHandler {
    private Activity mActivity;

    public FlutterGdtPlugin(Activity activity) {
        mActivity = activity;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_gdt");
        channel.setMethodCallHandler(new FlutterGdtPlugin(registrar.activity()));

        registrar.platformViewRegistry().registerViewFactory("flutter_gdt_native_express_ad_view",
                new FlutterNativeExpressViewFactory(new StandardMessageCodec(), registrar.activity(),
                        registrar.messenger()));
        registrar.platformViewRegistry().registerViewFactory("flutter_gdt_splash_ad_view",
                new FlutterSplashAdViewFactory(new StandardMessageCodec(), registrar.activity(),
                        registrar.messenger()));
        registrar.platformViewRegistry().registerViewFactory("flutter_gdt_banner_ad_view",
                new FlutterBannerAdViewFactory(new StandardMessageCodec(), registrar.activity(),
                        registrar.messenger()));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if ("checkPermissions".equals(call.method)) {
            this.checkPermission(call, result);
        } else if ("preloadNativeExpress".equals(call.method)) {
            this.preloadNativeExpress(call, result);
        }
    }

    private void checkPermission(MethodCall call, Result result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.checkAndRequestPermission(result);
        } else {
            try {
                result.success(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> getNeedPermissionList() {
        ArrayList<String> lackedPermission = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((mActivity
                    .checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)) {
                lackedPermission.add(Manifest.permission.READ_PHONE_STATE);
            }

            if ((mActivity.checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            if ((mActivity.checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                lackedPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        return lackedPermission;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermission(Result result) {
        List<String> lackedPermission = getNeedPermissionList();

        // 权限都已经有了，那么直接调用SDK
        if (lackedPermission.size() == 0) {
            try {
                result.success(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 请求所缺少的权限，在onRequestPermissionsResult中再看是否获得权限，如果获得权限就可以调用SDK，否则不要调用SDK。
            try {
                result.success(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String[] requestPermissions = new String[lackedPermission.size()];
            lackedPermission.toArray(requestPermissions);
            mActivity.requestPermissions(requestPermissions, 1024);
        }
    }

    private void preloadNativeExpress(MethodCall call, Result result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lackedPermission = getNeedPermissionList();
            if (lackedPermission.size() > 0) {
                LogUtils.e(Consts.TAG, "no permission");
                return;
            }
        }

        String appId = call.argument("appId");
        String posId = call.argument("positionId");
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

        int preloadCount = Consts.DEFAULT_PRELOAD_COUNT;
        Object preload = call.argument("preloadCount");
        if (preload != null) {
            preloadCount = (int) preload;
        }

        // NativeExpressManager.getInstance().preloadNativeExpressAd(mActivity, appId,
        // posId, new ADSize((int) width, (int) height), preloadCount);
    }
}
