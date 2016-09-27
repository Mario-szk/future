package com.jnhyxx.html5.activity.account;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.BaseActivity;
import com.jnhyxx.html5.domain.NameAuth;
import com.jnhyxx.html5.domain.account.UserInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.ToastUtil;
import com.jnhyxx.html5.utils.ValidationWatcher;
import com.jnhyxx.html5.view.CommonFailWarn;
import com.johnz.kutils.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NameAuthActivity extends BaseActivity {

    @BindView(R.id.name)
    EditText mName;
    @BindView(R.id.identityNum)
    EditText mIdentityNum;
    @BindView(R.id.submitToAuthButton)
    TextView mSubmitToAuthButton;
    @BindView(R.id.identityCardWarn)
    CommonFailWarn mRlIdentityCardWarn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_auth);
        ButterKnife.bind(this);

        mName.addTextChangedListener(mValidationWatcher);
        mIdentityNum.addTextChangedListener(mValidationWatcher);

        updateNameAuthView();
    }

    private void updateNameAuthView() {
        UserInfo userInfo = LocalUser.getUser().getUserInfo();
        if (userInfo != null) {
            mName.setText(userInfo.getRealName());
            mIdentityNum.setText(userInfo.getIdCard());

            if (userInfo.getIdStatus() == UserInfo.REAL_NAME_AUTH_STATUS_ATTESTATION) {
                mName.setEnabled(false);
                mIdentityNum.setEnabled(false);
                mSubmitToAuthButton.setVisibility(View.GONE);
            } else {
                mName.setEnabled(true);
                mIdentityNum.setEnabled(true);
                mSubmitToAuthButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private ValidationWatcher mValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {
            boolean enable = checkSubmitButtonEnable();
            if (enable != mSubmitToAuthButton.isEnabled()) {
                mSubmitToAuthButton.setEnabled(enable);
            }
        }
    };

    private boolean checkSubmitButtonEnable() {
        String realName = ViewUtil.getTextTrim(mName);
        String identityNum = ViewUtil.getTextTrim(mIdentityNum);
        if (TextUtils.isEmpty(realName) || TextUtils.isEmpty(identityNum)) {
            return false;
        }
        return true;
    }

    @OnClick(R.id.submitToAuthButton)
    public void onClick() {
        final String realName = mName.getText().toString().trim();
        final String identityNum = mIdentityNum.getText().toString().trim();
        if (LocalUser.getUser().isLogin()) {
            // TODO: 2016/9/13 目前这个判断有问题，无法输入X
//            boolean cardValidate = CommonMethodUtils.IDCardValidate(identityNum);
//            if (cardValidate) {
                API.User.authUserName(realName, identityNum)
                        .setTag(TAG)
                        .setIndeterminate(this)
                        .setCallback(new Callback<Resp>() {
                            @Override
                            public void onReceive(Resp resp) {
                                if (resp.isSuccess()) {
                                    //将是否实名认证状态修改
                                    UserInfo user = LocalUser.getUser().getUserInfo();
                                    user.setRealName(realName);
                                    user.setIdCard(identityNum);
                                    user.setIdStatus(1);
                                    setResult(RESULT_OK);
                                    Log.d(TAG,"实名认证后的用户信息"+user.toString());

                                    // TODO: 2016/9/13 原来的逻辑
//                                        SmartDialog.with(getActivity(), resp.getMsg())
//                                                .setCancelableOnTouchOutside(false)
//                                                .setPositive(R.string.ok, new SmartDialog.OnClickListener() {
//                                                    @Override
//                                                    public void onClick(Dialog dialog) {
//                                                        dialog.dismiss();
//                                                        sendResultForCalling(resp.getData());
//                                                        finish();
//                                                    }
//                                                }).show();
                                    ToastUtil.curt(R.string.auth_name_write_success);
                                    finish();
                                } else {
                                    mRlIdentityCardWarn.setVisible(true);
                                    mRlIdentityCardWarn.setCenterTxt(resp.getMsg());
                                }
                            }
                        }).fire();
        } else {
            ToastUtil.curt(R.string.setting_identity_card_when_login);
        }
    }

    /**
     * 由银行卡认证页面唤起,在实名认证成功后返回结果
     * <p>
     * 由个人信息页唤起,实名认证后返回结果
     *
     * @param result
     */

    private void sendResultForCalling(NameAuth.Result result) {
        if (getCallingActivity() == null) return;
        String fromClass = getCallingActivity().getClassName();

        if (fromClass.equals(BankcardAuthActivity.class.getName())) {
            Intent intent = new Intent().putExtra(BankcardAuthActivity.NAME_AUTH_RESULT, result);
            setResult(RESULT_OK, intent);
        }

        if (fromClass.equals(ProfileActivity.class.getName())) {
            Intent intent = new Intent().putExtra(ProfileActivity.RESULT_NAME_AUTH, result);
            setResult(RESULT_OK, intent);
        }
    }
}
