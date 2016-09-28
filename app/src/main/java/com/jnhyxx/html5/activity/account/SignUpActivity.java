package com.jnhyxx.html5.activity.account;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.BaseActivity;
import com.jnhyxx.html5.activity.WebViewActivity;
import com.jnhyxx.html5.domain.account.UserInfo;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.CommonMethodUtils;
import com.jnhyxx.html5.utils.ValidationWatcher;
import com.jnhyxx.html5.view.CommonFailWarn;
import com.jnhyxx.html5.view.CustomToast;
import com.jnhyxx.html5.view.TitleBar;
import com.johnz.kutils.Launcher;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SignUpActivity extends BaseActivity {

    private static final String TAG = "SignUpActivity";
    @BindView(R.id.phoneNum)
    EditText mPhoneNum;
    @BindView(R.id.registerAuthCode)
    EditText mMessageAuthCode;
    @BindView(R.id.obtainAuthCode)
    TextView mObtainAuthCode;
    @BindView(R.id.password)
    EditText mPassword;
    @BindView(R.id.agreeProtocol)
    CheckBox mAgreeProtocol;
    @BindView(R.id.serviceProtocol)
    TextView mServiceProtocol;
    @BindView(R.id.signUpButton)
    TextView mSignUpButton;
    @BindView(R.id.titleBar)
    TitleBar mTitleBar;
    @BindView(R.id.failWarn)
    CommonFailWarn mFailWarn;
    //获取图片验证码
    @BindView(R.id.imageCode)
    RelativeLayout mImageCode;
    @BindView(R.id.registerRetrieveIma)
    EditText mRegisterRetrieveImage;
    @BindView(R.id.showPasswordButton)
    ImageView mImagePasswordType;
    @BindView(R.id.retrieveImageCode)
    ImageView mRetrieveImage;

    private boolean flag = false;
    private boolean mFreezeObtainAuthCode;
    private int mCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);

        mPhoneNum.addTextChangedListener(mValidationWatcher);
        mMessageAuthCode.addTextChangedListener(mValidationWatcher);
        mPassword.addTextChangedListener(mValidationWatcher);
        mAgreeProtocol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mAgreeProtocol.setChecked(isChecked);
                mAgreeProtocol.setButtonDrawable(isChecked ? R.drawable.checkbox_register_selected : R.drawable.checkbox_register_nor);
                activeButtons();
            }
        });
        initTitleBar();
    }

    private void initTitleBar() {
        mTitleBar.setOnRightViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Launcher.with(SignUpActivity.this, SignInActivity.class).execute();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private ValidationWatcher mValidationWatcher = new ValidationWatcher() {
        @Override
        public void afterTextChanged(Editable editable) {
            activeButtons();
            String phoneNum = mPassword.getText().toString().trim();
            if (!TextUtils.isEmpty(phoneNum)) {
                mImagePasswordType.setVisibility(View.VISIBLE);
            } else {
                mImagePasswordType.setVisibility(View.INVISIBLE);
            }
        }
    };

    private void activeButtons() {
        boolean enable = checkObtainAuthCodeEnable();
        if (enable != mObtainAuthCode.isEnabled()) {
            mObtainAuthCode.setEnabled(enable);
        }

        enable = checkSignUpButtonEnable();
        if (enable != mSignUpButton.isEnabled()) {
            mSignUpButton.setEnabled(enable);
        }
    }

    private boolean checkSignUpButtonEnable() {
        String phoneNum = mPhoneNum.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNum) || phoneNum.length() < 11) {
            return false;
        }

        String authCode = mMessageAuthCode.getText().toString().trim();
        if (TextUtils.isEmpty(authCode) || authCode.length() < 4) {
            return false;
        }

        String password = mPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            return false;
        }

        return true && mAgreeProtocol.isChecked();
    }

    private boolean checkObtainAuthCodeEnable() {
        String phoneNum = mPhoneNum.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNum) || phoneNum.length() < 11) {
            return false;
        }
        return true && !mFreezeObtainAuthCode;
    }

    @OnClick({R.id.obtainAuthCode, R.id.signUpButton, R.id.retrieveImageCode, R.id.serviceProtocol, R.id.showPasswordButton})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.obtainAuthCode:
                obtainAuthCode();
                break;
            case R.id.signUpButton:
                signUp();
                break;
            case R.id.retrieveImageCode:
                getRegisterImage();
                break;
            case R.id.serviceProtocol:
                Launcher.with(SignUpActivity.this, WebViewActivity.class)
                        .putExtra(WebViewActivity.EX_URL, API.getRegisterServiceProtocol())
                        .putExtra(WebViewActivity.EX_TITLE, getString(R.string.service_protocol_title)).execute();
                break;
            case R.id.showPasswordButton:
                changePasswordInputType();
                break;

        }
    }

    private void obtainAuthCode() {
        String phoneNum = mPhoneNum.getText().toString();
        String imageCode = "";
        if (mRegisterRetrieveImage.isShown()) {
            imageCode = mRegisterRetrieveImage.getText().toString().trim();
        }
        Log.d(TAG, "注册获取图片验证码的手机号码 " + phoneNum + "所输入的图片验证码" + imageCode);
        API.User.obtainAuthCode(phoneNum, imageCode)
                .setIndeterminate(this).setTag(TAG)
                .setCallback(new Callback<Resp>() {
                    @Override
                    public void onReceive(Resp resp) {
                        if (resp.isSuccess()) {
                            mCounter = 60;
                            mFreezeObtainAuthCode = true;
                            mObtainAuthCode.setEnabled(false);
                            mObtainAuthCode.setText(getString(R.string.resend_after_n_seconds, mCounter));
                            startScheduleJob(1 * 1000);
                        } else if (resp.getCode() == Resp.CODE_ERROR_REQUEST_OVERRUN) {
                            getRegisterImage();
                            mFailWarn.show(resp.getMsg());
                        } else {
                            mFailWarn.show(resp.getMsg());
                        }
                    }
                }).fire();
    }

    public void signUp() {
        String phoneNum = mPhoneNum.getText().toString().trim();
        String password = mPassword.getText().toString().trim();
        String authCode = mMessageAuthCode.getText().toString().trim();
        API.User.register(phoneNum, password, authCode, null)
                .setIndeterminate(this).setTag(TAG)
                .setCallback(new Callback<Resp<JsonObject>>() {
                    @Override
                    public void onReceive(Resp<JsonObject> resp) {
                        if (resp.isSuccess()) {
                            CustomToast.getInstance().makeText(SignUpActivity.this, R.string.register_succeed);
                            UserInfo info = new Gson().fromJson(resp.getData(), UserInfo.class);
                            LocalUser.getUser().setUserInfo(info);

                        } else {
                            mFailWarn.show(resp.getMsg());
                        }
                    }
                }).fire();
    }

    private void getRegisterImage() {
        final String userPhone = mPhoneNum.getText().toString().trim();
        // TODO: 2016/9/26      必须放在子线程中 不然java.lang.IllegalStateException: Method call should not happen from the main thread.

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(userPhone)) return;
                String url = CommonMethodUtils.imageCodeUri(userPhone, "/user/user/getRegImage.do");
                Log.d(TAG, "register image code Url  " + url);
                Picasso picasso = Picasso.with(SignUpActivity.this);
                RequestCreator requestCreator = picasso.load(url);
                try {
                    final Bitmap bitmap = requestCreator.get();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (bitmap != null) {
                                mImageCode.setVisibility(View.VISIBLE);
                                mRetrieveImage.setImageBitmap(bitmap);
                            } else {
                                // TODO: 2016/9/27 这里产品每明确
//                                mFailWarn.setVisible(true, true);
//                                mFailWarn.show(R.string.network_error_load_image);
                            }
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void changePasswordInputType() {
        if (!flag) {
            mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mImagePasswordType.setSelected(true);
        } else {
            mImagePasswordType.setSelected(false);
            mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        flag = !flag;
        mPassword.postInvalidate();
        CharSequence text = mPassword.getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, text.length());
        }
    }

    @Override
    public void onTimeUp(int count) { // TODO: 7/22/16 考虑Home了之后页面被回收的情况
        mCounter--;
        if (mCounter <= 0) {
            mFreezeObtainAuthCode = false;
            mObtainAuthCode.setEnabled(true);
            mObtainAuthCode.setText(R.string.obtain_auth_code);
            stopScheduleJob();
        } else {
            mObtainAuthCode.setText(getString(R.string.resend_after_n_seconds, mCounter));
        }
    }
}
