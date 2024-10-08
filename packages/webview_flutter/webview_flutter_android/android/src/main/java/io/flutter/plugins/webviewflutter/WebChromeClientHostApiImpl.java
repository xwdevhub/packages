// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebChromeClientHostApi;

import java.util.Objects;

import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.view.View;
import android.view.WindowManager;
import android.content.Context;

/**
 * Host api implementation for {@link WebChromeClient}.
 *
 * <p>Handles creating {@link WebChromeClient}s that intercommunicate with a paired Dart object.
 */
public class WebChromeClientHostApiImpl implements WebChromeClientHostApi {
    private final InstanceManager instanceManager;
    private final WebChromeClientCreator webChromeClientCreator;
    private final WebChromeClientFlutterApiImpl flutterApi;

    /**
     * Implementation of {@link WebChromeClient} that passes arguments of callback methods to Dart.
     */
    public static class WebChromeClientImpl extends SecureWebChromeClient {
        private final WebChromeClientFlutterApiImpl flutterApi;
        private boolean returnValueForOnShowFileChooser = false;

        /**
         * Creates a {@link WebChromeClient} that passes arguments of callbacks methods to Dart.
         *
         * @param flutterApi handles sending messages to Dart
         */
        public WebChromeClientImpl(@NonNull WebChromeClientFlutterApiImpl flutterApi) {
            this.flutterApi = flutterApi;
        }

        @Override
        public void onProgressChanged(WebView view, int progress) {
            flutterApi.onProgressChanged(this, view, (long) progress, reply -> {
            });
        }
        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                flutterApi.onReceivedTitle(
                        this,
                        view,
                        title,
                        reply -> {

                        });
            }
        }


        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // 进入全屏模式
            // 在这里你可以添加代码来处理全屏模式的UI和逻辑
            // android.util.Log.i("进入全屏模式");
            if (windowManager == null) {
                windowManager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
            }
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, // 设置宽度为匹配父容器
                    WindowManager.LayoutParams.MATCH_PARENT, // 设置高度为匹配父容器
                    WindowManager.LayoutParams.TYPE_APPLICATION, // 窗口类型
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,// 使用全屏标志
                    PixelFormat.TRANSLUCENT // 透明度设置为半透明
            );
            params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            windowManager.addView(view, params);
            WebViewFlutterPlugin.activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            fullScreenPlayer = view;

        }

        private View fullScreenPlayer;
        private WindowManager windowManager;

        @Override
        public void onHideCustomView() {
            // 退出全屏模式
            // 在这里你可以添加代码来处理退出全屏模式的UI和逻辑
            // android.util.Log.i("退出全屏模式");
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            windowManager.removeViewImmediate(fullScreenPlayer);
            WebViewFlutterPlugin.activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            fullScreenPlayer = null;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams) {
            final boolean currentReturnValueForOnShowFileChooser = returnValueForOnShowFileChooser;
            flutterApi.onShowFileChooser(
                    this,
                    webView,
                    fileChooserParams,
                    reply -> {
                        // The returned list of file paths can only be passed to `filePathCallback` if the
                        // `onShowFileChooser` method returned true.
                        if (currentReturnValueForOnShowFileChooser) {
                            final Uri[] filePaths = new Uri[reply.size()];
                            for (int i = 0; i < reply.size(); i++) {
                                filePaths[i] = Uri.parse(reply.get(i));
                            }
                            filePathCallback.onReceiveValue(filePaths);
                        }
                    });
            return currentReturnValueForOnShowFileChooser;
        }



        /**
         * Sets return value for {@link #onShowFileChooser}.
         */
        public void setReturnValueForOnShowFileChooser(boolean value) {
            returnValueForOnShowFileChooser = value;
        }
    }

    /**
     * Implementation of {@link WebChromeClient} that only allows secure urls when opening a new
     * window.
     */
    public static class SecureWebChromeClient extends WebChromeClient {
        @Nullable
        private WebViewClient webViewClient;

        @Override
        public boolean onCreateWindow(
                final WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            return onCreateWindow(view, resultMsg, new WebView(view.getContext()));
        }

        /**
         * Verifies that a url opened by `Window.open` has a secure url.
         *
         * @param view                  the WebView from which the request for a new window originated.
         * @param resultMsg             the message to send when once a new WebView has been created. resultMsg.obj
         *                              is a {@link WebView.WebViewTransport} object. This should be used to transport the new
         *                              WebView, by calling WebView.WebViewTransport.setWebView(WebView)
         * @param onCreateWindowWebView the temporary WebView used to verify the url is secure
         * @return this method should return true if the host application will create a new window, in
         * which case resultMsg should be sent to its target. Otherwise, this method should return
         * false. Returning false from this method but also sending resultMsg will result in
         * undefined behavior
         */
        @VisibleForTesting
        boolean onCreateWindow(
                final WebView view, Message resultMsg, @Nullable WebView onCreateWindowWebView) {
            // WebChromeClient requires a WebViewClient because of a bug fix that makes
            // calls to WebViewClient.requestLoading/WebViewClient.urlLoading when a new
            // window is opened. This is to make sure a url opened by `Window.open` has
            // a secure url.
            if (webViewClient == null) {
                return false;
            }

            final WebViewClient windowWebViewClient =
                    new WebViewClient() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public boolean shouldOverrideUrlLoading(
                                @NonNull WebView windowWebView, @NonNull WebResourceRequest request) {
                            if (!webViewClient.shouldOverrideUrlLoading(view, request)) {
                                view.loadUrl(request.getUrl().toString());
                            }
                            return true;
                        }

                        // Legacy codepath for < N.
                        @SuppressWarnings("deprecation")
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView windowWebView, String url) {
                            if (!webViewClient.shouldOverrideUrlLoading(view, url)) {
                                view.loadUrl(url);
                            }
                            return true;
                        }

                        @Override
                        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                            handler.proceed();
                        }
                    };

            if (onCreateWindowWebView == null) {
                onCreateWindowWebView = new WebView(view.getContext());
            }
            onCreateWindowWebView.setWebViewClient(windowWebViewClient);

            final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(onCreateWindowWebView);
            resultMsg.sendToTarget();

            return true;
        }

        /**
         * Set the {@link WebViewClient} that calls to {@link WebChromeClient#onCreateWindow} are passed
         * to.
         *
         * @param webViewClient the forwarding {@link WebViewClient}
         */
        public void setWebViewClient(@NonNull WebViewClient webViewClient) {
            this.webViewClient = webViewClient;
        }
    }

    /**
     * Handles creating {@link WebChromeClient}s for a {@link WebChromeClientHostApiImpl}.
     */
    public static class WebChromeClientCreator {
        /**
         * Creates a {@link DownloadListenerHostApiImpl.DownloadListenerImpl}.
         *
         * @param flutterApi handles sending messages to Dart
         * @return the created {@link WebChromeClientHostApiImpl.WebChromeClientImpl}
         */
        public WebChromeClientImpl createWebChromeClient(WebChromeClientFlutterApiImpl flutterApi) {
            return new WebChromeClientImpl(flutterApi);
        }
    }

    /**
     * Creates a host API that handles creating {@link WebChromeClient}s.
     *
     * @param instanceManager        maintains instances stored to communicate with Dart objects
     * @param webChromeClientCreator handles creating {@link WebChromeClient}s
     * @param flutterApi             handles sending messages to Dart
     */
    public WebChromeClientHostApiImpl(
            InstanceManager instanceManager,
            WebChromeClientCreator webChromeClientCreator,
            WebChromeClientFlutterApiImpl flutterApi) {
        this.instanceManager = instanceManager;
        this.webChromeClientCreator = webChromeClientCreator;
        this.flutterApi = flutterApi;
    }

    @Override
    public void create(Long instanceId) {
        final WebChromeClient webChromeClient =
                webChromeClientCreator.createWebChromeClient(flutterApi);
        instanceManager.addDartCreatedInstance(webChromeClient, instanceId);
    }

    @Override
    public void setSynchronousReturnValueForOnShowFileChooser(
            @NonNull Long instanceId, @NonNull Boolean value) {
        final WebChromeClientImpl webChromeClient =
                Objects.requireNonNull(instanceManager.getInstance(instanceId));
        webChromeClient.setReturnValueForOnShowFileChooser(value);
    }
}
