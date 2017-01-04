package com.jnhyxx.html5.fragment.live;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.format.DateUtils;
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

import com.android.volley.VolleyError;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.domain.live.LiveHomeChatInfo;
import com.jnhyxx.html5.domain.live.LiveMessage;
import com.jnhyxx.html5.domain.live.LiveSpeakInfo;
import com.jnhyxx.html5.fragment.BaseFragment;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.ToastUtil;
import com.jnhyxx.html5.utils.UmengCountEventIdUtils;
import com.jnhyxx.html5.utils.ValidationWatcher;
import com.jnhyxx.html5.utils.transform.CircleTransform;
import com.johnz.kutils.DateUtil;
import com.johnz.kutils.ViewUtil;
import com.squareup.picasso.Picasso;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


/**
 * Created by ${wangJie} on 2016/11/8.
 * 直播互动界面
 */

public class LiveInteractionFragment extends BaseFragment implements AbsListView.OnScrollListener {

    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.listView)
    ListView mListView;
    @BindView(R.id.empty)
    TextView mEmpty;

    @BindView(R.id.inputBox)
    EditText mInputBox;
    @BindView(R.id.sendButton)
    TextView mSendButton;
    @BindView(R.id.inputBoxArea)
    LinearLayout mInputBoxArea;

    private Unbinder mBind;

    private int mPageOffset = 0;
    private int mPageSize = 0;

    private LiveChatInfoAdapter mLiveChatInfoAdapter;

    private HashSet<Long> mHashSet;

    private List<LiveHomeChatInfo> mLiveHomeChatInfoListInfo;

    private ArrayList<LiveHomeChatInfo> mDataArrayList;

    private LiveMessage.TeacherInfo mTeacherInfo;

    private boolean mIsKeyboardOpened;

    private InputMethodManager mInputMethodManager;

    private OnSendButtonClickListener mOnSendButtonClickListener;

    public interface OnSendButtonClickListener {
        void onSendButtonClick(String message);
    }

    public static LiveInteractionFragment newInstance() {
        Bundle args = new Bundle();
        LiveInteractionFragment fragment = new LiveInteractionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LiveInteractionFragment.OnSendButtonClickListener) {
            mOnSendButtonClickListener = (OnSendButtonClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement LiveInteractionFragment.Callback");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interaction, container, false);
        mBind = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mInputBox.removeTextChangedListener(mValidationWatcher);
        mBind.unbind();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mListView.setEmptyView(mEmpty);
        if (mLiveChatInfoAdapter == null) {
            mLiveChatInfoAdapter = new LiveChatInfoAdapter(getActivity());
            mListView.setAdapter(mLiveChatInfoAdapter);
        }


        mListView.setOnScrollListener(this);

        mInputBox.addTextChangedListener(mValidationWatcher);

        mPageSize = 10;
        mPageOffset = 0;
        mHashSet = new HashSet<>();
        mDataArrayList = new ArrayList<>();

        getChatInfo();
        setOnRefresh();
        setLiveViewStackFromBottom(true);
    }

    private void setLiveViewStackFromBottom(boolean isStackFromBottom) {
        mListView.setStackFromBottom(isStackFromBottom);
        if (isStackFromBottom) {
            mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        } else {
            mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mPageOffset = 0;
    }

    private ValidationWatcher mValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {
            boolean enable = checkSendButtonEnable();
            if (enable != mSendButton.isEnabled()) {
                mSendButton.setEnabled(enable);
            }
        }
    };

    private boolean checkSendButtonEnable() {
        String content = ViewUtil.getTextTrim(mInputBox);
        if (!TextUtils.isEmpty(content)) {
            return true;
        }
        return false;
    }

    public void showInputBox() {
        mInputBoxArea.setVisibility(View.VISIBLE);
        mInputBox.requestFocus();
        mInputMethodManager.showSoftInput(mInputBox, InputMethodManager.SHOW_FORCED);
        mInputBoxArea.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    hideInputBox();
                    return true;
                }
                return false;
            }
        });
    }

    public void hideInputBox() {
        mInputBoxArea.setVisibility(View.GONE);
        mInputMethodManager.hideSoftInputFromWindow(mInputBox.getWindowToken(), 0);
    }

    public void setKeyboardOpened(boolean keyboardOpened) {
        mIsKeyboardOpened = keyboardOpened;
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
                getChatInfo();
                setLiveViewStackFromBottom(false);
            }
        });
    }

    public void setTeacherInfo(LiveMessage.TeacherInfo teacherInfo) {
        mTeacherInfo = teacherInfo;
    }

    public void setData(LiveSpeakInfo liveSpeakInfo) {
        if (liveSpeakInfo != null) {
            mPageOffset++;
            if (liveSpeakInfo.isOwner() && mDataArrayList.size() > 5) {
                setLiveViewStackFromBottom(true);
            }
            if (liveSpeakInfo.isSlience() && liveSpeakInfo.isOwner()) {
                ToastUtil.curt("您被禁言，请稍后发言");
            }
            updateTalkData(liveSpeakInfo);
        }
    }

    //更新接到的最新聊天数据
    private void updateTalkData(LiveSpeakInfo liveSpeakInfo) {
        if (!TextUtils.isEmpty(liveSpeakInfo.getMsg())) {
            LiveHomeChatInfo LiveHomeChatInfo = new LiveHomeChatInfo(liveSpeakInfo);
            if (mLiveChatInfoAdapter != null) {
                if (mHashSet.add(LiveHomeChatInfo.getCreateTime())) {
                    mDataArrayList.add(LiveHomeChatInfo);
                    if (mDataArrayList.size() > 2) {
                        if (DateUtil.isTimeBetweenFiveMin(LiveHomeChatInfo.getCreateTime(), mDataArrayList.get(mDataArrayList.size() - 2).getCreateTime())) {
                            LiveHomeChatInfo.setMoreThanFiveMin(true);
                        }
                    }
                    mLiveChatInfoAdapter.add(LiveHomeChatInfo);
                    mLiveChatInfoAdapter.notifyDataSetChanged();
                }
            }
        }
    }


    private void getChatInfo() {
        API.Live.getLiveTalk(mPageOffset, mPageSize)
                .setTag(TAG)
                .setCallback(new Callback<Resp<List<LiveHomeChatInfo>>>() {
                                 @Override
                                 public void onReceive(Resp<List<LiveHomeChatInfo>> liveHomeChatInfoResp) {
                                     if (liveHomeChatInfoResp.isSuccess()) {
                                         if (liveHomeChatInfoResp.hasData()) {

                                             if (liveHomeChatInfoResp.getData().size() < 5 || liveHomeChatInfoResp.getData().size() < mListView.getChildCount()) {
                                                 setLiveViewStackFromBottom(false);
                                             }
                                             mPageOffset = mPageOffset + mPageSize;
                                             mLiveHomeChatInfoListInfo = liveHomeChatInfoResp.getData();
                                             mDataArrayList.addAll(0, mLiveHomeChatInfoListInfo);
                                             updateCHatInfo(mDataArrayList);
                                             if (mPageOffset > 10) {
                                                 mListView.setSelection(mPageSize - 1);
                                             }
                                         } else {
                                             stopRefreshAnimation();
                                         }
                                     } else {
                                         stopRefreshAnimation();
                                     }
                                 }

                                 @Override
                                 public void onFailure(VolleyError volleyError) {
                                     super.onFailure(volleyError);
                                     stopRefreshAnimation();
                                 }
                             }
                ).fire();
    }

    private void stopRefreshAnimation() {
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    //登录成功后需要清空数据，重新获取状态
    public void setLoginSuccess(boolean isLogin) {
        if (isLogin) {
            if (mLiveChatInfoAdapter != null) {
                mLiveChatInfoAdapter.clear();
            }
            if (mDataArrayList != null && !mDataArrayList.isEmpty()) {
                mDataArrayList.clear();
            }
            getChatInfo();
        }
    }

    private void updateCHatInfo(final List<LiveHomeChatInfo> liveHomeChatInfoList) {
        if (liveHomeChatInfoList == null || liveHomeChatInfoList.isEmpty()) {
            stopRefreshAnimation();
            return;
        }
        stopRefreshAnimation();

        if (mTeacherInfo != null) {
            mLiveChatInfoAdapter.setTeacher(mTeacherInfo);
        }

        mLiveChatInfoAdapter.clear();
        getTalkTimeIsThanFiveMinute();
        mLiveChatInfoAdapter.notifyDataSetChanged();
    }

    //判断谈话时间是否超过5分钟，如果超过，出现分割线
    private void getTalkTimeIsThanFiveMinute() {
        if (mDataArrayList != null && !mDataArrayList.isEmpty()) {
            for (int i = mDataArrayList.size(); i > 0; i--) {
                if (i < 3) break;
                if (DateUtil.isTimeBetweenFiveMin(mDataArrayList.get(i - 1).getCreateTime(), mDataArrayList.get(i - 2).getCreateTime())) {
                    mDataArrayList.get(i - 1).setMoreThanFiveMin(true);
                }
            }
            mLiveChatInfoAdapter.addAll(mDataArrayList);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            if (mIsKeyboardOpened) {
                hideInputBox();
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int topRowVerticalPosition =
                (mListView == null || mListView.getChildCount() == 0) ? 0 : mListView.getChildAt(0).getTop();
        mSwipeRefreshLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
    }

    @OnClick(R.id.sendButton)
    public void onClick() {
        MobclickAgent.onEvent(getActivity(), UmengCountEventIdUtils.SEND_SPEAK);
        if (mOnSendButtonClickListener != null) {
            String message = ViewUtil.getTextTrim(mInputBox);
            mOnSendButtonClickListener.onSendButtonClick(message);
        }
        mInputBox.setText("");
    }

    static class LiveChatInfoAdapter extends ArrayAdapter<LiveHomeChatInfo> {
        private Context mContext;
        private LiveMessage.TeacherInfo mTeacherInfo;

        public LiveChatInfoAdapter(Context context) {
            super(context, 0);
            this.mContext = context;
        }

        public void setTeacher(LiveMessage.TeacherInfo teacher) {
            mTeacherInfo = teacher;
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
            viewHolder.bindViewWithData(getItem(position), mContext, mTeacherInfo);
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
            @BindView(R.id.userHeadImage)
            ImageView mUserHeadImage;
            @BindView(R.id.arrow)
            ImageView mArrow;
            @BindView(R.id.content)
            TextView mContent;
            @BindView(R.id.managerLayout)
            RelativeLayout mManagerLayout;
            @BindView(R.id.ivTeacherImage)
            ImageView mIvTeacherImage;
            @BindView(R.id.llImageLayout)
            LinearLayout mLlImageLayout;

            //自己发言的layout
            @BindView(R.id.userMineStatus)
            TextView mUserMineStatus;
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
            @BindView(R.id.commonUserHeadImage)
            ImageView mCommonUserHeadImage;
            @BindView(R.id.commonUserContent)
            TextView mCommonUserContent;
            @BindView(R.id.commonUserLayout)
            RelativeLayout mCommonUserLayout;


            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            public void bindViewWithData(LiveHomeChatInfo item, Context context, LiveMessage.TeacherInfo teacherInfo) {

                String formatTime = DateUtil.getFormatTime(item.getCreateTime());

                String format = DateUtil.format(item.getCreateTime(), DateUtil.DEFAULT_FORMAT);
                CharSequence relativeTimeSpanString2 = DateUtils.getRelativeTimeSpanString(item.getCreateTime());
                format = format + "  " + relativeTimeSpanString2.toString();
                if (format.equalsIgnoreCase("0分钟前") || format.equalsIgnoreCase("0分钟后")) {
                    format = "刚刚";
                }

                if (item.isMoreThanFiveMin()) {
                    mTimeBeforeHintLayout.setVisibility(View.VISIBLE);
                    mTimeBeforeHint.setText(formatTime);
                } else {
                    mTimeBeforeHintLayout.setVisibility(View.GONE);
                }

                //老师或者管理员
                if (!item.isNormalUser()) {
                    showManagerLayout();
                    setChatUserStatus(item, context, teacherInfo, format);
                    setTeacherMsg(item, context);
                    //普通游客发言
                } else {
                    //自己发的言
                    if (item.isOwner()) {
                        showUserMineLayout();
                        mUserMineStatus.setText(R.string.live_type_mine);
                        mUserMineContent.setText(item.getMsg());
                        //普通游客发言
                    } else {
                        showCommonUserLayout();
                        mCommonUserStatus.setText(item.getName());
                        mCommonUserContent.setText(item.getMsg());
                    }
                }
            }

            private void setTeacherMsg(LiveHomeChatInfo item, Context context) {
                if (item.isText()) {
                    if (mContent.getVisibility() == View.GONE) {
                        mContent.setVisibility(View.VISIBLE);
                    }
                    if (mLlImageLayout.getVisibility() == View.VISIBLE)
                        mLlImageLayout.setVisibility(View.GONE);
                    mContent.setText(item.getMsg());

                } else {
                    if (!TextUtils.isEmpty(item.getMsg())) {
                        if (mContent.getVisibility() == View.VISIBLE || mLlImageLayout.getVisibility() == View.GONE) {
                            mContent.setVisibility(View.GONE);
                            mLlImageLayout.setVisibility(View.VISIBLE);
                        }
                        Picasso.with(context).load(item.getMsg()).into(mIvTeacherImage);
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

            private void setChatUserStatus(LiveHomeChatInfo item, Context context, LiveMessage.TeacherInfo teacherInfo, String format) {
                String chatUser = "";
                if (item.getChatType() == LiveHomeChatInfo.CHAT_TYPE_MANAGER) {
                    chatUser = context.getString(R.string.live_type_manager);
                    Picasso.with(context).load(R.drawable.ic_live_pic_head)
                            .transform(new CircleTransform()).into(mUserHeadImage);
                } else if (item.getChatType() == LiveHomeChatInfo.CHAT_TYPE_TEACHER) {
                    chatUser = item.getName();
                    if (teacherInfo != null && !TextUtils.isEmpty(teacherInfo.getPictureUrl())) {
                        Picasso.with(context).load(teacherInfo.getPictureUrl())
                                .transform(new CircleTransform()).into(mUserHeadImage);
                    } else {
                        Picasso.with(context).load(R.drawable.ic_live_pic_head)
                                .transform(new CircleTransform()).into(mUserHeadImage);
                    }
                }
                mUserStatus.setText(chatUser);
            }
        }
    }
}

