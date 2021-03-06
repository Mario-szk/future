package com.jnhyxx.html5.activity.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.BaseActivity;
import com.jnhyxx.html5.domain.account.UserInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.KeyBoardHelper;
import com.jnhyxx.html5.utils.StrFormatter;
import com.jnhyxx.html5.utils.ToastUtil;
import com.jnhyxx.html5.utils.ValidationWatcher;
import com.jnhyxx.html5.view.CommonFailWarn;
import com.johnz.kutils.AppInfo;
import com.johnz.kutils.Launcher;
import com.johnz.kutils.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignInActivity extends BaseActivity {

    private static final int REQ_CODE_REGISTER = 1;

    public static final String ACTION_LOGIN  = "com.jnhyxx.html5.login_success";

    @BindView(R.id.phoneNum)
    EditText mPhoneNum;
    @BindView(R.id.password)
    EditText mPassword;

    @BindView(R.id.clearPhoneNumButton)
    ImageView mClearPhoneNumButton;
    @BindView(R.id.showPasswordButton)
    ImageView mShowPasswordButton;

    @BindView(R.id.signUpButton)
    TextView mSignUpButton;
    @BindView(R.id.forgetPassword)
    TextView mForgetPassword;
    @BindView(R.id.signInButton)
    TextView mSignInButton;

    @BindView(R.id.failWarn)
    CommonFailWarn mCommonFailWarn;


    @BindView(R.id.topLayout)
    LinearLayout mTopLayout;
    @BindView(R.id.hideLayout)
    LinearLayout mHideLayout;

    private KeyBoardHelper mKeyBoardHelper;
    private int bottomHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);

        mPhoneNum.addTextChangedListener(mPhoneValidationWatcher);
        mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mPassword.addTextChangedListener(mValidationWatcher);

        mPhoneNum.setText(LocalUser.getUser().getPhone());

        setKeyboardHelper();
    }

    /**
     * 设置对键盘高度的监听
     */
    private void setKeyboardHelper() {
        mKeyBoardHelper = new KeyBoardHelper(this);
        mKeyBoardHelper.onCreate();
        mKeyBoardHelper.setOnKeyBoardStatusChangeListener(onKeyBoardStatusChangeListener);
        mHideLayout.post(new Runnable() {
            @Override
            public void run() {
                bottomHeight = mHideLayout.getHeight();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPhoneNum.removeTextChangedListener(mPhoneValidationWatcher);
        mPassword.removeTextChangedListener(mValidationWatcher);
        mKeyBoardHelper.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_REGISTER && resultCode == RESULT_OK) {
            // User register done, then finish
            setResult(RESULT_OK);
            finish();
        }
    }

    private KeyBoardHelper.OnKeyBoardStatusChangeListener onKeyBoardStatusChangeListener = new KeyBoardHelper.OnKeyBoardStatusChangeListener() {

        @Override
        public void OnKeyBoardPop(int keyboardHeight) {
            final int height = keyboardHeight;
            if (bottomHeight < height) {
                int offset = bottomHeight - height;
                final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTopLayout
                        .getLayoutParams();
                lp.topMargin = offset;
                mTopLayout.setLayoutParams(lp);
            }

        }

        @Override
        public void OnKeyBoardClose(int oldKeyboardHeight) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTopLayout
                    .getLayoutParams();
            if (lp.topMargin != 0) {
                lp.topMargin = 0;
                mTopLayout.setLayoutParams(lp);
            }

        }
    };
    private ValidationWatcher mValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {
            boolean enable = checkSignInButtonEnable();
            if (enable != mSignInButton.isEnabled()) {
                mSignInButton.setEnabled(enable);
            }

            boolean visible = checkClearPhoneNumButtonVisible();
            mClearPhoneNumButton.setVisibility(visible ? View.VISIBLE : View.GONE);

            visible = checkShowPasswordButtonVisible();
            mShowPasswordButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    };

    private ValidationWatcher mPhoneValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            mValidationWatcher.afterTextChanged(s);

            formatPhoneNumber();
        }
    };

    private void formatPhoneNumber() {
        String oldPhone = mPhoneNum.getText().toString();
        String phoneNoSpace = oldPhone.replaceAll(" ", "");
        String newPhone = StrFormatter.getFormatPhoneNumber(phoneNoSpace);
        if (!newPhone.equalsIgnoreCase(oldPhone)) {
            mPhoneNum.setText(newPhone);
            mPhoneNum.setSelection(newPhone.length());
        }
    }

    private boolean checkClearPhoneNumButtonVisible() {
        String phoneNum = ViewUtil.getTextTrim(mPhoneNum);
        if (!TextUtils.isEmpty(phoneNum)) {
            return true;
        }
        return false;
    }

    private boolean checkShowPasswordButtonVisible() {
        String password = ViewUtil.getTextTrim(mPassword);
        if (!TextUtils.isEmpty(password)) {
            return true;
        }
        return false;
    }

    private boolean checkSignInButtonEnable() {
        String phoneNum = ViewUtil.getTextTrim(mPhoneNum).replaceAll(" ", "");
        if (TextUtils.isEmpty(phoneNum) || phoneNum.length() < 11) {
            return false;
        }
        String password = ViewUtil.getTextTrim(mPassword);
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            return false;
        }
        return true;
    }

    @OnClick({R.id.clearPhoneNumButton, R.id.showPasswordButton, R.id.signInButton, R.id.signUpButton, R.id.forgetPassword})
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.clearPhoneNumButton:
                mPhoneNum.setText("");
                break;
            case R.id.showPasswordButton:
                changePasswordInputType();
                break;
            case R.id.signInButton:
                signIn();
                break;
            case R.id.signUpButton:
                switchToSignUpPage();
                break;
            case R.id.forgetPassword:
                Launcher.with(this, FindPwdActivity.class).execute();
                break;
        }
    }

    private void switchToSignUpPage() {
        if (getCallingActivity() != null
                && getCallingActivity().getClassName().equals(SignUpActivity.class.getName())) {
            finish();
        } else {
            Launcher.with(this, SignUpActivity.class).executeForResult(REQ_CODE_REGISTER);
        }
    }

    private void signIn() {
        final String phoneNum = ViewUtil.getTextTrim(mPhoneNum).replaceAll(" ", "");
        String password = ViewUtil.getTextTrim(mPassword);
        API.User.login(phoneNum, password, AppInfo.getMetaData(this, AppInfo.Meta.UMENG_CHANNEL))
                .setIndeterminate(this).setTag(TAG)
                .setCallback(new Callback<Resp<JsonObject>>() {
                    @Override
                    public void onReceive(Resp<JsonObject> jsonObjectResp) {
                        if (jsonObjectResp.isSuccess()) {
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(ACTION_LOGIN));

                            UserInfo userInfo = new Gson().fromJson(jsonObjectResp.getData(), UserInfo.class);
                            LocalUser.getUser().setUserInfo(userInfo, phoneNum);
                            ToastUtil.show(R.string.login_success);
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            mCommonFailWarn.show(jsonObjectResp.getMsg());
                        }
                    }
                }).fire();
    }

    private void changePasswordInputType() {
        if (mShowPasswordButton.isSelected()) {
            mShowPasswordButton.setSelected(false);
            mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mShowPasswordButton.setSelected(true);
        }
        CharSequence text = mPassword.getText();
        if (text != null) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, text.length());
        }
        mPassword.postInvalidate();
    }
}