package com.mrumi.dnd5edicelab;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String EXTRA_LOWER_DISPLAY = "dnd_lower_display";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        final boolean lowerDisplay = getIntent().getBooleanExtra(EXTRA_LOWER_DISPLAY, false);
        boolean dualScreen = false;
        if (!lowerDisplay && isAynThor()) {
            int secondDisplayId = findSecondDisplayId();
            if (secondDisplayId >= 0) dualScreen = launchLowerDisplay(secondDisplayId);
        }

        if (lowerDisplay) {
            // The Thor's lower panel is a dedicated touch dice surface. Hide Android chrome there.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF120E0B);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(webView);

        if (savedInstanceState == null) {
            if (lowerDisplay) {
                webView.loadUrl("file:///android_asset/index.html?thorScreen=bottom");
            } else if (dualScreen) {
                webView.loadUrl("file:///android_asset/index.html?thorScreen=top");
            } else {
                // Normal one-screen behavior on Galaxy phones/tablets and non-Thor Android devices.
                webView.loadUrl("file:///android_asset/index.html");
            }
        } else {
            webView.restoreState(savedInstanceState);
        }
        webView.requestFocus(View.FOCUS_DOWN);
    }

    private boolean isAynThor() {
        String fingerprint = ((Build.MANUFACTURER == null ? "" : Build.MANUFACTURER) + " "
                + (Build.BRAND == null ? "" : Build.BRAND) + " "
                + (Build.MODEL == null ? "" : Build.MODEL) + " "
                + (Build.DEVICE == null ? "" : Build.DEVICE)).toLowerCase(Locale.ROOT);
        // AYN firmware normally exposes AYN and/or Thor in these identifiers. The screen-count check
        // below is still required, so a single-display AYN device will simply use the normal layout.
        return fingerprint.contains("ayn") || fingerprint.contains("thor");
    }

    private int findSecondDisplayId() {
        DisplayManager manager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (manager == null) return -1;
        Display[] displays = manager.getDisplays();
        if (displays == null) return -1;
        for (Display display : displays) {
            if (display != null && display.isValid() && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                return display.getDisplayId();
            }
        }
        return -1;
    }

    private boolean launchLowerDisplay(int displayId) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(EXTRA_LOWER_DISPLAY, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);
            startActivity(intent, options.toBundle());
            return true;
        } catch (RuntimeException ex) {
            // Graceful fallback: preserve the complete one-screen UI if vendor firmware denies the
            // secondary-display launch instead of hiding the dice tray from the primary display.
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
