package com.example.flutter_gdt.managers;

import android.app.Activity;
import android.util.Log;

import com.example.flutter_gdt.Consts;
import com.example.flutter_gdt.LogUtils;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.comm.util.AdError;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author luopeng Created at 2019/7/9 10:45
 */
public class NativeExpressManager {
  public interface NativeExpressViewGetCallback {
    void viewGet(NativeExpressADView view);

    void viewGetError(int code, String reason);
  }

  private volatile static NativeExpressManager mInstance;

  public static NativeExpressManager getInstance() {
    if (mInstance == null) {
      synchronized (NativeExpressManager.class) {
        if (mInstance == null) {
          mInstance = new NativeExpressManager();
        }
      }
    }
    return mInstance;
  }

  public NativeExpressManager() {
    mNativeExpressAdViewCache = new HashMap<>();
    mNativeExpressAdCache = new HashMap<String, NativeExpressAD>();
  }

  private HashMap<String, NativeExpressAD> mNativeExpressAdCache;
  private HashMap<String, ConcurrentLinkedQueue<NativeExpressADView>> mNativeExpressAdViewCache;
  private MethodChannel.Result mTmpResult;
  private MethodChannel mMethodChannel;

  public void preloadNativeExpressAd(Activity activity, String appId, final String positionId, ADSize adSize,
      int preloadCount) {
    if (mNativeExpressAdCache.get(positionId) == null) {
      mNativeExpressAdCache.put(positionId, new NativeExpressAD(activity, adSize, appId, positionId, new AdListener() {
        @Override
        public void onADLoaded(List<NativeExpressADView> list) {
          super.onADLoaded(list);

          putAdViewCache(positionId, list);
        }

        @Override
        public void onRenderSuccess(NativeExpressADView nativeExpressADView) {
          super.onRenderSuccess(nativeExpressADView);

          if (mTmpResult != null) {
            mTmpResult.success(true);
            mTmpResult = null;
          }
        }

        @Override
        public void onRenderFail(NativeExpressADView adView) {
          super.onRenderFail(adView);

          if (mTmpResult != null) {
            mTmpResult.success(false);
            mTmpResult = null;
          }
        }

        @Override
        public void onNoAD(AdError adError) {
          super.onNoAD(adError);

          if (mTmpResult != null) {
            mTmpResult.success(false);
            mTmpResult = null;
          }
        }

        @Override
        public void onADClicked(NativeExpressADView adView) {
          super.onADClicked(adView);

          if (mMethodChannel != null) {
            mMethodChannel.invokeMethod("adClicked", null);
            mMethodChannel = null;
          }
        }
      }));
    }

    NativeExpressAD nativeExpressAD = mNativeExpressAdCache.get(positionId);
    nativeExpressAD.loadAD(preloadCount);
  }

  public void getNativeExpressView(Activity activity, String appId, final String positionId, ADSize adSize,
      int preloadCount, final MethodChannel.Result result, final MethodChannel methodChannel,
      final NativeExpressViewGetCallback callback) {
    mTmpResult = result;
    mMethodChannel = methodChannel;

    ConcurrentLinkedQueue<NativeExpressADView> adViews = getViewQueue(positionId);
    if (adViews != null && adViews.size() > 0) {
      if (callback != null) {
        callback.viewGet(getAdView(positionId));
      }

      if (adViews.isEmpty() && preloadCount > 0) {
        preloadNativeExpressAd(activity, appId, positionId, adSize, preloadCount);
      }
      return;
    }

    new NativeExpressAD(activity, adSize, appId, positionId, new AdListener() {
      @Override
      public void onADLoaded(List<NativeExpressADView> list) {
        super.onADLoaded(list);

        if (list == null || list.isEmpty()) {
          return;
        }
        if (callback == null) {
          return;
        }
        callback.viewGet(list.get(0));
      }

      @Override
      public void onRenderSuccess(NativeExpressADView nativeExpressADView) {
        super.onRenderSuccess(nativeExpressADView);

        if (mTmpResult != null) {
          mTmpResult.success(true);
          mTmpResult = null;
        }
      }

      @Override
      public void onRenderFail(NativeExpressADView adView) {
        super.onRenderFail(adView);

        if (mTmpResult != null) {
          mTmpResult.success(false);
          mTmpResult = null;
        }
      }

      @Override
      public void onNoAD(AdError adError) {
        super.onNoAD(adError);

        if (mTmpResult != null) {
          mTmpResult.success(false);
          mTmpResult = null;
        }
      }

      @Override
      public void onADClicked(NativeExpressADView adView) {
        super.onADClicked(adView);

        if (mMethodChannel != null) {
          mMethodChannel.invokeMethod("adClicked", null);
          mMethodChannel = null;
        }
      }
    }).loadAD(preloadCount);

    preloadNativeExpressAd(activity, appId, positionId, adSize, preloadCount);
  }

  private synchronized ConcurrentLinkedQueue<NativeExpressADView> getViewQueue(String posId) {
    if (mNativeExpressAdViewCache.get(posId) == null) {
      mNativeExpressAdViewCache.put(posId, new ConcurrentLinkedQueue<NativeExpressADView>());
    }
    return mNativeExpressAdViewCache.get(posId);
  }

  private void putAdViewCache(String posId, List<NativeExpressADView> list) {
    ConcurrentLinkedQueue<NativeExpressADView> queue = getViewQueue(posId);
    queue.addAll(list);
  }

  private NativeExpressADView getAdView(String posId) {
    ConcurrentLinkedQueue<NativeExpressADView> queue = getViewQueue(posId);
    return (queue == null || queue.isEmpty()) ? null : queue.poll();
  }

  private abstract class AdListener implements NativeExpressAD.NativeExpressADListener {
    @Override
    public void onADLoaded(List<NativeExpressADView> list) {
      Log.i(Consts.TAG, "Adnet express ad render loaded");
    }

    @Override
    public void onRenderSuccess(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad render success");
    }

    @Override
    public void onRenderFail(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad render fail");
    }

    @Override
    public void onADExposure(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad showed");
    }

    @Override
    public void onADClicked(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad clicked");
    }

    @Override
    public void onADClosed(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad closed");
    }

    @Override
    public void onADLeftApplication(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad left app");
    }

    @Override
    public void onADOpenOverlay(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad open overlay");
    }

    @Override
    public void onADCloseOverlay(NativeExpressADView nativeExpressADView) {
      Log.i(Consts.TAG, "Adnet express ad close overlay");
    }

    @Override
    public void onNoAD(AdError adError) {
      Log.e(Consts.TAG, "No Ad Error " + adError.getErrorCode() + "," + adError.getErrorMsg());
    }
  }
}
