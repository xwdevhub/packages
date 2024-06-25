// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.os.Build;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import androidx.annotation.RequiresApi;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebChromeClientFlutterApi;
import java.util.List;
import java.util.Objects;

/**
 * Flutter Api implementation for {@link WebChromeClient}.
 *
 * <p>Passes arguments of callbacks methods from a {@link WebChromeClient} to Dart.
 */
public class WebChromeClientFlutterApiImpl extends WebChromeClientFlutterApi {
  private final BinaryMessenger binaryMessenger;
  private final InstanceManager instanceManager;
  private final WebViewFlutterApiImpl webViewFlutterApi;

  /**
   * Creates a Flutter api that sends messages to Dart.
   *
   * @param binaryMessenger handles sending messages to Dart
   * @param instanceManager maintains instances stored to communicate with Dart objects
   */
  public WebChromeClientFlutterApiImpl(
      BinaryMessenger binaryMessenger, InstanceManager instanceManager) {
    super(binaryMessenger);
    this.binaryMessenger = binaryMessenger;
    this.instanceManager = instanceManager;
    webViewFlutterApi = new WebViewFlutterApiImpl(binaryMessenger, instanceManager);
  }

  /** Passes arguments from {@link WebChromeClient#onProgressChanged} to Dart. */
  public void onProgressChanged(
      WebChromeClient webChromeClient, WebView webView, Long progress, Reply<Void> callback) {
    webViewFlutterApi.create(webView, reply -> {});

    final Long webViewIdentifier =
        Objects.requireNonNull(instanceManager.getIdentifierForStrongReference(webView));
    super.onProgressChanged(
        getIdentifierForClient(webChromeClient), webViewIdentifier, progress, callback);
  }
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void onReceivedTitle(
          WebChromeClient webChromeClient,
          WebView webView,
          String title,
          Reply<Void> callback) {
    webViewFlutterApi.create(webView, reply -> {});

    final Long webViewIdentifier =
            Objects.requireNonNull(instanceManager.getIdentifierForStrongReference(webView));
    super.onReceivedTitle(
            getIdentifierForClient(webChromeClient), webViewIdentifier, title, callback);

  }

  /** Passes arguments from {@link WebChromeClient#onShowFileChooser} to Dart. */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void onShowFileChooser(
      WebChromeClient webChromeClient,
      WebView webView,
      WebChromeClient.FileChooserParams fileChooserParams,
      Reply<List<String>> callback) {
    webViewFlutterApi.create(webView, reply -> {});

    new FileChooserParamsFlutterApiImpl(binaryMessenger, instanceManager)
        .create(fileChooserParams, reply -> {});

    onShowFileChooser(
        Objects.requireNonNull(instanceManager.getIdentifierForStrongReference(webChromeClient)),
        Objects.requireNonNull(instanceManager.getIdentifierForStrongReference(webView)),
        Objects.requireNonNull(instanceManager.getIdentifierForStrongReference(fileChooserParams)),
        callback);
  }


  private long getIdentifierForClient(WebChromeClient webChromeClient) {
    final Long identifier = instanceManager.getIdentifierForStrongReference(webChromeClient);
    if (identifier == null) {
      throw new IllegalStateException("Could not find identifier for WebChromeClient.");
    }
    return identifier;
  }
}
