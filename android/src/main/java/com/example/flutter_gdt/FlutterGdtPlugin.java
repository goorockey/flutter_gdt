package com.example.flutter_gdt;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Date;


import com.example.flutter_gdt.managers.NativeExpressManager;
import com.example.flutter_gdt.views.FlutterNativeExpressViewFactory;
import com.example.flutter_gdt.views.FlutterSplashAdViewFactory;
import com.example.flutter_gdt.views.FlutterBannerAdViewFactory;

import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.rewardvideo.RewardVideoAD;
import com.qq.e.ads.rewardvideo.RewardVideoADListener;
import com.qq.e.comm.util.AdError;

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
            return;
        } else if ("preloadNativeExpress".equals(call.method)) {
            this.preloadNativeExpress(call, result);
            return;
        } else if ("loadRewardVideoAd".equals(call.method)) {
            this.loadRewardVideoAd(call, result);
            return;
        }

        result.notImplemented();
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
        return;
        /*
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

        // if (mActivity != null) {
        //     NativeExpressManager.getInstance().preloadNativeExpressAd(mActivity, appId,
        //         posId, new ADSize((int) width, (int) height), preloadCount);
        // }
        */
    }

    private static final class VIDEO_RESULT_TYPE {
        static final int VIDEO_FAILED = 1;
        static final int VIDEO_ERROR = 2;
        static final int VIDEO_CLOSE = 3;
        static final int VIDEO_COMPLETE = 4;
        static final int VIDEO_REWARD_VERIFIED = 5;
    }

    private class RewardAdListener implements RewardVideoADListener {
        private Result mResult;
        private boolean mVideoComplete;
        private RewardVideoAD mRewardVideoAD;

        RewardAdListener(Result result) {
            mResult = result;
            mVideoComplete = false;
        }

        public void setRewardVideoAd(RewardVideoAD rewardVideoAD) {
            mRewardVideoAD = rewardVideoAD;
        }

        /**
        * 广告加载成功，可在此回调后进行广告展示
        **/
        @Override
        public void onADLoad() {
            System.out.println("GDT video load ad success");
            mRewardVideoAD.showAD();
        }

        /**
        * 视频素材缓存成功，可在此回调后进行广告展示
        */
        @Override
        public void onVideoCached() {
            LogUtils.i(Consts.TAG, "GDT video onVideoCached");
        }

        /**
        * 激励视频广告页面展示
        */
        @Override
        public void onADShow() {
            LogUtils.i(Consts.TAG, "GDT video onADShow");
        }

        /**
        * 激励视频广告曝光
        */
        @Override
        public void onADExpose() {
            LogUtils.i(Consts.TAG, "GDT video onADExpose");
        }

        /**
        * 激励视频触发激励（观看视频大于一定时长或者视频播放完毕）
        */
        @Override
        public void onReward() {
            LogUtils.i(Consts.TAG, "GDT video onReward");
        }

        /**
        * 激励视频广告被点击
        */
        @Override
        public void onADClick() {
            LogUtils.i(Consts.TAG, "GDT video onADClick");
        }

        /**
        * 激励视频播放完毕
        */
        @Override
        public void onVideoComplete() {
            LogUtils.i(Consts.TAG, "GDT video onVideoComplete");
            mVideoComplete = true;
        }

        /**
        * 激励视频广告被关闭
        */
        @Override
        public void onADClose() {
            LogUtils.i(Consts.TAG, "GDT video onADClose");
            mResult.success(mVideoComplete ? VIDEO_RESULT_TYPE.VIDEO_COMPLETE : VIDEO_RESULT_TYPE.VIDEO_CLOSE);
        }

        /**
        * 广告流程出错
        */
        @Override
        public void onError(AdError adError) {
            System.out.println(String.format("GDT video onError, error code: %d, error msg: %s",
                adError.getErrorCode(), adError.getErrorMsg()));
            mResult.success(VIDEO_RESULT_TYPE.VIDEO_ERROR);
        }

    }


    private void loadRewardVideoAd(final MethodCall call, final Result result) {
        try {
            String appId = call.argument("appId");
            String positionId = call.argument("positionId");

            RewardAdListener rewardAdListener = new RewardAdListener(result);
            final RewardVideoAD rewardVideoAD = new RewardVideoAD(mActivity, appId, positionId, rewardAdListener);
            rewardAdListener.setRewardVideoAd(rewardVideoAD);

            rewardVideoAD.loadAD();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result.success(VIDEO_RESULT_TYPE.VIDEO_FAILED);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
