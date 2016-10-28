package com.jnhyxx.html5.fragment;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.account.AboutUsActivity;
import com.jnhyxx.html5.activity.account.MessageCenterActivity;
import com.jnhyxx.html5.activity.account.RechargeActivity;
import com.jnhyxx.html5.activity.account.SignInActivity;
import com.jnhyxx.html5.activity.account.SignUpActivity;
import com.jnhyxx.html5.activity.account.TradeDetailActivity;
import com.jnhyxx.html5.activity.account.WithdrawActivity;
import com.jnhyxx.html5.activity.setting.SettingsActivity;
import com.jnhyxx.html5.activity.web.PaidToPromoteActivity;
import com.jnhyxx.html5.domain.account.UserFundInfo;
import com.jnhyxx.html5.domain.account.UserInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Callback1;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.ToastUtil;
import com.jnhyxx.html5.view.CircularAnnulusImageView;
import com.jnhyxx.html5.view.IconTextRow;
import com.jnhyxx.html5.view.TitleBar;
import com.jnhyxx.html5.view.dialog.SmartDialog;
import com.johnz.kutils.FinanceUtil;
import com.johnz.kutils.Launcher;
import com.johnz.kutils.net.CookieManger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import me.grantland.widget.AutofitTextView;

import static com.jnhyxx.html5.R.id.paidToPromote;

public class MineFragment extends BaseFragment {

    //账户余额
    @BindView(R.id.balance)
    AutofitTextView mBalance;
    //我的积分
    @BindView(R.id.score)
    TextView mScore;
    //登陆
    @BindView(R.id.signInButton)
    TextView mSignIn;
    //注册
    @BindView(R.id.signUpButton)
    TextView mSignUp;
    //消息中心
    @BindView(R.id.messageCenter)
    IconTextRow mMessageCenter;

    //交易明细
    @BindView(R.id.tradeDetail)
    IconTextRow mTradeDetail;
    @BindView(R.id.aboutUs)
    IconTextRow mAboutUs;
    @BindView(paidToPromote)
    IconTextRow mPaidToPromote;
    @BindView(R.id.nickname)
    TextView mNickname;

    //登陆和注册按钮的父容器
    @BindView(R.id.signArea)
    LinearLayout mSignArea;
    @BindView(R.id.recharge)
    TextView mRecharge;
    @BindView(R.id.withdraw)
    TextView mWithdraw;

    //充值和提现按钮的父容器
    @BindView(R.id.fundArea)
    LinearLayout mFundArea;
    @BindView(R.id.titleBar)
    TitleBar mTitleBar;
    @BindView(R.id.headImage)
    CircularAnnulusImageView mHeadImage;

    private Unbinder mBinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        mBinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mTitleBar.setOnRightViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Launcher.with(getActivity(), SettingsActivity.class).execute();
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isAdded()) {
            requestUserInfo();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinder.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccountInfoView();
        requestUserInfo();
    }

    private void requestUserInfo() {
        updateUsableMoneyScore(new LocalUser.Callback() {
            @Override
            public void onUpdateCompleted() {
                updateAccountInfoView();
            }
        });
    }

    private void updateAccountInfoView() {
        if (LocalUser.getUser().isLogin()) {
            mSignArea.setVisibility(View.GONE);
            mFundArea.setVisibility(View.VISIBLE);
            mTitleBar.setRightVisible(true);

            UserInfo userInfo = LocalUser.getUser().getUserInfo();

            String userName = userInfo.getUserName();
            double moneyUsable = userInfo.getMoneyUsable();
            double scoreUsable = userInfo.getScoreUsable();
            mNickname.setText(getString(R.string.nickname_logged, userName));
            mBalance.setText(FinanceUtil.formatWithScale(moneyUsable));
            mScore.setText(getString(R.string.mine_score, FinanceUtil.formatWithScale(scoreUsable)));

        } else {
            mSignArea.setVisibility(View.VISIBLE);
            mFundArea.setVisibility(View.GONE);

            mNickname.setText(R.string.no_logged);
            mTitleBar.setRightVisible(false);
            mBalance.setText(R.string.zero);
            mScore.setText(getString(R.string.mine_score, getString(R.string.zero)));
        }
    }

    @OnClick({R.id.signInButton, R.id.signUpButton, R.id.recharge, R.id.withdraw, R.id.messageCenter, R.id.tradeDetail, R.id.aboutUs, R.id.paidToPromote, R.id.headImage})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.signInButton:
                Launcher.with(getActivity(), SignInActivity.class).execute();
                break;
            case R.id.signUpButton:
                Launcher.with(getActivity(), SignUpActivity.class).execute();
                break;
            case R.id.recharge: //充值
                Launcher.with(getActivity(), RechargeActivity.class).execute();
                break;
            case R.id.withdraw: //提现
                Launcher.with(getActivity(), WithdrawActivity.class).execute();
                break;
            case R.id.messageCenter:
                Launcher.with(getActivity(), MessageCenterActivity.class).execute();
                break;
            case R.id.tradeDetail:
                openTradeDetailPage();
                break;
            case R.id.aboutUs:
                Launcher.with(getActivity(), AboutUsActivity.class).execute();
                break;
            case R.id.paidToPromote:
                openPaidToPromotePage();
                break;
            case R.id.headImage:
                if (LocalUser.getUser().isLogin()) {
                    Launcher.with(getActivity(), SettingsActivity.class).execute();
                } else {
                    Launcher.with(getActivity(), SignInActivity.class).execute();
                }
                break;
        }
    }

    private void openPaidToPromotePage() {
        if (LocalUser.getUser().isLogin()) {
            API.User.getPromoteCode().setTag(TAG).setIndeterminate(this)
                    .setCallback(new Callback<Resp<JsonObject>>() {
                        @Override
                        public void onReceive(Resp<JsonObject> resp) {
                            if (resp.isSuccess()) {
                                Launcher.with(getActivity(), PaidToPromoteActivity.class)
                                        .putExtra(PaidToPromoteActivity.EX_URL, API.getPromotePage())
                                        .putExtra(PaidToPromoteActivity.EX_TITLE, getString(R.string.paid_to_promote))
                                        .putExtra(PaidToPromoteActivity.EX_RAW_COOKIE, CookieManger.getInstance().getRawCookie())
                                        .execute();
                            } else if (resp.getCode() == Resp.CODE_GET_PROMOTE_CODE_FAILED) {
                                showAskApplyPromoterDialog();
                            } else {
                                ToastUtil.show(resp.getMsg());
                            }
                        }
                    }).fire();
        } else {
            Launcher.with(getActivity(), SignInActivity.class).execute();
        }
    }

    private void showAskApplyPromoterDialog() {
        SmartDialog.with(getActivity(), R.string.dialog_you_are_not_promoter_yet)
                .setPositive(R.string.ok, new SmartDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog) {
                        dialog.dismiss();
                        applyForPromoter();
                    }
                })
                .setNegative(R.string.cancel)
                .show();
    }

    private void applyForPromoter() {
        API.User.becomePromoter().setTag(TAG)
                .setCallback(new Callback1<Resp<JsonObject>>() {
                    @Override
                    protected void onRespSuccess(Resp<JsonObject> resp) {
                        if (resp.isSuccess()) {
                            ToastUtil.show(resp.getMsg());
                            Launcher.with(getActivity(), PaidToPromoteActivity.class)
                                    .putExtra(PaidToPromoteActivity.EX_URL, API.getPromotePage())
                                    .putExtra(PaidToPromoteActivity.EX_TITLE, getString(R.string.paid_to_promote))
                                    .putExtra(PaidToPromoteActivity.EX_RAW_COOKIE, CookieManger.getInstance().getRawCookie())
                                    .execute();
                        }
                    }
                }).fire();
    }

    private void openTradeDetailPage() {
        if (LocalUser.getUser().isLogin()) {
            API.Finance.getFundInfo().setTag(TAG)
                    .setIndeterminate(this)
                    .setCallback(new Callback1<Resp<UserFundInfo>>() {
                        @Override
                        protected void onRespSuccess(Resp<UserFundInfo> resp) {
                            UserFundInfo userFundInfo = resp.getData();
                            Launcher.with(getActivity(), TradeDetailActivity.class)
                                    .putExtra(Launcher.EX_PAYLOAD, userFundInfo).execute();
                        }
                    }).fire();
        } else {
            Launcher.with(getActivity(), SignInActivity.class).execute();
        }
    }
}
