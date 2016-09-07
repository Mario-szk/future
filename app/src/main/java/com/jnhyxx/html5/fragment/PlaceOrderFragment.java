package com.jnhyxx.html5.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.jnhyxx.html5.R;
import com.jnhyxx.html5.domain.local.SubmittedOrder;
import com.jnhyxx.html5.domain.market.Product;
import com.jnhyxx.html5.domain.order.FuturesFinancing;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback2;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.utils.BlurEngine;
import com.jnhyxx.html5.view.BuySellVolumeLayout;
import com.jnhyxx.html5.view.OrderConfigurationSelector;
import com.johnz.kutils.FinanceUtil;
import com.johnz.kutils.StrUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class PlaceOrderFragment extends BaseFragment {

    @BindView(R.id.tradeQuantitySelector)
    OrderConfigurationSelector mTradeQuantitySelector;
    @BindView(R.id.lastPrice)
    TextView mLastPrice;
    @BindView(R.id.priceChange)
    TextView mPriceChange;
    @BindView(R.id.buySellVolumeLayout)
    BuySellVolumeLayout mBuySellVolumeLayout;
    @BindView(R.id.touchStopLossSelector)
    OrderConfigurationSelector mTouchStopLossSelector;
    @BindView(R.id.touchStopProfitSelector)
    OrderConfigurationSelector mTouchStopProfitSelector;
    @BindView(R.id.margin)
    TextView mMargin;
    @BindView(R.id.tradeFee)
    TextView mTradeFee;
    @BindView(R.id.rateAndMarketTime)
    TextView mRateAndMarketTime;
    @BindView(R.id.totalTobePaid)
    TextView mTotalTobePaid;
    @BindView(R.id.lastBidAskPrice)
    TextView mLastBidAskPrice;
    @BindView(R.id.confirmButton)
    TextView mConfirmButton;

    private Callback mCallback;

    private static final String TYPE = "longOrShort";
    public static final int TYPE_BUY_LONG = 1;
    public static final int TYPE_SELL_SHORT = 0;

    private int mLongOrShort;
    private Product mProduct;
    private FuturesFinancing mFuturesFinancing;
    private SubmittedOrder mSubmittedOrder;

    private Unbinder mBinder;
    private BlurEngine mBlurEngine;

    public static PlaceOrderFragment newInstance(int longOrShort, Product product) {
        PlaceOrderFragment fragment = new PlaceOrderFragment();
        Bundle args = new Bundle();
        args.putInt(TYPE, longOrShort);
        args.putSerializable(Product.EX_PRODUCT, product);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Callback) {
            mCallback = (Callback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnBuyBtnClickListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mLongOrShort = getArguments().getInt(TYPE, 0);
            mProduct = (Product) getArguments().getSerializable(Product.EX_PRODUCT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBlurEngine = new BlurEngine(container, R.color.blackHalfTransparent);

        View view = inflater.inflate(R.layout.fragment_place_order, container, false);
        mBinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSubmittedOrder = new SubmittedOrder(mProduct.getVarietyId(), mLongOrShort);

        mTradeQuantitySelector.setOnItemSelectedListener(new OrderConfigurationSelector.OnItemSelectedListener() {
            @Override
            public void onItemSelected(OrderConfigurationSelector.OrderConfiguration configuration, int position) {
                if (configuration instanceof FuturesFinancing.TradeQuantity) {
                    FuturesFinancing.TradeQuantity tradeQuantity = (FuturesFinancing.TradeQuantity) configuration;
                    mSubmittedOrder.setHandsNum(tradeQuantity.getQuantity());
                    updateMarginTradeFeeAndTotal(tradeQuantity);
                }
            }
        });
        mTouchStopLossSelector.setOnItemSelectedListener(new OrderConfigurationSelector.OnItemSelectedListener() {
            @Override
            public void onItemSelected(OrderConfigurationSelector.OrderConfiguration configuration, int position) {
                if (configuration instanceof FuturesFinancing.StopLoss) {

                    FuturesFinancing.StopLoss stopLoss = (FuturesFinancing.StopLoss) configuration;

                    // 设置止盈
                    List<FuturesFinancing.StopProfit> stopProfitList = stopLoss.getStopProfitList();
                    mTouchStopProfitSelector.setOrderConfigurationList(stopProfitList);

                    // 设置手数
                    List<FuturesFinancing.TradeQuantity> tradeQuantityList = stopLoss.getTradeQuantityList();
                    mTradeQuantitySelector.setOrderConfigurationList(tradeQuantityList);

                    mSubmittedOrder.setAssetsId(stopLoss.getAssetsBean().getAssetsId());
                }
            }
        });
        mTouchStopProfitSelector.setOnItemSelectedListener(new OrderConfigurationSelector.OnItemSelectedListener() {
            @Override
            public void onItemSelected(OrderConfigurationSelector.OrderConfiguration configuration, int position) {
                if (configuration instanceof FuturesFinancing.StopProfit) {
                    FuturesFinancing.StopProfit stopProfit = (FuturesFinancing.StopProfit) configuration;
                    mSubmittedOrder.setStopProfitPoint(stopProfit.getStopProfitPoint());
                }
            }
        });

        API.Order.getFuturesFinancing(mProduct.getVarietyId()).setTag(TAG)
                .setIndeterminate(this)
                .setCallback(new Callback2<Resp<FuturesFinancing>, FuturesFinancing>() {
                    @Override
                    public void onRespSuccess(FuturesFinancing futuresFinancing) {
                        mFuturesFinancing = futuresFinancing;
                        updatePlaceOrderViews();
                    }
                }).fire();
    }

    private void updateMarginTradeFeeAndTotal(FuturesFinancing.TradeQuantity tradeQuantity) {
        int scale = mProduct.getLossProfitScale();

        // DOMESTIC
        String marginWithSign = mProduct.getSign()
                + FinanceUtil.formatWithScale(tradeQuantity.getMargin(), scale);
        String tradeFeeWithSign = mProduct.getSign()
                + FinanceUtil.formatWithScale(tradeQuantity.getFee() / mProduct.getRatio(), scale);
        String totalWithSign = mProduct.getSign()
                + FinanceUtil.formatWithScale(tradeQuantity.getMargin()
                + tradeQuantity.getFee() / mProduct.getRatio(), scale);
        mMargin.setText(marginWithSign);
        mTradeFee.setText(tradeFeeWithSign);
        mTotalTobePaid.setText(totalWithSign);

        if (mProduct.isForeign()) {
            String marginRmb = "  ( " + FinanceUtil.UNIT_SIGN_CNY +
                    FinanceUtil.formatWithScale(tradeQuantity.getMargin() * mProduct.getRatio()) + " )";
            mMargin.setText(
                    StrUtil.mergeTextWithColor(marginWithSign, marginRmb, Color.parseColor("#666666"))
            );
            String tradeFeeRmb = "  ( " + FinanceUtil.UNIT_SIGN_CNY +
                    FinanceUtil.formatWithScale(tradeQuantity.getFee()) + " )";
            mTradeFee.setText(
                    StrUtil.mergeTextWithColor(tradeFeeWithSign, tradeFeeRmb, Color.parseColor("#666666"))
            );
            String totalRmb = FinanceUtil.UNIT_SIGN_CNY
                    + (tradeQuantity.getMargin() * mProduct.getRatio() + tradeQuantity.getFee());
            String totalForeign = "  ( " + totalWithSign + " )";
            mTotalTobePaid.setText(
                    StrUtil.mergeTextWithColor(totalRmb, totalForeign, Color.parseColor("#666666"))
            );
        }
    }

    private void updatePlaceOrderViews() {
        if (isRemoving() || !isAdded()) return;

        // 设置止损
        mTouchStopLossSelector.setOrderConfigurationList(mFuturesFinancing.getStopLossList(mProduct));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBlurEngine.onDestroyView();
        mBinder.unbind();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @OnClick(R.id.confirmButton)
    public void onClick() {
        if (mCallback != null) {
            mCallback.onConfirmBtnClick(mSubmittedOrder);
        }
    }

    public interface Callback {
        void onConfirmBtnClick(SubmittedOrder submittedOrder);
    }

    @Override
    public Animation onCreateAnimation(int transit, final boolean enter, int nextAnim) {
        Animation animation;

        if (enter) {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_from_bottom);
        } else {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_to_bottom);
        }

        return animation;
    }
}
