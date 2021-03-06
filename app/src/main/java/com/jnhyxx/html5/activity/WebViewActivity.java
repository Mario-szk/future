package com.jnhyxx.html5.activity;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.jnhyxx.html5.AppJs;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.dialog.SaveImageActivity;
import com.jnhyxx.html5.utils.Network;
import com.jnhyxx.html5.view.TitleBar;
import com.johnz.kutils.Launcher;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.jnhyxx.html5.utils.Network.isNetworkAvailable;
import static com.jnhyxx.html5.utils.Network.registerNetworkChangeReceiver;
import static com.jnhyxx.html5.utils.Network.unregisterNetworkChangeReceiver;

public class WebViewActivity extends BaseActivity {

    public static final String EX_URL = "url";
    public static final String EX_TITLE = "title";
    public static String EX_RAW_COOKIE = "rawCookie";
    public static final String EX_HTML = "html";

    @BindView(R.id.titleBar)
    TitleBar mTitleBar;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.webView)
    WebView mWebView;
    @BindView(R.id.refreshButton)
    Button mRefreshButton;
    @BindView(R.id.errorPage)
    LinearLayout mErrorPage;

    private boolean mLoadSuccess;
    protected String mPageUrl;
    protected String mTitle;
    protected String mRawCookie;
    protected String mPureHtml;

    private BroadcastReceiver mNetworkChangeReceiver;
    private WebViewClient mWebViewClient;

    public TitleBar getTitleBar() {
        return mTitleBar;
    }

    public String getRawCookie() {
        return mRawCookie;
    }

    public WebView getWebView() {
        return mWebView;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        ButterKnife.bind(this);

        mNetworkChangeReceiver = new NetworkReceiver();
        mLoadSuccess = true;

        initData(getIntent());
        initWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

    public void destroy() {
        if (mWebView != null) {
            // 5.1 如果先调用destroy()方法，则会命中if (isDestroyed()) return;这一行代码，需要先onDetachedFromWindow()，再
            // destory()
            ViewParent parent = mWebView.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(mWebView);
            }
            mWebView.stopLoading();
            // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
            mWebView.getSettings().setJavaScriptEnabled(false);
            mWebView.clearHistory();
            mWebView.clearView();
            mWebView.removeAllViews();
            try {
                mWebView.destroy();
            } catch (Throwable ex) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    protected void initData(Intent intent) {
        mTitle = intent.getStringExtra(EX_TITLE);
        mPageUrl = intent.getStringExtra(EX_URL);
        mRawCookie = intent.getStringExtra(EX_RAW_COOKIE);
        mPureHtml = intent.getStringExtra(EX_HTML);

        tryToFixPageUrl();
    }

    private void tryToFixPageUrl() {
        if (!TextUtils.isEmpty(mPageUrl)) {
            if (!mPageUrl.startsWith("http")) { // http or https
                mPageUrl = "http://" + mPageUrl;
            }
        }
    }

    protected void initWebView() {
        // init cookies
        initCookies(mRawCookie, mPageUrl);

        // init webSettings
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUserAgentString(webSettings.getUserAgentString()
                + " ###" + getString(R.string.android_web_agent) + "/1.0");
        //mWebView.getSettings().setAppCacheEnabled(true);l
        //webSettings.setAppCachePath(getExternalCacheDir().getPath());
        webSettings.setAllowFileAccess(true);

        // performance improve
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setEnableSmoothTransition(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);


        mWebView.clearHistory();
        mWebView.clearCache(true);
        mWebView.clearFormData();
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.addJavascriptInterface(new AppJs(this), "AppJs");

        if (Build.VERSION.SDK_INT >= 19) {
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        mWebViewClient = new WebViewClient();
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) {
                    mProgress.setVisibility(View.GONE);
                } else {
                    if (mProgress.getVisibility() == View.GONE) {
                        mProgress.setVisibility(View.VISIBLE);
                    }
                    mProgress.setProgress(newProgress);
                }
            }
        });
        mWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult result = mWebView.getHitTestResult();
                if (result != null) {
                    int type = result.getType();
                    if (type == WebView.HitTestResult.IMAGE_TYPE) {
                        showSaveImageDialog(result);
                    }
                }
                return false;
            }
        });
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });


        loadPage();
    }

    protected void initCookies(String rawCookie, String pageUrl) {
        if (!TextUtils.isEmpty(rawCookie) && !TextUtils.isEmpty(pageUrl)) {
            String[] cookies = rawCookie.split("\n");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().removeSessionCookies(null);
            } else {
                CookieManager.getInstance().removeAllCookie();
            }
            CookieManager.getInstance().setAcceptCookie(true);
            for (String cookie : cookies) {
                CookieManager.getInstance().setCookie(pageUrl, cookie);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().flush();
            } else {
                CookieSyncManager.getInstance().sync();
            }
        }
    }

    protected void loadPage() {
        if (!TextUtils.isEmpty(mPageUrl)) {
            mWebView.loadUrl(mPageUrl);
        } else if (!TextUtils.isEmpty(mPureHtml)) {
            mWebView.loadData(mPureHtml, "text/html", "utf-8");
        }
    }

    protected class WebViewClient extends android.webkit.WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mLoadSuccess = true;
            mPageUrl = url;

            if (!isNetworkAvailable()) {
                mLoadSuccess = false;
                mWebView.stopLoading();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mLoadSuccess) {
                mWebView.setVisibility(View.VISIBLE);
                mErrorPage.setVisibility(View.GONE);
            } else {
                mWebView.setVisibility(View.GONE);
                mErrorPage.setVisibility(View.VISIBLE);
            }
            if (isNeedViewTitle()) {
                String titleText = view.getTitle();
                if (!TextUtils.isEmpty(titleText) && !url.contains(titleText)) {
                    mTitle = titleText;
                }
                mTitleBar.setTitle(mTitle);
            } else {
                mTitleBar.setTitle(mTitle);
            }

        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String requestUrl = request.getUrl().toString();
                if (mPageUrl.equalsIgnoreCase(requestUrl) && error.getErrorCode() <= ERROR_UNKNOWN) {
                    mLoadSuccess = false;
                }
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (mPageUrl.equalsIgnoreCase(failingUrl) && errorCode <= ERROR_UNKNOWN) {
                mLoadSuccess = false;
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (onShouldOverrideUrlLoading(view, url)) {
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    protected boolean isNeedViewTitle() {
        return true;
    }

    protected boolean onShouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerNetworkChangeReceiver(this, mNetworkChangeReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkChangeReceiver(this, mNetworkChangeReceiver);
        mWebView.onPause();
    }

    private void showSaveImageDialog(WebView.HitTestResult result) {
        final String imageUrl = result.getExtra();
        Launcher.with(this, SaveImageActivity.class)
                .putExtra(SaveImageActivity.EXTRA_IMAGE_URL, imageUrl)
                .execute();
    }

    @OnClick(R.id.refreshButton)
    public void onClick() {
        mWebView.reload();
    }

    private class NetworkReceiver extends Network.NetworkChangeReceiver {

        @Override
        protected void onNetworkChanged(int availableNetworkType) {
            if (availableNetworkType > Network.NET_NONE && !mLoadSuccess) {
                if (mWebView != null) {
                    mWebView.reload();
                }
            }
        }
    }

}
