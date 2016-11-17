package com.jnhyxx.html5.fragment.live;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.gson.Gson;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.account.SignInActivity;
import com.jnhyxx.html5.domain.live.ChatData;
import com.jnhyxx.html5.domain.live.LiveHomeChatInfo;
import com.jnhyxx.html5.domain.live.LiveMessage;
import com.jnhyxx.html5.domain.live.LiveSpeakInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.fragment.BaseFragment;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.netty.NettyClient;
import com.jnhyxx.html5.utils.Network;
import com.jnhyxx.html5.utils.ToastUtil;
import com.johnz.kutils.DateUtil;
import com.johnz.kutils.Launcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.jnhyxx.html5.R.id.listView;


/**
 * Created by ${wangJie} on 2016/11/8.
 * 直播互动界面
 */

public class LiveInteractionFragment extends BaseFragment implements AbsListView.OnScrollListener, View.OnKeyListener {

    private static final int REQUEST_CODE_LOGIN = 583;

    @BindView(listView)
    ListView mListView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.empty)
    TextView mEmpty;
    @BindView(R.id.liveSpeak)
    ImageView mLiveSpeak;
    @BindView(R.id.speakEditText)
    EditText mSpeakEditText;

    //发送发言
    @BindView(R.id.sendSpeak)
    TextView mSendSpeak;
    @BindView(R.id.speakLayout)
    LinearLayout mSpeakLayout;

    private Unbinder mBind;

    private TextView mFooter;

    private int mPage = 0;
    private int mPageSize = 0;
    private long mTimeStamp = 0;
    private LiveChatInfoAdapter mLiveChatInfoAdapter;

    private HashSet<Long> mHashSet;


    private List<ChatData> mChatDataListInfo;
    private InputMethodManager mInputMethodManager;

    private ArrayList<ChatData> mDataArrayList;

    //用来记录时间好超过5分钟的集合
    private ArrayList<Integer> mTimeMoreThanFiveItemPosition;

    private boolean isRefreshed;

    //判断用户是否被禁言
    private boolean recordUserIsDeny;

    public static LiveInteractionFragment newInstance() {
        Bundle args = new Bundle();
        LiveInteractionFragment fragment = new LiveInteractionFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);
        mBind = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBind.unbind();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        mListView.setStackFromBottom(true);

        mPageSize = 10;
        mHashSet = new HashSet<>();
        mDataArrayList = new ArrayList<>();
        mTimeMoreThanFiveItemPosition = new ArrayList<>();
        mListView.setOnScrollListener(this);
        getChatInfo();
        setOnRefresh();

    }

    private void setOnRefresh() {
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isRefreshed) {
                    mTimeStamp = getTimeStamp(mDataArrayList);
                    mPage = mPage + 1;
                    getChatInfo();
                    if (!Network.isNetworkAvailable() && mSwipeRefreshLayout.isRefreshing()) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                    mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
                    mListView.setStackFromBottom(false);
                } else {
                    ToastUtil.curt("没有更多的数据了");
                    if (mSwipeRefreshLayout.isRefreshing()) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        });
    }

    public void setData(String data) {
        Log.d(TAG, "新数据" + data);
        LiveSpeakInfo liveSpeakInfo = new Gson().fromJson(data, LiveSpeakInfo.class);

        if (liveSpeakInfo != null) {
            if (liveSpeakInfo.isSlience() && liveSpeakInfo.isOwner()) {
                recordUserIsDeny = true;
            } else {
                recordUserIsDeny = false;
            }
            if (!TextUtils.isEmpty(liveSpeakInfo.getMsg())) {
                if (liveSpeakInfo.isOwner() || !liveSpeakInfo.isSlience()) {
                    ChatData chatData = new ChatData(liveSpeakInfo);
                    if (chatData != null && mLiveChatInfoAdapter != null) {
                        if (mHashSet.add(chatData.getCreateTime())) {
//                            if (DateUtil.isTimeMatchFiveMin(DateUtil.format(chatData.getCreateTime()))) {
//                                mTimeMoreThanFiveItemPosition.add(0);
//                                mLiveChatInfoAdapter.setTimeMoreThanFive(true);
//                            }

                            mDataArrayList.add(0, chatData);
                            mLiveChatInfoAdapter.add(chatData);
                            mLiveChatInfoAdapter.notifyDataSetChanged();
                            // TODO: 2016/11/15 自动跑到ListView的最后一个item
                            mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                            mListView.setStackFromBottom(true);
                        }
                    }
                }
            }
        }
    }

    @OnClick({R.id.liveSpeak, R.id.sendSpeak})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.liveSpeak:
                if (LocalUser.getUser().isLogin()) {
                    getLiveMessage();
                } else {
                    Launcher.with(getActivity(), SignInActivity.class).executeForResult(REQUEST_CODE_LOGIN);
                }
                break;
            case R.id.sendSpeak:
                if (mSpeakEditText != null && !TextUtils.isEmpty(mSpeakEditText.getText().toString())) {
                    NettyClient.getInstance().sendMessage(mSpeakEditText.getText().toString());
                    mSpeakEditText.setText("");
                }
                break;
        }
    }

    public void getLiveMessage() {
        API.Live.getLiveMessage().setTag(TAG)
                .setCallback(new Callback<Resp<LiveMessage>>() {
                    @Override
                    public void onReceive(Resp<LiveMessage> liveMessageResp) {
                        if (liveMessageResp.isSuccess() &&
                                liveMessageResp.hasData() &&
                                liveMessageResp.getData().getTeacher() != null &&
                                liveMessageResp.getData().getTeacher().getTeacherAccountId() != 0) {
                            // TODO: 2016/11/15 处理被禁言状态
                            if (!recordUserIsDeny) {
                                sendLiveSpeak();
                            } else {
                                ToastUtil.curt("您被禁言，请稍后发言");
                            }
                        } else {
                            ToastUtil.curt(R.string.live_time_is_not);
                        }
                    }
                }).fire();
    }


    private void sendLiveSpeak() {
        mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (!mSpeakLayout.isShown()) {
            mSpeakLayout.setVisibility(View.VISIBLE);
            mLiveSpeak.setVisibility(View.GONE);
        }
        mSpeakEditText.setFocusable(true);
        mSpeakEditText.setFocusableInTouchMode(true);
        mSpeakEditText.requestFocus();
        mSpeakEditText.setOnKeyListener(this);

        boolean b = mInputMethodManager.isActive(mSpeakEditText);
        if (b) {
            mInputMethodManager.showSoftInput(mSpeakEditText, InputMethodManager.SHOW_FORCED);
        }
        mSpeakEditText.setOnEditorActionListener(mOnEditorActionListener);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            if (mSpeakLayout.isShown()) {
                mSpeakLayout.setVisibility(View.GONE);
                mLiveSpeak.setVisibility(View.VISIBLE);
                return true;
            }
        }
        return false;
    }


    private OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {

                if (mSpeakEditText != null && !TextUtils.isEmpty(mSpeakEditText.getText().toString())) {
                    NettyClient.getInstance().sendMessage(mSpeakEditText.getText().toString());
                }
                if (mInputMethodManager.isActive(mSpeakEditText)) {
                    mInputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                }
                if (mSpeakEditText.isShown()) {
                    mSpeakEditText.setText("");
                    mSpeakLayout.setVisibility(View.GONE);
                }
                if (!mLiveSpeak.isShown()) {
                    mLiveSpeak.setVisibility(View.VISIBLE);
                }
                return true;
            }

            return false;
        }
    };

    private void getChatInfo() {
        API.Live.getLiveTalk(mTimeStamp, mPage, mPageSize)
                .setTag(TAG)
                .setCallback(new Callback<Resp<LiveHomeChatInfo>>() {

                                 @Override
                                 public void onReceive(Resp<LiveHomeChatInfo> liveHomeChatInfoResp) {
                                     if (liveHomeChatInfoResp.isSuccess()) {
                                         if (liveHomeChatInfoResp.hasData()) {

                                             mChatDataListInfo = liveHomeChatInfoResp.getData().getData();

                                             // TODO: 2016/11/15 如果不是本人，则被屏蔽或者被禁言的部分看不到
                                             Iterator<ChatData> iterator = mChatDataListInfo.iterator();
                                             while (iterator.hasNext()) {
                                                 ChatData chatData = iterator.next();
                                                 Log.d(TAG, "下载的数据" + chatData.toString() + "\n");
                                                 if (!chatData.isOwner())
                                                     if (!chatData.isNormalSpeak() || chatData.isDeleted()) {
                                                         iterator.remove();
                                                     }
                                             }

                                             mDataArrayList.addAll(0, mChatDataListInfo);
                                             updateCHatInfo(liveHomeChatInfoResp.getData());
                                         } else {
                                             if (mChatDataListInfo.size() < mPageSize) {
                                                 isRefreshed = true;
                                             }
                                         }
                                     }
                                 }
                             }
                ).fire();
    }

    private long getTimeStamp(List<ChatData> chatDatas) {
        if (chatDatas != null && !chatDatas.isEmpty()) {
            return chatDatas.get(chatDatas.size() - 1).getTimeStamp();
        }
        return 0;
    }

    private void updateCHatInfo(final LiveHomeChatInfo liveHomeChatInfo) {
        if (liveHomeChatInfo == null || liveHomeChatInfo.getData().isEmpty() || liveHomeChatInfo.getData().size() == 0) {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
                return;
            }
            return;
        }
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

        if (mLiveChatInfoAdapter == null) {
            mLiveChatInfoAdapter = new LiveChatInfoAdapter(getActivity());
            mListView.setAdapter(mLiveChatInfoAdapter);
        }

        mLiveChatInfoAdapter.clear();
//        if (mTimeMoreThanFiveItemPosition != null && !mTimeMoreThanFiveItemPosition.isEmpty()) {
//            mLiveChatInfoAdapter.setTimeMoreThanFiveList(mTimeMoreThanFiveItemPosition);
//        }
        if (mDataArrayList != null && !mDataArrayList.isEmpty()) {
            mLiveChatInfoAdapter.addAll(mDataArrayList);
        }
        mLiveChatInfoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {


    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int topRowVerticalPosition =
                (mListView == null || mListView.getChildCount() == 0) ? 0 : mListView.getChildAt(0).getTop();
        mSwipeRefreshLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == getActivity().RESULT_OK) {
            getLiveMessage();
        }
    }


    static class LiveChatInfoAdapter extends ArrayAdapter<ChatData> {

        private Context mContext;
        private boolean isMoreThanFive = false;

        public LiveChatInfoAdapter(Context context) {
            super(context, 0);
            this.mContext = context;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_live_interaction, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.bindViewWithData(getItem(position), mContext, isMoreThanFive, getCount());
            return convertView;
        }

        static class ViewHolder {
            @BindView(R.id.timeBeforeHint)
            TextView mTimeBeforeHint;
            @BindView(R.id.timeBeforeHintLayout)
            LinearLayout mTimeBeforeHintLayout;

            //老师或者管理员的layout
            @BindView(R.id.userStatus)
            TextView mUserStatus;
            @BindView(R.id.timeHint)
            TextView mTimeHint;
            @BindView(R.id.userHeadImage)
            ImageView mUserHeadImage;
            @BindView(R.id.arrow)
            ImageView mArrow;
            @BindView(R.id.content)
            TextView mContent;
            @BindView(R.id.managerLayout)
            RelativeLayout mManagerLayout;

            //自己发言的layout
            @BindView(R.id.userMineStatus)
            TextView mUserMineStatus;
            @BindView(R.id.mineTimeHint)
            TextView mMineTimeHint;
            @BindView(R.id.userMineContent)
            TextView mUserMineContent;
            @BindView(R.id.userMineArrow)
            ImageView mUserMineArrow;
            @BindView(R.id.userMineHeadImage)
            ImageView mUserMineHeadImage;
            @BindView(R.id.userMineLayout)
            RelativeLayout mUserMineLayout;

            //普通游客
            @BindView(R.id.commonUserStatus)
            TextView mCommonUserStatus;
            @BindView(R.id.commonUserTimeHint)
            TextView mCommonUserTimeHint;
            @BindView(R.id.commonUserHeadImage)
            ImageView mCommonUserHeadImage;
            @BindView(R.id.commonUserContent)
            TextView mCommonUserContent;
            @BindView(R.id.commonUserLayout)
            RelativeLayout mCommonUserLayout;


            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            public void bindViewWithData(ChatData item, Context context, boolean isMoreThanFive, int count) {

                //用来记录时间大于5分钟的数据在数组中的索引
//                int dataPosition = 0;
//                for (int i = 0; i < position - 1; i++) {
//                    if (DateUtil.isTimeBetweenFiveMin(chatDataArrayList.get(i + 1).getCreateTime(), chatDataArrayList.get(dataPosition).getCreateTime())) {
//                        mTimeBeforeHintLayout.setVisibility(View.VISIBLE);
//                        mTimeBeforeHint.setText(DateUtil.format(chatDataArrayList.get(i + 1).getCreateTime()));
//                        dataPosition = i + 1;
//                    } else {
//                        mTimeBeforeHintLayout.setVisibility(View.GONE);
//                    }
//                }

                String format = DateUtil.format(item.getCreateTime(), DateUtil.DEFAULT_FORMAT);
                CharSequence relativeTimeSpanString2 = DateUtils.getRelativeTimeSpanString(item.getCreateTime());
                format = relativeTimeSpanString2.toString();
                if (format.equalsIgnoreCase("0分钟前")||format.equalsIgnoreCase("0分钟后")) {
                    format = "刚刚";
                }
                //老师或者管理员
                if (!item.isNormalUser()) {
                    showManagerLayout();
                    setChatUserStatus(item, context);
                    mContent.setText(item.getMsg());
                    mTimeHint.setText(format);
                    //普通游客发言
                } else {
                    //自己发的言
                    if (item.isOwner()) {
                        showUserMineLayout();
                        mMineTimeHint.setText(format);
                        mUserMineStatus.setText(R.string.live_type_mine);
                        mUserMineContent.setText(item.getMsg());
                        //普通游客发言
                    } else {
                        showCommonUserLayout();
                        mCommonUserStatus.setText(item.getName());
                        mCommonUserContent.setText(item.getMsg());
                        mCommonUserTimeHint.setText(format);
                    }
                }
            }

            private void showManagerLayout() {
                if (!mManagerLayout.isShown()) {
                    mManagerLayout.setVisibility(View.VISIBLE);
                    mCommonUserLayout.setVisibility(View.GONE);
                    mUserMineLayout.setVisibility(View.GONE);

                }
            }

            private void showUserMineLayout() {
                if (!mUserMineLayout.isShown()) {
                    mManagerLayout.setVisibility(View.GONE);
                    mCommonUserLayout.setVisibility(View.GONE);
                    mUserMineLayout.setVisibility(View.VISIBLE);
                }
            }

            private void showCommonUserLayout() {
                if (!mCommonUserLayout.isShown()) {
                    mCommonUserLayout.setVisibility(View.VISIBLE);
                    mManagerLayout.setVisibility(View.GONE);
                    mUserMineLayout.setVisibility(View.GONE);
                }
            }

            private void setChatUserStatus(ChatData item, Context context) {
                String chatUser = "";
                if (item.getChatType() == item.CHAT_TYPE_MANAGER) {
                    chatUser = context.getString(R.string.live_type_manager);
                } else if (item.getChatType() == item.CHAT_TYPE_TEACHER) {
                    chatUser = context.getString(R.string.live_type_teacher);
                }
                mUserStatus.setText(chatUser);
            }
        }
    }
}

