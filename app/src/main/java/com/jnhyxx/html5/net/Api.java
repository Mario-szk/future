package com.jnhyxx.html5.net;

import com.jnhyxx.html5.App;
import com.johnz.kutils.SecurityUtil;
import com.johnz.kutils.net.ApiParams;
import com.umeng.message.UmengRegistrar;

import java.security.NoSuchAlgorithmException;

public class API extends APIBase {

    public static final String TELE = "tele";
    public static final String CODE = "code";
    public static final String AUTH_CODE = "authCode";
    public static final String PASSWORD = "password";
    public static final String SIGN = "sign";
    public static final String TOKEN = "token";

    private API(String uri, ApiParams apiParams) {
        super(uri, apiParams);
    }

    public static class Account {
        /**
         * 更新友盟设备号 /user/user/updateUmCode
         * @param token
         */
        public static API updateUMDeviceId(String token) {
            ApiParams params = new ApiParams()
                    .put("token", token)
                    .put("umCode", UmengRegistrar.getRegistrationId(App.getAppContext()))
                    .put("platform", "android")
                    .put("environment", 1) // Release
                    .put("pkgtype", "androidcainiu");
            return new API("/user/user/updateUmCode", params);
        }

        /**
         * 获取注册短信验证码 /user/sms/getRegCode
         * @param tele
         */
        public static API obtainAuthCode(String tele) {
            String sign = null;
            try {
                 sign = SecurityUtil.md5Encrypt(tele + "luckin");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            ApiParams params = new ApiParams()
                    .put(TELE, tele)
                    .put(SIGN, sign);
            return new API("/user/sms/getRegCode", params);
        }

        /**
         * 找回密码时候获取短信验证码 /user/sms/findLoginPwdCode
         * @param tele
         */
        public static API obtainAuthCodeWhenFindPwd(String tele) {
            String sign = null;
            try {
                sign = SecurityUtil.md5Encrypt(tele + "luckin");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            ApiParams params = new ApiParams()
                    .put(TELE, tele)
                    .put(SIGN, sign);
            return new API("/user/sms/findLoginPwdCode", params);
        }

        /**
         * 找回登录密码 - 验证码验证 /user/sms/authLoginPwdCode
         * @param tele
         * @param code
         * @return
         */
        public static API authCodeWhenFindPassword(String tele, String code) {
            ApiParams params = new ApiParams()
                    .put(TELE, tele)
                    .put(CODE, code);
            return new API("/user/sms/authLoginPwdCode", params);
        }

        /**
         * 注册 "/user/register
         * @param phoneNum
         * @param password
         * @param authCode
         */
        public static API signUp(String phoneNum, String password, String authCode) {
            ApiParams params = new ApiParams()
                    .put("tele", phoneNum)
                    .put(PASSWORD, password)
                    .put(AUTH_CODE, authCode);
            // TODO: 7/22/16 统计数据
                    /*.put("deviceModel", "deviceModel")
                    .put("deviceImei", "deviceImei")
                    .put("deviceVersion", "deviceVersion")
                    .put("clientVersion", "clientVersion")
                    .put("regSource", "regSource")
                    .put("operator", "operator");*/
            return new API("/user/register", params);
        }

        /**
         * 登录 /user/login
         * @param loginName
         * @param password
         */
        public static API signIn(String loginName, String password) {
            try {
                password = SecurityUtil.md5Encrypt(password);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            ApiParams params = new ApiParams()
                    .put("loginName", loginName)
                    .put(PASSWORD, password);
            // TODO: 7/22/16 统计数据
            /*      .put("deviceModel", deviceModel)
                    .put("deviceImei", deviceImei)
                    .put("deviceVersion", deviceVersion)
                    .put("clientVersion", clientVersion)
                    .put("operator", operator);*/
            return new API("/user/login", params);
        }

        /**
         * 找回登录密码-修改登录密码 /user/user/findLoginPwd
         * @param phone
         * @param authCode
         * @param newPwd
         */
        public static API modifyPwdWhenFindPwd(String phone, String authCode, String newPwd) {
            ApiParams params = new ApiParams()
                    .put(TELE, phone)
                    .put(AUTH_CODE, authCode)
                    .put(PASSWORD, newPwd);
            return new API("/user/user/findLoginPwd", params);
        }

        /**
         * 验证银行卡是否绑定 /user/user/checkBankCard
         * @param token
         */
        public static API getBankcardInfo(String token) {
            ApiParams params = new ApiParams()
                    .put(TOKEN, token);
            return new API("/user/user/checkBankCard", params);
        }


        /**
         * 获取简单的个人信息 /user/user/getAcountDetail
         * @param token
         * @return
         */
        public static API getProfileSummary(String token) {
            ApiParams params = new ApiParams()
                    .put(TOKEN, token);
            return new API("/user/user/getAcountDetail", params);
        }

        /**
         * /user/user/checkUserName 验证是否实名认证
         * @param token
         * @return
         */
        public static API getUserNameAuth(String token) {
            ApiParams params = new ApiParams()
                    .put(TOKEN, token);
            return new API("/user/user/checkUserName", params);
        }

        /**
         * /user/user/authUser 实名认证
         * @param token
         * @param realName
         * @param identityNum
         * @return
         */
        public static API authUserName(String token, String realName, String identityNum) {
            ApiParams params = new ApiParams()
                    .put(TOKEN, token)
                    .put("realName", realName)
                    .put("idCard", identityNum);
            return new API("/user/user/authUser", params);
        }
    }

    public static class Finance {
        /**
         * 用户资金账户 /financy/financy/apiFinancyMain
         * @param token
         */
        public static API getFundInfo(String token) {
            ApiParams params = new ApiParams()
                    .put("token", token);
            return new API("/financy/financy/apiFinancyMain", params);
        }

        /**
         * 用户提现申请 /financy/financy/apiWithdraw
         * @param token
         * @param amount
         */
        public static API withdraw(String token, double amount) {
            ApiParams params = new ApiParams()
                    .put(TOKEN, token)
                    .put("inoutAmt", amount);
            return new API("/financy/financy/apiWithdraw", params);
        }
    }
}
