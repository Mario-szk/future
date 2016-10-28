package com.jnhyxx.html5.utils.presenter;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.jnhyxx.html5.activity.TradeActivity;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.domain.market.FullMarketData;
import com.jnhyxx.html5.domain.order.HoldingOrder;
import com.jnhyxx.html5.fragment.order.HoldingFragment;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback1;
import com.jnhyxx.html5.net.Callback2;
import com.jnhyxx.html5.net.Resp;
import com.johnz.kutils.FinanceUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class HoldingOrderPresenter {

    public HoldingOrderPresenter(IHoldingOrderView iHoldingOrderView) {
        mHandler = new Handler();
        mIHoldingOrderView = iHoldingOrderView;
        mResume = false;
    }

    private IHoldingOrderView mIHoldingOrderView;
    private List<HoldingOrder> mHoldingOrderList;

    private int mVarietyId;
    private int mFundType;
    private FullMarketData mMarketData;

    private boolean mResume;
    private boolean mLoading;
    private int mCounter;

    private Handler mHandler;

    public interface IHoldingOrderView {

        void onShowHoldingOrderList(List<HoldingOrder> holdingOrderList);

        void onShowTotalProfit(boolean hasHoldingOrders, double totalProfit, double ratio);

        void onSubmitAllHoldingPositionsCompleted(String message);

        void onSubmitHoldingOrderCompleted(HoldingOrder holdingOrder);
    }

    public void onResume() {
        mResume = true;
    }

    public void onPause() {
        mResume = false;
        mLoading = false;
        if (mHoldingOrderList != null) {
            mHoldingOrderList.clear();
        }
    }

    public void closeAllHoldingPositions(int fundType) {
        StringBuilder showIds = new StringBuilder();
        StringBuilder unwindPrices = new StringBuilder();
        for (final HoldingOrder holdingOrder : mHoldingOrderList) {
            if (holdingOrder.getOrderStatus() == HoldingOrder.ORDER_STATUS_HOLDING) {
                double unwindPrice = 0;
                if (mMarketData != null) {
                    if (holdingOrder.getDirection() == HoldingOrder.DIRECTION_LONG) {
                        unwindPrice = mMarketData.getBidPrice();
                    } else {
                        unwindPrice = mMarketData.getAskPrice();
                    }
                }
                showIds.append(holdingOrder.getShowId()).append(",");
                unwindPrices.append(unwindPrice).append(",");
            }
        }

        if (showIds.length() > 0) {
            showIds.deleteCharAt(showIds.length() - 1);
            unwindPrices.deleteCharAt(unwindPrices.length() - 1);
        }

        if (!TextUtils.isEmpty(showIds.toString())) {
            API.Order.closeAllHoldingOrders(showIds.toString(), fundType, unwindPrices.toString())
                    .setCallback(new Callback1<Resp<JsonObject>>() {
                        @Override
                        protected void onRespSuccess(Resp<JsonObject> resp) {
                            setOrderListStatus(HoldingOrder.ORDER_STATUS_CLOSING);
                            onViewShowHoldingOrderList(mHoldingOrderList);
                            onSubmitAllHoldingPositionsCompleted(resp.getMsg());
                            startQueryJob();

                        }
                    }).fire();
        }
    }

    private void setOrderListStatus(int orderStatus) {
        for (HoldingOrder holdingOrder : mHoldingOrderList) {
            holdingOrder.setOrderStatus(orderStatus);
        }
    }

    public void closePosition(int fundType, final HoldingOrder order) {
        double unwindPrice = 0;
        if (mMarketData != null) {
            if (order.getDirection() == HoldingOrder.DIRECTION_LONG) {
                unwindPrice = mMarketData.getBidPrice();
            } else {
                unwindPrice = mMarketData.getAskPrice();
            }
        }

        API.Order.closeHoldingOrder(order.getShowId(), fundType, unwindPrice)
                .setCallback(new Callback1<Resp<JsonObject>>() {
                    @Override
                    protected void onRespSuccess(Resp<JsonObject> resp) {
                        order.setOrderStatus(HoldingOrder.ORDER_STATUS_CLOSING);
                        onViewShowHoldingOrderList(mHoldingOrderList);
                        onSubmitHoldingOrderCompleted(order);
                        startQueryJob();
                    }
                }).fireSync();
    }

    public void setFullMarketData(FullMarketData marketData) {
        if (mHoldingOrderList == null) {
            mHoldingOrderList = new ArrayList<>();
        }

        mMarketData = marketData;
        BigDecimal totalProfit = new BigDecimal(0);
        boolean hasHoldingOrders = false;
        double ratio = 0;
        boolean refresh = false;

        for (HoldingOrder holdingOrder : mHoldingOrderList) {
            int orderStatus = holdingOrder.getOrderStatus();
            if (orderStatus >= HoldingOrder.ORDER_STATUS_HOLDING && orderStatus < HoldingOrder.ORDER_STATUS_SETTLED) {
                // 持仓中、卖处理中的订单
                hasHoldingOrders = true;
                ratio = holdingOrder.getRatio();

                BigDecimal eachPointMoney = new BigDecimal(holdingOrder.getEachPointMoney());
                BigDecimal diff;
                if (holdingOrder.getDirection() == HoldingOrder.DIRECTION_LONG) {
                    diff = FinanceUtil.subtraction(marketData.getBidPrice(), holdingOrder.getRealAvgPrice());
                } else {
                    diff = FinanceUtil.subtraction(holdingOrder.getRealAvgPrice(), marketData.getAskPrice());
                }
                diff = diff.multiply(eachPointMoney).setScale(4, RoundingMode.HALF_EVEN);

                BigDecimal bigDecimalStopLoss = FinanceUtil.multiply(holdingOrder.getStopLoss(), -1);
                if (diff.compareTo(new BigDecimal(holdingOrder.getStopWin())) >= 0) {
                    refresh = true;
                    Log.d("TAG", "setFullMarketData: diff: " + diff.doubleValue() + ", stopWin: " + holdingOrder.getStopWin());
                }
                if (diff.compareTo(bigDecimalStopLoss) <= 0) {
                    Log.d("TAG", "setFullMarketData: diff: " + diff.doubleValue() + ", stopLoss: " + bigDecimalStopLoss.doubleValue());
                    refresh = true;
                }
                totalProfit = totalProfit.add(diff);
            }

        }

        onViewShowTotalProfit(hasHoldingOrders, totalProfit.doubleValue(), ratio);

        if (refresh) { // 触及风控刷新
            Log.d("TAG", "触及风控刷新");
            loadHoldingOrderList(mVarietyId, mFundType);
        }
    }

    private void onViewShowTotalProfit(boolean hasHoldingOrders, double totalProfit, double ratio) {
        if (mResume && mIHoldingOrderView != null) {
            mIHoldingOrderView.onShowTotalProfit(hasHoldingOrders, totalProfit, ratio);
        }
    }

    private void onViewShowHoldingOrderList(List<HoldingOrder> holdingOrderList) {
        if (mResume && mIHoldingOrderView != null) {

            if (mIHoldingOrderView instanceof TradeActivity) {
                Log.d("TAG", "onViewShowHoldingOrderList: " + "TradeActivity");
            } else if (mIHoldingOrderView instanceof HoldingFragment) {
                Log.d("TAG", "onViewShowHoldingOrderList: " + "HoldingFragment");
            }

            mIHoldingOrderView.onShowHoldingOrderList(holdingOrderList);
        }
    }

    private void onSubmitAllHoldingPositionsCompleted(String message) {
        if (mResume && mIHoldingOrderView != null) {
            mIHoldingOrderView.onSubmitAllHoldingPositionsCompleted(message);
        }
    }

    private void onSubmitHoldingOrderCompleted(HoldingOrder holdingOrder) {
        if (mResume && mIHoldingOrderView != null) {
            mIHoldingOrderView.onSubmitHoldingOrderCompleted(holdingOrder);
        }
    }

    public void loadHoldingOrderList(int varietyId, int fundType) {
        if (!LocalUser.getUser().isLogin()) return;

        Log.d("TAG", "mLoading: " + mLoading);
        if (mLoading) return;
        Log.d("TAG", "Counter: " + mCounter);

        mVarietyId = varietyId;
        mFundType = fundType;

        mLoading = true;
        API.Order.getHoldingOrderList(varietyId, fundType)
                .setCallback(new Callback2<Resp<List<HoldingOrder>>, List<HoldingOrder>>() {
                    @Override
                    public void onRespSuccess(List<HoldingOrder> holdingOrderList) {
                        Log.d("TAG", "onRespSuccess: loadHoldingOrderList finished");
                        mLoading = false;
                        mHoldingOrderList = holdingOrderList;
                        onViewShowHoldingOrderList(holdingOrderList);
                        startQueryJob();
                    }
                }).fire();
    }

    /**
     * 刷新策略:
     * <p/>
     * 500ms 间隔, 刷新5次
     * 2s 间隔, 刷新5次
     * 4s 间隔, 刷新10次
     */
    private void startQueryJob() {
        boolean refresh = false;
        for (HoldingOrder holdingOrder : mHoldingOrderList) {
            int orderStatus = holdingOrder.getOrderStatus();

            // 存在处理中的订单,买处理中(代持有),卖处理中(平仓中), 使用 "策略" 刷新持仓数据
            if (orderStatus == HoldingOrder.ORDER_STATUS_PAID_UNHOLDING
                    || orderStatus == HoldingOrder.ORDER_STATUS_CLOSING) {
                refresh = true;
                break;
            }
        }

        if (refresh) {
            Log.d("TAG", "startQueryJob: " + refresh);
            doDelayRefreshJob();
        } else {
            mCounter = 0;
        }
    }

    private void doDelayRefreshJob() {
        if (mCounter < 5) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadHoldingOrderList(mVarietyId, mFundType);
                }
            }, mCounter == 0 ? 0 : 5 * 100); // 第一次不延时
        } else if (mCounter < 10) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadHoldingOrderList(mVarietyId, mFundType);
                }
            }, 2 * 1000);
        } else if (mCounter < 20) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadHoldingOrderList(mVarietyId, mFundType);
                }
            }, 4 * 1000);
        }
        mCounter++;
    }
}
