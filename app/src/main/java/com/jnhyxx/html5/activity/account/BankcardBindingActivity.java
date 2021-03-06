package com.jnhyxx.html5.activity.account;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jnhyxx.html5.Preference;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.BaseActivity;
import com.jnhyxx.html5.domain.account.ChannelBank;
import com.jnhyxx.html5.domain.account.UserInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Callback1;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.StrFormatter;
import com.jnhyxx.html5.utils.ValidationWatcher;
import com.jnhyxx.html5.utils.ValidityDecideUtil;
import com.jnhyxx.html5.view.CommonFailWarn;
import com.jnhyxx.html5.view.CustomToast;
import com.jnhyxx.html5.view.TitleBar;
import com.jnhyxx.html5.view.WheelView;
import com.jnhyxx.html5.view.dialog.SmartDialog;
import com.johnz.kutils.Launcher;
import com.johnz.kutils.ViewUtil;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.jnhyxx.html5.R.id.identityNum;

public class BankcardBindingActivity extends BaseActivity {

    public static final int REQ_CODE_BIND_BANK = 414;

    @BindView(R.id.bankcardInputArea)
    LinearLayout mBankcardInputArea;
    @BindView(R.id.cardholderName)
    EditText mCardholderName;
    @BindView(R.id.bankcardNum)
    EditText mBankcardNum;
    @BindView(R.id.phoneNum)
    EditText mPhoneNum;
    @BindView(R.id.payingBank)
    TextView mPayingBank;
    @BindView(R.id.bindCardHint)
    ImageView mBindCardHint;
    @BindView(R.id.identityNumTitle)
    TextView mIdentityNumTitle;
    @BindView(identityNum)
    EditText mIdentityNum;

    @BindView(R.id.bankcardImageArea)
    LinearLayout mBankcardImageArea;
    //银行名称
    @BindView(R.id.bankName)
    TextView mBank;
    @BindView(R.id.bankCardNumber)
    TextView mBankCardNumber;
    //解除绑定
    @BindView(R.id.unbindBankcard)
    TextView mUnbindBankcard;
    //所绑定银行卡的父容器,没有绑定之前不显示
    //银行的图标
    @BindView(R.id.bankCardIcon)
    ImageView mBankCardIcon;

    @BindView(R.id.titleBar)
    TitleBar mTitleBar;

    @BindView(R.id.commonFailTvWarn)
    CommonFailWarn mCommonFailTvWarn;

    @BindView(R.id.submitToAuthButton)
    TextView mSubmitToAuthButton;
    @BindView(R.id.cardholderUserName)
    TextView mCardholderUserName;
    @BindView(R.id.cardholderIdentityNum)
    TextView mCardholderIdentityNum;


    private ChannelBank mChannelBank;

    private int mMDefaultSelectBankId = LocalUser.getUser().getUserInfo().getBankId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bankcard_binding);
        ButterKnife.bind(this);

        boolean isFromUserInfoPage = getIntent().getBooleanExtra(Launcher.EX_PAYLOAD, false);
        if (isFromUserInfoPage) {
            mSubmitToAuthButton.setText(R.string.save);
        }

        mPhoneNum.addTextChangedListener(mPhoneValidationWatcher);
        mBankcardNum.addTextChangedListener(mBankCardValidationWatcher);
        mCardholderName.addTextChangedListener(mCardHolderValidationWatcher);
        mIdentityNum.addTextChangedListener(mValidationWatcher);
        mPayingBank.addTextChangedListener(mValidationWatcher);
        showBankcardInfo();

        getUserBindBankInfo();
    }

    private void getUserBindBankInfo() {
        API.User.getUserBankInfo()
                .setTag(TAG)
                .setIndeterminate(this)
                .setCallback(new Callback1<Resp<UserInfo>>() {

                    @Override
                    protected void onRespSuccess(Resp<UserInfo> resp) {
                        updateUserBankInfo(resp);
                        showBankcardInfo();
                    }
                })
                .fireSync();
    }

    private void updateUserBankInfo(Resp<UserInfo> resp) {
        UserInfo webUserBankInfo = resp.getData();
        UserInfo userInfo = LocalUser.getUser().getUserInfo();

        userInfo.setIdStatus(webUserBankInfo.getIdStatus());
        userInfo.setRealName(webUserBankInfo.getRealName());
        userInfo.setCardPhone(webUserBankInfo.getCardPhone());
        userInfo.setAppIcon(webUserBankInfo.getAppIcon());
        userInfo.setIssuingbankName(webUserBankInfo.getIssuingbankName());
        userInfo.setIdCard(webUserBankInfo.getIdCard());
        userInfo.setCardState(webUserBankInfo.getCardState());
        userInfo.setCardNumber(webUserBankInfo.getCardNumber());
        userInfo.setIcon(webUserBankInfo.getIcon());
        userInfo.setAppIcon(webUserBankInfo.getAppIcon());
        userInfo.setBankId(webUserBankInfo.getBankId());
        userInfo.setLimitSingle(webUserBankInfo.getLimitSingle());

        LocalUser.getUser().setUserInfo(userInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBankcardNum.removeTextChangedListener(mBankCardValidationWatcher);
        mPhoneNum.removeTextChangedListener(mPhoneValidationWatcher);
        mCardholderName.removeTextChangedListener(mCardHolderValidationWatcher);
        mIdentityNum.removeTextChangedListener(mValidationWatcher);
        mPayingBank.removeTextChangedListener(mValidationWatcher);
    }

    @Override
    public void onBackPressed() {
        if (!LocalUser.getUser().isBankcardFilled()) {
            SmartDialog.with(getActivity(), R.string.whether_if_give_up_bind_bank_card)
                    .setNegative(R.string.no, new SmartDialog.OnClickListener() {
                        @Override
                        public void onClick(Dialog dialog) {
                            dialog.dismiss();
                        }
                    }).setPositive(R.string.yes, new SmartDialog.OnClickListener() {
                @Override
                public void onClick(Dialog dialog) {
                    dialog.dismiss();
                    finish();
                }
            }).show();
        } else {
            super.onBackPressed();
        }
    }

    private ValidationWatcher mPhoneValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            mValidationWatcher.afterTextChanged(s);
            formatPhoneNumber();
        }
    };

    private ValidationWatcher mCardHolderValidationWatcher = new ValidationWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            mCardholderName.setSelection(s.toString().length());
            mValidationWatcher.afterTextChanged(s);
        }
    };

    private ValidationWatcher mBankCardValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            mValidationWatcher.afterTextChanged(s);
            formatBankCardNumber();
        }
    };

    private ValidationWatcher mValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {
            boolean enable = checkSubmitButtonEnable();
//            if (enable != mSubmitToAuthButton.isEnabled()) {
            mSubmitToAuthButton.setEnabled(enable);
//            }
        }
    };


    private void formatBankCardNumber() {
        String oldBankCard = mBankcardNum.getText().toString();
        String bankCardNoSpace = oldBankCard.replaceAll(" ", "");
        String newBankCard = StrFormatter.getFormatBankCardNumber(bankCardNoSpace).trim();
        if (!newBankCard.equalsIgnoreCase(oldBankCard)) {
            mBankcardNum.setText(newBankCard);
            mBankcardNum.setSelection(newBankCard.length());
        }
    }

    private void formatPhoneNumber() {
        String oldPhone = mPhoneNum.getText().toString();
        String phoneNoSpace = oldPhone.replaceAll(" ", "");
        String newPhone = StrFormatter.getFormatPhoneNumber(phoneNoSpace);
        if (!newPhone.equalsIgnoreCase(oldPhone)) {
            mPhoneNum.setText(newPhone);
            mPhoneNum.setSelection(newPhone.length());
        }
    }

    /**
     * 这是显示银行卡是否绑定的方法，
     * 如果没有绑定，显示绑定银行卡界面，
     * 若果绑定了，显示银行卡信息
     */
    private void showBankcardInfo() {
        if (LocalUser.getUser().isBankcardBound() && LocalUser.getUser().isRealNameBound()) {
            mTitleBar.setTitle(R.string.bankcard);
            mBankcardInputArea.setVisibility(View.GONE);
            mBankcardImageArea.setVisibility(View.VISIBLE);

            UserInfo userInfo = LocalUser.getUser().getUserInfo();

            mCardholderUserName.setText(userInfo.getRealName());
            mCardholderIdentityNum.setText(userInfo.getIdCard());

            if (!TextUtils.isEmpty(userInfo.getIssuingbankName())) {
                mBank.setText(getString(R.string.bind_bank_card_name, userInfo.getIssuingbankName()));
            }

            String bankIconUrl = userInfo.getIcon();
            if (!TextUtils.isEmpty(bankIconUrl)) {
                Picasso.with(BankcardBindingActivity.this).load(bankIconUrl).into(mBankCardIcon);
            }

            if (!TextUtils.isEmpty(userInfo.getCardNumber())) {
                String bankNumber = StrFormatter.getHintFormatBankCardNumber(userInfo.getCardNumber());
                mBankCardNumber.setText(bankNumber);
            }
        } else {
            mBankcardInputArea.setVisibility(View.VISIBLE);
            mBankcardImageArea.setVisibility(View.GONE);
            setOldBindBankInfo(LocalUser.getUser().getUserInfo());
        }
    }

    private void setOldBindBankInfo(UserInfo userInfo) {
        mCardholderName.setText(userInfo.getRealName());
        mIdentityNum.setText(userInfo.getIdCard());
        mBankcardNum.setText(userInfo.getCardNumber());
        mPhoneNum.setText(userInfo.getCardPhone());
        mPayingBank.setText(userInfo.getIssuingbankName());

        if (LocalUser.getUser().isRealNameBound()) {
            mCardholderName.setEnabled(false);
            mIdentityNum.setEnabled(false);
        } else {
            mCardholderName.setEnabled(true);
            mIdentityNum.setEnabled(true);
        }

        if (LocalUser.getUser().isBankcardBound()) {
            mBankcardNum.setEnabled(false);
            mPhoneNum.setEnabled(false);
            mPayingBank.setEnabled(false);
        } else {
            mBankcardNum.setEnabled(true);
            mPhoneNum.setEnabled(true);
            mPayingBank.setEnabled(true);
        }
    }

    private boolean checkSubmitButtonEnable() {
        String cardholderName = ViewUtil.getTextTrim(mCardholderName);
        String bankcardNum = ViewUtil.getTextTrim(mBankcardNum);
        String payingBank = ViewUtil.getTextTrim(mPayingBank);
        String phoneNum = ViewUtil.getTextTrim(mPhoneNum);
        String identityNum = ViewUtil.getTextTrim(mIdentityNum);
        if (TextUtils.isEmpty(cardholderName) || TextUtils.isEmpty(bankcardNum)
                || TextUtils.isEmpty(payingBank) || TextUtils.isEmpty(phoneNum)
                || TextUtils.isEmpty(identityNum)) {
            return false;
        }


        UserInfo userInfo = LocalUser.getUser().getUserInfo();
        if (cardholderName.equalsIgnoreCase(userInfo.getRealName())
                & bankcardNum.replaceAll(" ", "").equalsIgnoreCase(userInfo.getCardNumber())
                & payingBank.equalsIgnoreCase(userInfo.getIssuingbankName())
                & phoneNum.replaceAll(" ", "").equalsIgnoreCase(userInfo.getCardPhone())
                & identityNum.equalsIgnoreCase(userInfo.getIdCard())) {
            return false;
        }

        return true;
    }

    @OnClick({R.id.payingBank, R.id.submitToAuthButton, R.id.unbindBankcard, R.id.bindCardHint})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.payingBank:
                showBankDialog();
                break;
            case R.id.submitToAuthButton:
                final String cardHolderName = ViewUtil.getTextTrim(mCardholderName);
                final String bankcardNum = ViewUtil.getTextTrim(mBankcardNum).replaceAll(" ", "");
                final String payingBank = ViewUtil.getTextTrim(mPayingBank);
                final String phoneNum = ViewUtil.getTextTrim(mPhoneNum).replaceAll(" ", "");
                final String identityNum = ViewUtil.getTextTrim(mIdentityNum);

                /**
                 * 如果用户已经实名认证，则不检验
                 */
                if (!LocalUser.getUser().getUserInfo().isUserRealNameAuth()) {
                    if (!ValidityDecideUtil.isOnlyAChineseName(cardHolderName)) {
                        mCommonFailTvWarn.show(R.string.is_only_a_chinese_name);
                        return;
                    }
                }
                if (!TextUtils.isEmpty(payingBank) && TextUtils.equals(payingBank, getString(R.string.please_choose_bank))) {
                    mCommonFailTvWarn.show(R.string.bind_bank_is_empty);
                    return;
                }
                submitBindBank(cardHolderName, bankcardNum, payingBank, phoneNum, identityNum);
                break;
            case R.id.unbindBankcard:
                unbindServiceTelephone();
                break;
            case R.id.bindCardHint:
                showCardHolderDialog();
                break;
        }
    }

    private void updateUserBindBankUserNameVerify(String identityNum, String payingBank, String bankcardNum, String phoneNum, int bankId) {
        final String cardHolderName = ViewUtil.getTextTrim(mCardholderName);
        LocalUser localUser = LocalUser.getUser();
        UserInfo userInfo = localUser.getUserInfo();
        userInfo.setIssuingbankName(payingBank);
        userInfo.setCardNumber(bankcardNum);
        userInfo.setCardPhone(phoneNum);
        userInfo.setBankId(bankId);
        userInfo.setCardState(UserInfo.BANKCARD_STATUS_FILLED);
        userInfo.setIdCard(identityNum);
        if (!LocalUser.getUser().isRealNameBound()) {
            userInfo.setIdStatus(UserInfo.REAL_NAME_STATUS_FILLED);
        }
        if (!LocalUser.getUser().isRealNameBound()) {
            userInfo.setRealName(cardHolderName);
        }
        localUser.setUserInfo(userInfo);
    }

    private void submitBindBank(String cardHolderName, final String bankcardNum, final String payingBank, final String phoneNum, final String identityNum) {
        final int bankId = mChannelBank != null ? mChannelBank.getId() : LocalUser.getUser().getUserInfo().getBankId();
        cardHolderName = LocalUser.getUser().getUserInfo().isUserRealNameAuth() ? null : cardHolderName;
        API.User.bindBankCard(cardHolderName, identityNum, bankId, payingBank, bankcardNum, phoneNum)
                .setIndeterminate(this).setTag(TAG)
                .setCallback(new Callback<Resp>() {
                    @Override
                    public void onReceive(Resp resp) {
                        if (resp.isSuccess()) {
                            updateUserBindBankUserNameVerify(identityNum, payingBank, bankcardNum, phoneNum, bankId);
                            CustomToast.getInstance().showText(getActivity(), resp.getMsg());

                            setResult(RESULT_OK);
                            finish();
                        } else {
                            mCommonFailTvWarn.show(resp.getMsg());
                        }
                    }
                }).fire();
    }

    private void showCardHolderDialog() {
        SmartDialog.with(getActivity(), R.string.bind_bank_hint)
                .setPositive(R.string.i_get_it, new SmartDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog) {
                        dialog.dismiss();
                    }
                })
                .setSingleButtonBg(R.drawable.btn_blue)
                .setMessageGravity(Gravity.LEFT)
                .setMessageMaxLines(Integer.MAX_VALUE)
                .show();
    }

    private void showBankDialog() {
        getChannelBankList();
    }

    private void getChannelBankList() {
        API.User.showChannelBankList()
                .setTag(TAG)
                .setIndeterminate(this)
                .setCallback(new Callback<Resp<List<ChannelBank>>>() {
                    @Override
                    public void onReceive(Resp<List<ChannelBank>> listResp) {
                        if (listResp.isSuccess()) {
                            if (listResp.getData() != null && !listResp.getData().isEmpty()) {
                                View view = setWheelView(listResp.getData());
                                setChannelBank(listResp.getData());
                                setSelectBankDialog(view);
                            } else {
                                mCommonFailTvWarn.show(R.string.no_bank_can_bind);
                            }
                        } else {
                            mCommonFailTvWarn.show(listResp.getMsg());
                        }
                    }
                }).fire();
    }

    private void setChannelBank(List<ChannelBank> data) {
        int bankId = LocalUser.getUser().getUserInfo().getBankId();
        for (ChannelBank bank : data) {
            if (bank.getId() == bankId) {
                mChannelBank = bank;
                break;
            }
        }
    }

    private void setSelectBankDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(BankcardBindingActivity.this)
                .setView(view)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mChannelBank != null) {
                            mPayingBank.setText(mChannelBank.getName());
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mChannelBank = null;
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    @NonNull
    private View setWheelView(final List<ChannelBank> channelBanks) {

        View view = LayoutInflater.from(BankcardBindingActivity.this).inflate(R.layout.dialog_wheel_view, null);
        final WheelView mWheelView = (WheelView) view.findViewById(R.id.wheelView);
        mWheelView.setOffset(1);
        if (!LocalUser.getUser().isBankcardBound()) {
            for (int i = 0; i < channelBanks.size(); i++) {
                if (mMDefaultSelectBankId == channelBanks.get(i).getId()) {
                    mMDefaultSelectBankId = i;
                    break;
                }
            }

        }
        List<String> bankNameList = new ArrayList<>();
        for (ChannelBank data : channelBanks) {
            bankNameList.add(data.getName());
        }

        mWheelView.setItems(bankNameList);
        mWheelView.setSelectedIndex(mMDefaultSelectBankId);
        mWheelView.setOnWheelListener(new WheelView.OnWheelListener() {
            @Override
            public void onSelected(boolean isUserScroll, int index, String item) {
                for (int i = 0; i < channelBanks.size(); i++) {
                    if (item.equalsIgnoreCase(channelBanks.get(i).getName())) {
                        mMDefaultSelectBankId = channelBanks.get(i).getId();
                        mChannelBank = channelBanks.get(i);
                        break;
                    }
                }
            }
        });
        return view;
    }


    private void unbindServiceTelephone() {
        String dialogContent = getString(R.string.unBind_dialog_content, Preference.get().getServicePhone());
        SmartDialog.with(getActivity(), dialogContent)
                .setCancelableOnTouchOutside(false)
                .setPositive(R.string.ok, new SmartDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog) {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Preference.get().getServicePhone()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .setNegative(R.string.cancel, new SmartDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}