package com.jnhyxx.html5.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jnhnxx.livevideo.LivePlayer;
import com.jnhyxx.html5.Preference;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.BaseActivity;
import com.jnhyxx.html5.activity.MainActivity;
import com.jnhyxx.html5.activity.TradeActivity;
import com.jnhyxx.html5.activity.account.SignInActivity;
import com.jnhyxx.html5.domain.live.LastTeacherCommand;
import com.jnhyxx.html5.domain.live.LiveHomeChatInfo;
import com.jnhyxx.html5.domain.live.LiveMessage;
import com.jnhyxx.html5.domain.live.LiveSpeakInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.domain.local.SysTime;
import com.jnhyxx.html5.domain.market.Product;
import com.jnhyxx.html5.domain.market.ServerIpPort;
import com.jnhyxx.html5.domain.order.HomePositions;
import com.jnhyxx.html5.fragment.live.LiveInteractionFragment;
import com.jnhyxx.html5.fragment.live.LiveTeacherInfoFragment;
import com.jnhyxx.html5.fragment.live.TeacherGuideFragment;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback2;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.netty.NettyClient;
import com.jnhyxx.html5.netty.NettyHandler;
import com.jnhyxx.html5.utils.KeyBoardHelper;
import com.jnhyxx.html5.utils.ToastUtil;
import com.jnhyxx.html5.utils.UmengCountEventIdUtils;
import com.jnhyxx.html5.view.LiveProgrammeList;
import com.jnhyxx.html5.view.SlidingTabLayout;
import com.jnhyxx.html5.view.TeacherCommand;
import com.jnhyxx.html5.view.TitleBar;
import com.johnz.kutils.DateUtil;
import com.johnz.kutils.Launcher;
import com.johnz.kutils.net.CookieManger;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


public class LiveFragment extends BaseFragment implements LiveInteractionFragment.OnSendButtonClickListener {

    private static final int REQ_CODE_TRADE = 123;

    private static final int POS_LIVE_INTERACTION = 0;
    private static final int POS_TEACHER_ADVISE = 1;

    @BindView(R.id.slidingTabLayout)
    SlidingTabLayout mSlidingTabLayout;
    @BindView(R.id.viewPager)
    ViewPager mViewPager;
    @BindView(R.id.titleBar)
    TitleBar mTitleBar;

    @BindView(R.id.liveVideo)
    LivePlayer mLivePlayer;
    @BindView(R.id.videoContainer)
    RelativeLayout mVideoContainer;

    @BindView(R.id.publicNoticeArea)
    LinearLayout mPublicNoticeArea;
    @BindView(R.id.publicNotice)
    TextView mPublicNotice;

    @BindView(R.id.teacherCommand)
    TeacherCommand mTeacherCommand;
    @BindView(R.id.dimBackground)
    RelativeLayout mDimBackground;

    @BindView(R.id.showEditTextButton)
    ImageView mShowEditTextButton;
    @BindView(R.id.contactService)
    ImageView mContactService;

    private LiveProgrammeList mProgrammeList;
    private KeyBoardHelper mKeyBoardHelper;

    private List<Product> mProductList;

    private LiveMessage mLiveMessage;
    private LiveMessage.TeacherInfo mTeacher;
    private LiveMessage.NoticeInfo mNotice;
    private ServerIpPort mServerIpPort;
    private NettyClient mNettyClient;
    private boolean mStopLive;

    private LivePageFragmentAdapter mLivePageFragmentAdapter;
    private int mSelectedPage;

    private boolean mIsFragmentAdd = false;

    private Unbinder mBind;

    BroadcastReceiver LoginBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(SignInActivity.ACTION_LOGIN)) {
                LiveInteractionFragment fragment = (LiveInteractionFragment)
                        mLivePageFragmentAdapter.getFragment(POS_LIVE_INTERACTION);
                disconnectNettySocket();
                if (mLiveMessage != null) {
                    connectNettySocket();
                }
                if (fragment != null) {
                    fragment.updateLiveChatDataStatus();
                }
            }
        }
    };


    private NettyHandler mNettyHandler = new NettyHandler<LiveSpeakInfo>() {

        @Override
        public void onReceiveData(LiveSpeakInfo data) {
            if (getLiveInteractionFragment() != null) {
                getLiveInteractionFragment().setData(data);
            }

            LiveHomeChatInfo liveHomeChatInfo = new LiveHomeChatInfo(data);

            if (liveHomeChatInfo.getChatType() == LiveHomeChatInfo.CHAT_TYPE_TEACHER && liveHomeChatInfo.isOrder()) {
                mTeacherCommand.setTeacherCommand(liveHomeChatInfo);
            }

            if (liveHomeChatInfo.isOrder() || liveHomeChatInfo.getChatType() == LiveHomeChatInfo.CHAT_TYPE_TEACHER) {
                if (getTeacherGuideFragment() != null) {
                    getTeacherGuideFragment().setData(liveHomeChatInfo);
                }
            }
        }
    };


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_live, container, false);
        mBind = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(LoginBroadcastReceiver, new IntentFilter(SignInActivity.ACTION_LOGIN));

        mProgrammeList = new LiveProgrammeList(getActivity(), mDimBackground);
        mTeacherCommand.setOnClickListener(new TeacherCommand.OnClickListener() {
            @Override
            public void onTeacherHeadClick() {
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.LIVE_TEACHER_IMAGE);
                showTeacherInfoDialog();
            }

            @Override
            public void onCloseButtonClick(LiveHomeChatInfo teacherCommand) {
                Preference.get().setThisLastTeacherCommandShowed(teacherCommand);
            }
        });

        mLivePlayer.setOnScaleButtonClickListener(new LivePlayer.OnScaleButtonClickListener() {
            @Override
            public void onClick(boolean fullscreen) {
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.LIVE_FULL_SCREEN);
                if (fullscreen) {
                    mKeyBoardHelper.setOnKeyBoardStatusChangeListener(null);
                }
            }
        });

        mLivePlayer.setOnMuteButtonClickListener(new LivePlayer.OnMuteButtonClickListener() {
            @Override
            public void onClick(boolean isMute) {
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.LIVE_MUTE);
            }
        });

        mLivePlayer.setOnPlayClickListener(new LivePlayer.OnPlayClickListener() {
            @Override
            public void onClick(boolean isPlay) {
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.LIVE_PLAY);
            }
        });

        mNettyClient = new NettyClient();
        initTitleBar();
        initSlidingTabLayout();
        initKeyboardHelper();

        getLiveMessage();
        getChattingIpPort();

        updateLiveInfo();
    }

    public void updateLiveInfo() {
        LiveInteractionFragment liveInteractionFragment = getLiveInteractionFragment();
        if (liveInteractionFragment != null) {
            liveInteractionFragment.updateLiveChatDataStatus();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isAdded() && isVisibleToUser) {
            if (getLiveInteractionFragment() != null && !mIsFragmentAdd) {
                mIsFragmentAdd = true;
                getLiveInteractionFragment().setOnSendButtonClickListener(this);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLivePlayer.isStarted()) {
            mLivePlayer.stop();
        }
    }

    private void initKeyboardHelper() {
        mKeyBoardHelper = new KeyBoardHelper(getActivity());
        mKeyBoardHelper.onCreate();
        mKeyBoardHelper.setOnKeyBoardStatusChangeListener(mOnKeyBoardStatusChangeListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBind.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectNettySocket();
        mKeyBoardHelper.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(LoginBroadcastReceiver);
    }


    public void setBottomTabVisibility(int visibility) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) (getActivity())).getBottomTabs().setVisibility(visibility);
        }
    }

    private KeyBoardHelper.OnKeyBoardStatusChangeListener mOnKeyBoardStatusChangeListener
            = new KeyBoardHelper.OnKeyBoardStatusChangeListener() {
        @Override
        public void OnKeyBoardPop(int keyboardHeight) {
            mVideoContainer.setVisibility(GONE);
            if (getLiveInteractionFragment() != null) {
                getLiveInteractionFragment().setKeyboardOpened(true);
            }
            mShowEditTextButton.setVisibility(GONE);
            mContactService.setVisibility(GONE);
            setBottomTabVisibility(GONE);
        }

        @Override
        public void OnKeyBoardClose(int oldKeyboardHeight) {
            mVideoContainer.setVisibility(View.VISIBLE);
            if (getLiveInteractionFragment() != null) {
                getLiveInteractionFragment().setKeyboardOpened(false);
            }

            if (mSelectedPage > 0 ||
                    (getTeacherGuideFragment() != null && getTeacherGuideFragment().getUserVisibleHint())) {
                mShowEditTextButton.setVisibility(GONE);
                mContactService.setVisibility(GONE);
//                setBottomTabVisibility(GONE);
            } else {
                mShowEditTextButton.setVisibility(View.VISIBLE);
                mContactService.setVisibility(View.VISIBLE);
                setBottomTabVisibility(VISIBLE);
                if (getLiveInteractionFragment() != null) {
                    getLiveInteractionFragment().hideInputBox();
                }
            }
        }
    };


    @OnClick({R.id.showEditTextButton, R.id.contactService})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.showEditTextButton:
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.SPEAK);
                if (LocalUser.getUser().isLogin()) {
                    if (mStopLive) {
                        ToastUtil.show(R.string.live_time_is_not);
                    } else {
                        setBottomTabVisibility(GONE);
                        if (getLiveInteractionFragment() != null) {
                            getLiveInteractionFragment().showInputBox();
                        }
                    }
                } else {
                    startActivityForResult(new Intent(getActivity(), SignInActivity.class), BaseActivity.REQ_CODE_LOGIN);
                }
                break;

            case R.id.contactService:
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.CONNECT_SERVICE);
                String serviceQQUrl = API.getServiceQQ(Preference.get().getServiceQQ());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(serviceQQUrl));
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    ToastUtil.show(R.string.install_qq_first);
                }
                break;
        }
    }

    private void getLastTeacherCommand() {
        if (mTeacher == null) return;

        final int teacherId = mTeacher.getTeacherAccountId();
        API.Live.getLastTeacherGuide(teacherId).setTag(TAG)
                .setCallback(new Callback2<Resp<LastTeacherCommand>, LastTeacherCommand>() {
                    @Override
                    public void onRespSuccess(LastTeacherCommand lastTeacherCommand) {
                        LiveHomeChatInfo teacherCommand = lastTeacherCommand.getMsg();
                        if (teacherCommand == null) return;

                        if (!Preference.get().hasShowedThisLastTeacherCommand(teacherCommand)) {
                            long timeStamp = teacherCommand.getCreateTime();
                            long sysTime = SysTime.getSysTime().getSystemTimestamp();
                            if (DateUtil.isLessThanTimeInterval(sysTime, timeStamp, 60 * 1000)) {
                                mTeacherCommand.setTeacherCommand(teacherCommand);
                            }
                        }
                    }
                }).fireSync();
    }

    private void getChattingIpPort() {
        API.Market.getChattingServerIpAndPort().setTag(TAG)
                .setCallback(new Callback2<Resp<List<ServerIpPort>>, List<ServerIpPort>>() {
                    @Override
                    public void onRespSuccess(List<ServerIpPort> serverIpPorts) {
                        if (serverIpPorts != null && serverIpPorts.size() > 0) {
                            mServerIpPort = serverIpPorts.get(0);
                            if (mTeacher != null) {
                                connectNettySocket();
                            }
                        }
                    }
                }).fireSync();
    }

    private void getLiveMessage() {
        API.Live.getLiveMessage().setTag(TAG)
                .setCallback(new Callback2<Resp<LiveMessage>, LiveMessage>() {
                    @Override
                    public void onRespSuccess(LiveMessage liveMessage) {
                        mLiveMessage = liveMessage;
                        mTeacher = mLiveMessage.getTeacher();
                        mNotice = mLiveMessage.getNotice();
                        mStopLive = mLiveMessage.isStopLive();

                        if (mServerIpPort != null) {
                            connectNettySocket();
                        }

                        if (mStopLive) { // 未直播,显示通告
                            showNoLiveViews();
                        } else { // 在直播
                            if (mTeacher != null) {
                                if (getLiveInteractionFragment() != null) {
                                    getLiveInteractionFragment().setTeacherInfo(mTeacher);
                                }
                                showLiveViews();
                                getLastTeacherCommand();
                            }
                        }
                        mProgrammeList.setProgramme(mLiveMessage.getProgram());
                    }
                }).fireSync();
    }

    private void showLiveViews() {
        mPublicNoticeArea.setVisibility(GONE);
        mTeacherCommand.setVisibility(View.VISIBLE);
        mTeacherCommand.setTeacherHeader(mTeacher.getPictureUrl());
        connectRTMPServer(mLiveMessage.getActive());
    }

    private void showNoLiveViews() {
        mPublicNoticeArea.setVisibility(View.VISIBLE);
        mPublicNotice.setText(mNotice.getFormattedContent());
        mPublicNotice.setMovementMethod(new ScrollingMovementMethod());
        mTeacherCommand.setVisibility(GONE);
    }

    private void connectRTMPServer(LiveMessage.ActiveInfo active) {
        if (active != null && !TextUtils.isEmpty(active.getRtmp())) {
            mLivePlayer.setVideoPath(active.getRtmp());
//            this rtmp url will lead to crash
//            mLivePlayer.setVideoPath("rtmp://live.hkstv.hk.lxdns.com/live/hks");
        }
    }

    public void connectNettySocket() {
        if (mTeacher != null && mServerIpPort != null) {
            int teacherId = mLiveMessage.getTeacher().getTeacherAccountId();
            mNettyClient.setChattingIpAndPort(mServerIpPort.getIp(), mServerIpPort.getPort());
            mNettyClient.start(teacherId, CookieManger.getInstance().getCookies());
        }
        mNettyClient.addNettyHandler(mNettyHandler);
    }

    public void disconnectNettySocket() {
        mNettyClient.stop();
        mNettyClient.removeNettyHandler(mNettyHandler);
    }

    private void initSlidingTabLayout() {
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setDividerColors(ContextCompat.getColor(getActivity(), android.R.color.transparent));
        mSlidingTabLayout.setPadding(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                getResources().getDisplayMetrics()));
        mLivePageFragmentAdapter = new LivePageFragmentAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mLivePageFragmentAdapter);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mSelectedPage = position;
                if (position == POS_LIVE_INTERACTION) {
                    MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.LIVE_INTERACT);
                    if (!mShowEditTextButton.isShown() && !mContactService.isShown()) {
                        mShowEditTextButton.setVisibility(VISIBLE);
                        mContactService.setVisibility(VISIBLE);
                    }
                    setBottomTabVisibility(VISIBLE);
                } else if (position == POS_TEACHER_ADVISE) {
                    MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.TEACHER_GUIDE);
                    mShowEditTextButton.setVisibility(GONE);
                    mContactService.setVisibility(GONE);
                    if (getLiveInteractionFragment() != null) {
                        getLiveInteractionFragment().hideInputBox();
                    }
                    setBottomTabVisibility(VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private TeacherGuideFragment getTeacherGuideFragment() {
        return (TeacherGuideFragment) mLivePageFragmentAdapter.getFragment(POS_TEACHER_ADVISE);
    }

    private LiveInteractionFragment getLiveInteractionFragment() {
        return (LiveInteractionFragment) mLivePageFragmentAdapter.getFragment(POS_LIVE_INTERACTION);
    }

    private void initTitleBar() {
        mTitleBar.setOnRightViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getLiveInteractionFragment() != null) {
                    getLiveInteractionFragment().hideInputBox();
                }
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.LIVE_TRADE);
                switchToTradePage();
            }
        });
        View customView = mTitleBar.getCustomView();
        LinearLayout liveProgramme = (LinearLayout) customView.findViewById(R.id.liveProgramme);
        liveProgramme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.PROGRAMME);
                showLiveProgramme();
            }
        });
    }

    private void showLiveProgramme() {
        if (mProgrammeList != null) {
            mProgrammeList.show(mTitleBar);
        }
    }

    private void showTeacherInfoDialog() {
        LiveMessage.TeacherInfo teacherInfo = mLiveMessage.getTeacher();
        if (teacherInfo != null) {
            LiveTeacherInfoFragment liveTeacherInfoFragment
                    = LiveTeacherInfoFragment.newInstance(teacherInfo);
            liveTeacherInfoFragment.show(getChildFragmentManager());
        }
    }

    private void requestPositions() {
        API.Order.getHomePositions()
                .setTag(TAG).setIndeterminate(this)
                .setCallback(new Callback2<Resp<HomePositions>, HomePositions>() {
                    @Override
                    public void onRespSuccess(HomePositions homePositions) {
                        requestProductList(homePositions);
                    }
                }).fire();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BaseActivity.REQ_CODE_LOGIN && resultCode == getActivity().RESULT_OK) {
//            LiveInteractionFragment fragment = (LiveInteractionFragment)
//                    mLivePageFragmentAdapter.getFragment(POS_LIVE_INTERACTION);
//            disconnectNettySocket();
//            if (mLiveMessage != null) {
//                connectNettySocket();
//            }
//            if (fragment != null) {
//                fragment.updateLiveChatDataStatus();
//            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTitleBar.setVisibility(GONE);
            mTeacherCommand.setVisibility(GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            mVideoContainer.setLayoutParams(params);

            if (mShowEditTextButton.isShown() && mContactService.isShown()) {
                mShowEditTextButton.setVisibility(GONE);
                mContactService.setVisibility(GONE);
            }
            setBottomTabVisibility(GONE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mTitleBar.setVisibility(View.VISIBLE);
            mTeacherCommand.setVisibility(View.VISIBLE);
            int playerHeight = getResources().getDimensionPixelOffset(R.dimen.player_height);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    playerHeight);
            mVideoContainer.setLayoutParams(params);

            if (getLiveInteractionFragment() != null
                    && getLiveInteractionFragment().getUserVisibleHint()) {
                mShowEditTextButton.setVisibility(View.VISIBLE);
                mContactService.setVisibility(View.VISIBLE);
            }
            setBottomTabVisibility(VISIBLE);

            // remove keyboard listener when click full screen before, add it now
            mKeyBoardHelper.setOnKeyBoardStatusChangeListener(mOnKeyBoardStatusChangeListener);
        }
    }

    private void requestProductList(final HomePositions homePositions) {
        API.Market.getProductList()
                .setTag(TAG).setIndeterminate(this)
                .setCallback(new Callback2<Resp<List<Product>>, List<Product>>() {
                    @Override
                    public void onRespSuccess(List<Product> products) {
                        mProductList = products;
                        List<HomePositions.CashOpSBean> cashOpSBeanList = null;
                        if (homePositions != null) {
                            cashOpSBeanList = homePositions.getCashOpS();
                        }
                        Product enterProduct = null;
                        if (cashOpSBeanList != null && cashOpSBeanList.size() > 0) { // has cash positions
                            HomePositions.CashOpSBean cashOpSBean = cashOpSBeanList.get(0);
                            for (Product product : mProductList) {
                                if (product.getVarietyType().equals(cashOpSBean.getVarietyType())) {
                                    enterProduct = product;
                                    break;
                                }
                            }
                        } else {
                            for (Product product : mProductList) {
                                if (product.getVarietyType().equalsIgnoreCase(Product.VARIETY_TYPE_US_CRUDE)) {
                                    enterProduct = product;
                                }
                            }
                        }
                        if (enterProduct != null) {
                            Launcher.with(getActivity(), TradeActivity.class)
                                    .putExtra(Product.EX_PRODUCT, enterProduct)
                                    .putExtra(Product.EX_FUND_TYPE, Product.FUND_TYPE_CASH)
                                    .putExtra(Product.EX_PRODUCT_LIST, new ArrayList<>(mProductList))
                                    .executeForResult(REQ_CODE_TRADE);
                        }
                    }
                }).fire();
    }

    private void switchToTradePage() {
//        if (getCallingActivity() != null
//                && getCallingActivity().getClassName().equals(TradeActivity.class.getName())) {
//            finish();
//        } else {
        if (LocalUser.getUser().isLogin()) {
            requestPositions();
        } else {
            requestProductList(null);
        }

    }

    @Override
    public void onSendButtonClick(String message) {
        mNettyClient.sendMessage(message);
        if (getLiveInteractionFragment() != null) {
            getLiveInteractionFragment().hideInputBox();
        }
    }

    class LivePageFragmentAdapter extends FragmentPagerAdapter {

        private FragmentManager mFragmentManager;

        public LivePageFragmentAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.live_interaction);
                case 1:
                    return getString(R.string.live_teacher_guide);
            }
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return LiveInteractionFragment.newInstance();
                case 1:
                    return TeacherGuideFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        public Fragment getFragment(int position) {
            return mFragmentManager.findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + position);
        }
    }
}
