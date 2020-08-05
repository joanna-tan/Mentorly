package com.example.mentorly.zoom;


import android.content.Context;
import android.util.Log;

import com.example.mentorly.R;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKRawDataMemoryMode;

/**
 * Init and auth zoom sdk first before using SDK interfaces
 */
public class ZoomInitAuthSDKHelper implements ZoomSDKInitializeListener {

    private final static String TAG = "ZoomInitAuthSDKHelper";
    public final static String WEB_DOMAIN = "zoom.us";

    private static ZoomInitAuthSDKHelper mInitAuthSDKHelper;

    private ZoomSDK mZoomSDK;

    private ZoomInitAuthSDKHelper mInitAuthSDKCallback;

    public ZoomInitAuthSDKHelper() {
        mZoomSDK = ZoomSDK.getInstance();
    }

    public synchronized static ZoomInitAuthSDKHelper getInstance() {
        mInitAuthSDKHelper = new ZoomInitAuthSDKHelper();
        return mInitAuthSDKHelper;
    }

    /**
     * init sdk method
     */
    public void initSDK(Context context, ZoomInitAuthSDKHelper callback) {
        if (!mZoomSDK.isInitialized()) {
            mInitAuthSDKCallback = callback;
            ZoomSDKInitParams initParams = new ZoomSDKInitParams();
            initParams.appKey = context.getString(R.string.zoom_client_key);
            initParams.appSecret = context.getString(R.string.zoom_client_secret);
            initParams.enableLog = true;
            initParams.enableGenerateDump = true;
            initParams.logSize = 50;
            initParams.domain = WEB_DOMAIN;
            initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;
            mZoomSDK.initialize(context, this, initParams);
        }
    }

    /**
     * init sdk callback
     *
     * @param errorCode         defined in {@link us.zoom.sdk.ZoomError}
     * @param internalErrorCode Zoom internal error code
     */
    @Override
    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
        Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

        if (mInitAuthSDKCallback != null) {
            mInitAuthSDKCallback.onZoomSDKInitializeResult(errorCode, internalErrorCode);
        }
    }

    @Override
    public void onZoomAuthIdentityExpired() {
        Log.e(TAG, "onZoomAuthIdentityExpired in init");
    }

    public void reset() {
        mInitAuthSDKCallback = null;
    }
}
