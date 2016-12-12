package com.jnhyxx.html5.domain.market;

import android.util.Log;

import com.jnhyxx.html5.domain.order.FuturesFinancing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ${wangJie} on 2016/11/29.
 * 闪电下单状态
 */

public class ProductLightningOrderStatus implements Serializable {
    private static final long serialVersionUID = -6950030646707014378L;

    private static final String TAG = "ProductLightningOrderSt";

    public static final String KEY_LIGHTNING_ORDER_IS_OPEN = "KEY_LIGHTNING_ORDER_IS_OPEN";

    //服务端的产品配资数据
    public static final String KEY_WEB_PRODUCT_DEPLOY = "WEB_PRODUCT_DEPLOY";

    public static final int TYPE_BUY_LONG = 1;
    public static final int TYPE_SELL_SHORT = 0;

    //表示闪电下单按钮打开同意协议的fragment的标志
    public static final int TAG_OPEN_ARRGE_FRAGMENT_PAGE = 333;


    /**
     * assetsId : 1
     * varietyId : 2
     * handsNum : 1
     * stopLossPrice : 10
     * stopWinPrice : 180
     * marginMoney : 92
     * fees : 10
     * ratio : 7
     */

    /**
     * 支付方式   0：积分 1：现金
     */
    private int payType;
    /**
     * 配资id
     */
    private int assetsId;
    /**
     * 品种id
     */
    private int varietyId;
    /**
     * 手数
     */
    private int handsNum;
    /**
     * 止损金额
     */
    private double stopLossPrice;
    /**
     * 止盈金额
     */
    private double stopWinPrice;
    /**
     * 提交订单的时候的止盈点数
     */
    private int stopProfitPoint;

    /**
     * 保证金
     */
    private double marginMoney;
    /**
     * 手续费
     */
    private double fees;
    /**
     * 费率
     */
    private double ratio;

    /**
     * 产品配资
     */
    FuturesFinancing futuresFinancing;


    public int getPayType() {
        return payType;
    }

    public void setPayType(int payType) {
        this.payType = payType;
    }

    public int getAssetsId() {
        return assetsId;
    }

    public void setAssetsId(int assetsId) {
        this.assetsId = assetsId;
    }

    public int getHandsNum() {
        return handsNum;
    }

    public void setHandsNum(int handsNum) {
        this.handsNum = handsNum;
    }

    public int getVarietyId() {
        return varietyId;
    }

    public void setVarietyId(int varietyId) {
        this.varietyId = varietyId;
    }

    public double getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(double stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }

    public double getStopWinPrice() {
        return stopWinPrice;
    }

    public void setStopWinPrice(double stopWinPrice) {
        this.stopWinPrice = stopWinPrice;
    }

    public double getMarginMoney() {
        return marginMoney;
    }

    public void setMarginMoney(double marginMoney) {
        this.marginMoney = marginMoney;
    }

    public double getFees() {
        return fees;
    }

    public void setFees(double fees) {
        this.fees = fees;
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public int getStopProfitPoint() {
        return stopProfitPoint;
    }

    public void setStopProfitPoint(int stopProfitPoint) {
        this.stopProfitPoint = stopProfitPoint;
    }

    public void setFuturesFinancing(FuturesFinancing futuresFinancing) {
        this.futuresFinancing = futuresFinancing;
    }

    @Override
    public String toString() {
        return "ProductLightningOrderStatus{" +
                "payType=" + payType +
                ", assetsId=" + assetsId +
                ", varietyId=" + varietyId +
                ", handsNum=" + handsNum +
                ", stopLossPrice=" + stopLossPrice +
                ", stopWinPrice=" + stopWinPrice +
                ", stopProfitPoint=" + stopProfitPoint +
                ", marginMoney=" + marginMoney +
                ", fees=" + fees +
                ", ratio=" + ratio +
                ", futuresFinancing=" + futuresFinancing +
                '}';
    }

    /**
     * 本地的闪电下单数据和产品的配资进行对比，如果不相同，则闪电下单失效
     *
     * @param futuresFinancing 产品配资方案
     * @return
     */
    public boolean compareDataWithWeb(FuturesFinancing futuresFinancing) {
        if (futuresFinancing.getAssets() == null && futuresFinancing.getAssets().isEmpty())
            return false;
        List<FuturesFinancing.AssetsBean> assets = futuresFinancing.getAssets();
        if (futuresFinancing.getRatio() == getRatio()) {
            for (int i = 0; i < assets.size(); i++) {
                if (getAssetsId() == assets.get(i).getAssetsId()) {
                    FuturesFinancing.AssetsBean assetsBean = assets.get(i);
                    Log.d(TAG, "配资方案  assetsBean " + assetsBean.toString());
                    if (assetsBean.getFees() * getHandsNum() == getFees() &&
                            assetsBean.getStopLossBeat() == getStopLossPrice() &&
                            assetsBean.getMarginBeat() * getHandsNum() == getMarginMoney() &&
                            assetsBean.getHandsMultiple().contains(String.valueOf(getHandsNum())) &&
                            assetsBean.getStopWinBeats().containsKey(String.valueOf(getStopProfitPoint())) &&
                            assetsBean.getStopWinBeats().get(String.valueOf(getStopProfitPoint())) == getStopWinPrice()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<FuturesFinancing.StopLoss> getStopLossList(Product product) {
        List<FuturesFinancing.StopLoss> result = new ArrayList<>();
        if (futuresFinancing != null) {
            for (FuturesFinancing.AssetsBean assetsBean : futuresFinancing.getAssets()) {
                if (assetsBean.getStopLossBeat() == getStopLossPrice()) {
                    //是否是默认手数
                    assetsBean.setIsDefault(1);
                }
                FuturesFinancing.StopLoss stopLoss = new FuturesFinancing.StopLoss(product.getLossProfitScale(), product.getSign(), assetsBean);

                result.add(stopLoss);
            }
        }
        return result;
    }
    
}
