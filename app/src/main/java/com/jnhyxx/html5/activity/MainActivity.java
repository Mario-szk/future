package com.jnhyxx.html5.activity;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.webkit.WebView;

import com.jnhyxx.html5.R;
import com.jnhyxx.html5.domain.account.UserFundInfo;
import com.jnhyxx.html5.domain.account.UserInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.fragment.HomeFragment;
import com.jnhyxx.html5.fragment.InfoFragment;
import com.jnhyxx.html5.fragment.MineFragment;
import com.jnhyxx.html5.fragment.dialog.UpgradeDialog;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback1;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.Network;
import com.jnhyxx.html5.utils.NotificationUtil;
import com.jnhyxx.html5.utils.ToastUtil;
import com.jnhyxx.html5.utils.UpgradeUtil;
import com.jnhyxx.html5.view.BottomTabs;

import java.net.URISyntaxException;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.ContentValues.TAG;
import static com.jnhyxx.html5.R.styleable.BottomTabs;
import static com.jnhyxx.html5.utils.Network.registerNetworkChangeReceiver;
import static com.jnhyxx.html5.utils.Network.unregisterNetworkChangeReceiver;

public class MainActivity extends BaseActivity {

    @BindView(R.id.bottomTabs)
    BottomTabs mBottomTabs;
    @BindView(R.id.viewPager)
    ViewPager mViewPager;

    private MainFragmentsAdapter mMainFragmentsAdapter;

    private BroadcastReceiver mNetworkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        checkVersion();

        initView();

        processIntent(getIntent());

        mNetworkChangeReceiver = new NetworkReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewPager.clearOnPageChangeListeners();
    }

    private void initView() {
        mMainFragmentsAdapter = new MainFragmentsAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mMainFragmentsAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mBottomTabs.selectTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mViewPager.setCurrentItem(0);
        mBottomTabs.setOnTabClickListener(new BottomTabs.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                mBottomTabs.selectTab(position);
                mViewPager.setCurrentItem(position, false);
            }
        });
    }

    private void processIntent(Intent intent) {
        final String messageId = intent.getStringExtra(NotificationUtil.KEY_MESSAGE_ID);
//        if (!TextUtils.isEmpty(messageId)) {
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mWebView.loadUrl(APIBase.getMessageDetail(messageId));
//                }
//            });
//            return;
//        }
//
//        final String messageType = intent.getStringExtra(NotificationUtil.KEY_MESSAGE_TYPE);
//        if (!TextUtils.isEmpty(messageType)) {
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mWebView.loadUrl(APIBase.getMessageList(messageType));
//                }
//            });
//        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void checkVersion() {
        UpgradeUtil.log(this);
        if (UpgradeUtil.hasNewVersion(this)) {
            boolean forceUpgrade = UpgradeUtil.isForceUpgrade(this);
            DialogFragment dialogFragment = UpgradeDialog.newInstance(forceUpgrade);
            dialogFragment.show(getSupportFragmentManager(), "upgrade");
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerNetworkChangeReceiver(this, mNetworkChangeReceiver);

        final LocalUser localUser = LocalUser.getUser();
        final UserInfo userInfo = localUser.getUserInfo();
        API.Finance.getFundInfo().setTag(TAG)
                .setIndeterminate(this)
                .setCallback(new Callback1<Resp<UserFundInfo>>() {
                    @Override
                    protected void onRespSuccess(Resp<UserFundInfo> resp) {
                        UserFundInfo userFundInfo = resp.getData();
                        userInfo.setMoneyUsable(userFundInfo.getMoneyUsable());
                        userInfo.setScoreUsable(userFundInfo.getScoreUsable());
                        localUser.setUserInfo(userInfo);
                    }
                }).fire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkChangeReceiver(this, mNetworkChangeReceiver);
    }

    private void openQQChat(WebView webView, String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                ToastUtil.show(R.string.install_qq_first);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        while (webView.canGoBack()) {
            webView.goBack();
        }
    }

    private void openAlipay(WebView webView, String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private class NetworkReceiver extends Network.NetworkChangeReceiver {

        @Override
        protected void onNetworkChanged(int availableNetworkType) {
            if (availableNetworkType > Network.NET_NONE) {

            }
        }
    }

    private class MainFragmentsAdapter extends FragmentPagerAdapter {

        public MainFragmentsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new InfoFragment();
                case 2:
                    return new MineFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
