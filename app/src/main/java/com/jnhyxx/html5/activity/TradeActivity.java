package com.jnhyxx.html5.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jnhyxx.chart.FlashView;
import com.jnhyxx.chart.KlineView;
import com.jnhyxx.chart.TrendView;
import com.jnhyxx.chart.domain.FlashViewData;
import com.jnhyxx.chart.domain.KlineViewData;
import com.jnhyxx.chart.domain.TrendViewData;
import com.jnhyxx.html5.Preference;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.account.RechargeActivity;
import com.jnhyxx.html5.activity.account.SignInActivity;
import com.jnhyxx.html5.activity.order.OrderActivity;
import com.jnhyxx.html5.activity.trade.SetLightningOrdersActivity;
import com.jnhyxx.html5.constans.Unit;
import com.jnhyxx.html5.domain.local.LocalUser;
import com.jnhyxx.html5.domain.local.SubmittedOrder;
import com.jnhyxx.html5.domain.market.FullMarketData;
import com.jnhyxx.html5.domain.market.Product;
import com.jnhyxx.html5.domain.order.ExchangeStatus;
import com.jnhyxx.html5.domain.order.FuturesFinancing;
import com.jnhyxx.html5.domain.order.HoldingOrder;
import com.jnhyxx.html5.domain.order.LightningOrderAsset;
import com.jnhyxx.html5.fragment.order.AgreementFragment;
import com.jnhyxx.html5.fragment.order.PlaceOrderFragment;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.net.Callback1;
import com.jnhyxx.html5.net.Callback2;
import com.jnhyxx.html5.net.Resp;
import com.jnhyxx.html5.netty.NettyClient;
import com.jnhyxx.html5.netty.NettyHandler;
import com.jnhyxx.html5.utils.ToastUtil;
import com.jnhyxx.html5.utils.presenter.HoldingOrderPresenter;
import com.jnhyxx.html5.utils.presenter.IHoldingOrderView;
import com.jnhyxx.html5.view.BuySellVolumeLayout;
import com.jnhyxx.html5.view.ChartContainer;
import com.jnhyxx.html5.view.MarketDataView;
import com.jnhyxx.html5.view.TitleBar;
import com.jnhyxx.html5.view.TradePageHeader;
import com.jnhyxx.html5.view.dialog.SmartDialog;
import com.johnz.kutils.DateUtil;
import com.johnz.kutils.FinanceUtil;
import com.johnz.kutils.Launcher;
import com.johnz.kutils.StrUtil;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TradeActivity extends BaseActivity implements
        PlaceOrderFragment.Callback, AgreementFragment.Callback, IHoldingOrderView<HoldingOrder> {

    private static final int REQ_CODE_SET_LIGHTNING_ORDER_PAGE = 10000;
    private static final int REQ_CODE_LIVE = 321;

    @BindView(R.id.titleBar)
    TitleBar mTitleBar;
    @BindView(R.id.tradePageHeader)
    TradePageHeader mTradePageHeader;

    @BindView(R.id.openPrice)
    TextView mOpenPrice;
    @BindView(R.id.preClosePrice)
    TextView mPreClosePrice;
    @BindView(R.id.highestPrice)
    TextView mHighestPrice;
    @BindView(R.id.lowestPrice)
    TextView mLowestPrice;
    @BindView(R.id.chartContainer)
    ChartContainer mChartContainer;

    @BindView(R.id.lastPrice)
    TextView mLastPrice;
    @BindView(R.id.priceChange)
    TextView mPriceChange;
    @BindView(R.id.buySellVolumeLayout)
    BuySellVolumeLayout mBuySellVolumeLayout;

    @BindView(R.id.buyLongBtn)
    TextView mBuyLongBtn;
    @BindView(R.id.sellShortBtn)
    TextView mSellShortBtn;

    @BindView(R.id.holdingPositionTimeTo)
    TextView mHoldingPositionTimeTo;
    @BindView(R.id.nextTradeTime)
    TextView mNextTradeTime;
    @BindView(R.id.marketCloseArea)
    LinearLayout mMarketCloseArea;
    @BindView(R.id.marketOpenArea)
    LinearLayout mMarketOpenArea;

    @BindView(R.id.lightningOrderBtn)
    ImageView mLightningOrderBtn;

    private SlidingMenu mMenu;

    private Product mProduct;
    private int mFundType;
    private String mFundUnit;
    private List<Product> mProductList;
    private AnimationDrawable mQuestionMark;

    private boolean mUpdateRealTimeData;
    private boolean mProductChanged;

    private HoldingOrderPresenter mHoldingOrderPresenter;

    private FullMarketData mFullMarketData;

    private NettyHandler mNettyHandler = new NettyHandler<FullMarketData>() {
        @Override
        public void onReceiveData(FullMarketData data) {
            mFullMarketData = data;
            if (mUpdateRealTimeData) {
                updateFourMainPrices(data);
                updateLastPriceView(data);
                mBuySellVolumeLayout.setVolumes(data.getAskVolume(), data.getBidVolume());
                updateChartView(data);
                mHoldingOrderPresenter.setFullMarketData(data, mProduct.getVarietyId());
                updateBuyButtonsText(data);
            }
        }
    };

    //根据普通下单或者闪电下单改变买涨买跌按钮文字
    private void updateBuyButtonsText(FullMarketData data) {
        if (mLightningOrderBtn.isSelected()) {
            String lightningOrderBuyLong = getString(R.string.lightning_buy_long) + getFormattedPrice(data, true);
            mBuyLongBtn.setText(lightningOrderBuyLong);
            String lightningOrderBuyShort = getString(R.string.lightning_buy_short) + getFormattedPrice(data, false);
            mSellShortBtn.setText(lightningOrderBuyShort);
        } else {
            String buyLong = getString(R.string.buy_long) + getFormattedPrice(data, true);
            mBuyLongBtn.setText(buyLong);
            String sellShort = getString(R.string.sell_short) + getFormattedPrice(data, false);
            mSellShortBtn.setText(sellShort);
        }
    }

    private String getFormattedPrice(FullMarketData marketData, boolean askPrice) {
        if (marketData == null) return "";
        if (askPrice) {
            return FinanceUtil.formatWithScale(marketData.getAskPrice(), mProduct.getPriceDecimalScale());
        } else {
            return FinanceUtil.formatWithScale(marketData.getBidPrice(), mProduct.getPriceDecimalScale());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trade);
        ButterKnife.bind(this);

        mHoldingOrderPresenter = new HoldingOrderPresenter(this);
        mUpdateRealTimeData = true;

        initData(getIntent());
        initSlidingMenu();
        mTitleBar.setOnRightViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMenu.showMenu();
            }
        });
        mTradePageHeader.setOnViewClickListener(new TradePageHeader.OnViewClickListener() {
            @Override
            public void onSignInButtonClick() {
                Launcher.with(getActivity(), SignInActivity.class)
                        .executeForResult(REQ_CODE_LOGIN);
            }

            @Override
            public void onOrderListButtonClick() {
                openOrdersPage();
            }

            @Override
            public void onOneKeyClosePosButtonClick() {
                mHoldingOrderPresenter.closeAllHoldingPositions();
            }

            @Override
            public void onProfitAreaClick() {
                openOrdersPage();
            }
        });

        mTradePageHeader.setAvailableBalanceUnit(mFundUnit);
        mTradePageHeader.setTotalProfitUnit(mProduct.getCurrencyUnit()); // based on product

        mChartContainer.setLiveEnterVisible(mFundType == Product.FUND_TYPE_CASH);
        mChartContainer.setOnLiveEnterClickListener(new ChartContainer.OnLiveEnterClickListener() {
            @Override
            public void onClick() {
                switchToLivePage();
            }
        });

        updateTitleBar(); // based on product
        updateSignTradePagerHeader();
        updateChartView(); // based on product
        updateExchangeStatusView(); // based on product
        updateLightningOrderView(); // based on product
    }

    private void switchToLivePage() {
        if (getCallingActivity() != null
                && getCallingActivity().getClassName().equals(LiveActivity.class.getName())) {
            finish();
        } else {
            Launcher.with(getActivity(), LiveActivity.class).executeForResult(REQ_CODE_LIVE);
        }
    }

    private void updateLightningOrderView() {
        if (LocalUser.getUser().isLogin()) {
            if (LightningOrderAsset.isLightningOrderOpened(mProduct, mFundType)) {
                enableLightningOrderView(true);
                compareWithWebCache();
            } else {
                enableLightningOrderView(false);
                getLightningOrderWebCache();
            }
        }
    }

    private void getLightningOrderWebCache() {
        API.Market.getOrderAssetStoreStatus(mProduct.getVarietyId(), mFundType).setTag(TAG)
                .setCallback(new Callback2<Resp<LightningOrderAsset>, LightningOrderAsset>(false) {
                    @Override
                    public void onRespSuccess(LightningOrderAsset lightningOrderAsset) {
                        if (lightningOrderAsset != null) {
                            LightningOrderAsset.setLocalLightningOrder(mProduct, mFundType, lightningOrderAsset);
                            updateLightningOrderView();
                        }
                    }
                }).fire();
    }

    private void compareWithWebCache() {
        API.Order.getFuturesFinancing(mProduct.getVarietyId(), mFundType).setTag(TAG)
                .setCallback(new Callback2<Resp<FuturesFinancing>, FuturesFinancing>() {
                    @Override
                    public void onRespSuccess(FuturesFinancing futuresFinancing) {
                        if (futuresFinancing != null) {
                            LightningOrderAsset orderAsset = LightningOrderAsset.getLocalLightningOrderAsset(mProduct, mFundType);
                            boolean isValid = orderAsset.isValid(futuresFinancing);
                            if (isValid) {
                                enableLightningOrderView(true);
                            } else {
                                enableLightningOrderView(false);
                                showLightningOrderInvalidDialog();
                                removeLightningOrder();
                            }
                        }
                    }
                }).fireSync();
    }

    private void enableLightningOrderView(boolean enable) {
        mLightningOrderBtn.setSelected(enable);
        updateBuyButtonsText(mFullMarketData);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateQuestionMarker();
        updateExchangeStatusView(); // based on product

        startScheduleJob(60 * 1000, 60 * 1000);

        NettyClient.getInstance().addNettyHandler(mNettyHandler);
        NettyClient.getInstance().start(mProduct.getContractsCode());

        mHoldingOrderPresenter.onResume();
        mHoldingOrderPresenter.loadHoldingOrderList(mProduct.getVarietyId(), mFundType);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScheduleJob();

        NettyClient.getInstance().removeNettyHandler(mNettyHandler);
        NettyClient.getInstance().stop();

        mHoldingOrderPresenter.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHoldingOrderPresenter.onDestroy();
        mNettyHandler = null;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        } else {
            super.onBackPressed();
        }
    }

    private void showLightningOrderInvalidDialog() {
        SmartDialog.with(getActivity(),
                getString(R.string.lightning_orders_status_run_out))
                .setPositive(R.string.ok)
                .show();
    }

    private void removeLightningOrder() {
        LightningOrderAsset.setLocalLightningOrder(mProduct, mFundType, null);
        API.Market.removeOrderAssetStoreStatus(mProduct.getVarietyId(), mFundType)
                .setIndeterminate(this).setTag(TAG)
                .setCallback(new Callback1<Resp<JsonObject>>() {
                    @Override
                    protected void onRespSuccess(Resp<JsonObject> resp) {
                        Log.d(TAG, "removeLightningOrder: " + "remove web cache success");
                    }
                })
                .fire();
    }

    private void openOrdersPage() {
        Launcher.with(getActivity(), OrderActivity.class)
                .putExtra(Product.EX_PRODUCT, mProduct)
                .putExtra(Product.EX_FUND_TYPE, mFundType)
                .putExtra(FullMarketData.EX_MARKET_DATA, mFullMarketData)
                .execute();
    }

    private void updateSignTradePagerHeader() {
        if (LocalUser.getUser().isLogin()) {
            mTradePageHeader.showView(TradePageHeader.HEADER_AVAILABLE_BALANCE);
            mTradePageHeader.setAvailableBalance(
                    mFundType == Product.FUND_TYPE_CASH ?
                            LocalUser.getUser().getAvailableBalance() : LocalUser.getUser().getAvailableScore());
        } else {
            mTradePageHeader.showView(TradePageHeader.HEADER_UNLOGIN);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_LOGIN && resultCode == RESULT_OK) {
            updateSignTradePagerHeader();
            updateLightningOrderView();
        }
        //token失效重新登录回调
        if (requestCode == REQ_CODE_TOKEN_EXPIRED_LOGIN && resultCode == RESULT_OK) {
            updateLightningOrderView();
        }
        //闪电下单回调
        if (requestCode == REQ_CODE_SET_LIGHTNING_ORDER_PAGE && resultCode == RESULT_OK) {
            boolean isLightningOrderOpened = LightningOrderAsset.isLightningOrderOpened(mProduct, mFundType);
            enableLightningOrderView(isLightningOrderOpened);
            ToastUtil.curt(isLightningOrderOpened ? R.string.lightning_orders_open : R.string.lightning_orders_close);
        }
    }

    private void updateTitleBar() {
        View view = mTitleBar.getCustomView();
        TextView productName = (TextView) view.findViewById(R.id.productName);
        View productRule = view.findViewById(R.id.productRule);
        String productTitle = mFundType == Product.FUND_TYPE_CASH ? mProduct.getVarietyName() + " "
                : getString(R.string.gold) + " - " + mProduct.getVarietyName() + " ";
        productName.setText(StrUtil.mergeTextWithRatio(productTitle, mProduct.getContractsCode(), 0.75f));
        productRule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Launcher.with(getActivity(), WebViewActivity.class)
                        .putExtra(WebViewActivity.EX_TITLE, mProduct.getVarietyName() + getString(R.string.play_rule))
                        .putExtra(WebViewActivity.EX_URL, API.getTradeRule(mProduct.getVarietyId()))
                        .execute();
                Preference.get().setTradeRuleClicked(LocalUser.getUser().getPhone(), mProduct.getVarietyType());
            }
        });
        ImageView ruleIcon = (ImageView) view.findViewById(R.id.programmeArrow);
        mQuestionMark = (AnimationDrawable) ruleIcon.getBackground();
        updateQuestionMarker();
    }

    private void updateExchangeStatusView() {
        API.Order.getExchangeTradeStatus(mProduct.getExchangeId(), mProduct.getVarietyType())
                .setCallback(new Callback2<Resp<ExchangeStatus>, ExchangeStatus>() {
                    @Override
                    public void onRespSuccess(ExchangeStatus exchangeStatus) {
                        mProduct.setExchangeStatus(exchangeStatus.isTradeable()
                                ? Product.MARKET_STATUS_OPEN : Product.MARKET_STATUS_CLOSE);
                        if (exchangeStatus.isTradeable()) {
                            mMarketCloseArea.setVisibility(View.GONE);
                            mMarketOpenArea.setVisibility(View.VISIBLE);
                            mHoldingPositionTimeTo.setText(getString(R.string.prompt_holding_position_time_to,
                                    exchangeStatus.getNextTime()));
                        } else {
                            mMarketCloseArea.setVisibility(View.VISIBLE);
                            mMarketOpenArea.setVisibility(View.GONE);
                            mNextTradeTime.setText(getString(R.string.prompt_next_trade_time_is,
                                    exchangeStatus.getNextTime()));
                        }
                    }
                }).setTag(TAG).fireSync();
    }

    private void initData(Intent intent) {
        mProduct = intent.getParcelableExtra(Product.EX_PRODUCT);
        mFundType = intent.getIntExtra(Product.EX_FUND_TYPE, 0);
        mFundUnit = (mFundType == Product.FUND_TYPE_CASH ? Unit.YUAN : Unit.GOLD);
        mProductList = intent.getParcelableArrayListExtra(Product.EX_PRODUCT_LIST);
    }

    @OnClick({R.id.buyLongBtn, R.id.sellShortBtn, R.id.lightningOrderBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buyLongBtn:
                if (mLightningOrderBtn.isSelected()) {
                    placeLightningOrder(LightningOrderAsset.TYPE_BUY_LONG);
                } else {
                    placeOrder(PlaceOrderFragment.TYPE_BUY_LONG);
                }

                break;
            case R.id.sellShortBtn:
                if (mLightningOrderBtn.isSelected()) {
                    placeLightningOrder(LightningOrderAsset.TYPE_SELL_SHORT);
                } else {
                    placeOrder(PlaceOrderFragment.TYPE_SELL_SHORT);
                }
                break;
            case R.id.lightningOrderBtn:
                openLightningOrdersPage();
                break;
        }
    }

    private void openLightningOrdersPage() {
        if (!LocalUser.getUser().isLogin()) {
            Launcher.with(getActivity(), SignInActivity.class).executeForResult(REQ_CODE_LOGIN);
            return;
        }

        if (mFundType == Product.FUND_TYPE_CASH) {
            String userPhone = LocalUser.getUser().getPhone();
            if (Preference.get().hadShowTradeAgreement(userPhone, mProduct.getVarietyType())) {
                openSetLightningOrderPage();
            } else {
                showAgreementFragment(LightningOrderAsset.TAG_OPEN_ARRGE_FRAGMENT_PAGE);
            }
        } else {
            openSetLightningOrderPage();
        }
    }

    //设置闪电下单的提交单
    private void placeLightningOrder(int buyType) {
        LightningOrderAsset orderAsset = LightningOrderAsset.getLocalLightningOrderAsset(mProduct, mFundType);
        SubmittedOrder submittedOrder = orderAsset.getSubmittedOrder(mProduct, mFundType, buyType, mFullMarketData);
        submitOrder(submittedOrder);
    }

    private void openSetLightningOrderPage() {
        Launcher.with(getActivity(), SetLightningOrdersActivity.class)
                .putExtra(Product.EX_PRODUCT, mProduct)
                .putExtra(Product.EX_FUND_TYPE, mFundType)
                .executeForResult(REQ_CODE_SET_LIGHTNING_ORDER_PAGE);

    }

    private void updateChartView(FullMarketData data) {
        TrendView trendView = mChartContainer.getTrendView();
        if (trendView != null) {
            List<TrendViewData> dataList = trendView.getDataList();
            if (dataList != null && dataList.size() > 0) {
                TrendViewData lastData = dataList.get(dataList.size() - 1);
                String date = DateUtil.addOneMinute(lastData.getDate(), TrendViewData.DATE_FORMAT);
                TrendView.Settings settings = trendView.getSettings();
                if (TrendView.Util.isValidDate(date, settings.getOpenMarketTimes())) {
                    float lastPrice = (float) data.getLastPrice();
                    TrendViewData unstableData = new TrendViewData(lastData.getContractId(), lastPrice, date);
                    trendView.setUnstableData(unstableData);
                }
            }
        }

        FlashView flashView = mChartContainer.getFlashView();
        if (flashView != null) {
            flashView.addData(new FlashViewData((float) data.getLastPrice()));
        }

        MarketDataView marketDataView = mChartContainer.getMarketDataView();
        if (marketDataView != null) {
            marketDataView.setMarketData(data, mProduct);
        }
    }

    private void updateFourMainPrices(FullMarketData data) {
        int scale = mProduct.getPriceDecimalScale();
        mOpenPrice.setText(getString(R.string.today_open, FinanceUtil.formatWithScale(data.getOpenPrice(), scale)));
        mPreClosePrice.setText(getString(R.string.pre_close, FinanceUtil.formatWithScale(data.getPreClsPrice(), scale)));
        mHighestPrice.setText(getString(R.string.highest, FinanceUtil.formatWithScale(data.getHighestPrice(), scale)));
        mLowestPrice.setText(getString(R.string.lowest, FinanceUtil.formatWithScale(data.getLowestPrice(), scale)));
    }

    private void updateLastPriceView(FullMarketData data) {
        mLastPrice.setText(FinanceUtil.formatWithScale(data.getLastPrice(), mProduct.getPriceDecimalScale()));
        double priceChangeValue = data.getLastPrice() - data.getPreSetPrice();
        double priceChangePercent = priceChangeValue / data.getPreSetPrice() * 100;
        int textColor;
        if (priceChangeValue >= 0) {
            textColor = ContextCompat.getColor(getActivity(), R.color.redPrimary);
            mLastPrice.setTextColor(textColor);
            mPriceChange.setTextColor(textColor);
            String priceChangeStr = "+" + FinanceUtil.formatWithScale(priceChangeValue, mProduct.getPriceDecimalScale())
                    + "\n+" + FinanceUtil.formatWithScale(priceChangePercent) + "%";
            mPriceChange.setText(priceChangeStr);
        } else {
            textColor = ContextCompat.getColor(getActivity(), R.color.greenPrimary);
            mLastPrice.setTextColor(textColor);
            mPriceChange.setTextColor(textColor);
            String priceChangeStr = FinanceUtil.formatWithScale(priceChangeValue, mProduct.getPriceDecimalScale())
                    + "\n" + FinanceUtil.formatWithScale(priceChangePercent) + "%";
            mPriceChange.setText(priceChangeStr);
        }
    }


    private void updateQuestionMarker() {
        String userPhone = LocalUser.getUser().getPhone();
        if (Preference.get().isTradeRuleClicked(userPhone, mProduct.getVarietyType())) {
            mQuestionMark.stop();
        } else {
            mQuestionMark.start();
        }
    }

    @Override
    public void onTimeUp(int count) {
        requestTrendDataAndSet();
    }

    private void updateChartView() {
        TrendView trendView = mChartContainer.getTrendView();
        if (trendView == null) {
            trendView = new TrendView(this);
            mChartContainer.addTrendView(trendView);
        }
        trendView.clearData();
        TrendView.Settings settings = new TrendView.Settings();
        settings.setBaseLines(mProduct.getBaseline());
        settings.setNumberScale(mProduct.getPriceDecimalScale());
        settings.setOpenMarketTimes(mProduct.getOpenMarketTime());
        settings.setDisplayMarketTimes(mProduct.getDisplayMarketTimes());
        settings.setLimitUpPercent((float) mProduct.getLimitUpPercent());
        settings.setCalculateXAxisFromOpenMarketTime(true);
        trendView.setSettings(settings);

        FlashView flashView = mChartContainer.getFlashView();
        if (flashView == null) {
            flashView = new FlashView(this);
            mChartContainer.addFlashView(flashView);
        }
        flashView.clearData();
        FlashView.Settings settings1 = new FlashView.Settings();
        settings1.setFlashChartPriceInterval(mProduct.getFlashChartPriceInterval());
        settings1.setNumberScale(mProduct.getPriceDecimalScale());
        settings1.setBaseLines(mProduct.getBaseline());
        flashView.setSettings(settings1);

        MarketDataView marketDataView = mChartContainer.getMarketDataView();
        if (marketDataView == null) {
            marketDataView = new MarketDataView(this);
            mChartContainer.addMarketDataView(marketDataView);
        }
        marketDataView.clearData();

        KlineView klineView = mChartContainer.getKlineView();
        if (klineView == null) {
            klineView = new KlineView(this);
            mChartContainer.addKlineView(klineView);
        }
        klineView.clearData();
        KlineView.Settings settings2 = new KlineView.Settings();
        settings2.setBaseLines(mProduct.getBaseline());
        settings2.setNumberScale(mProduct.getPriceDecimalScale());
        settings2.setXAxis(40);
        settings2.setIndexesType(KlineView.Settings.INDEXES_VOL);
        klineView.setSettings(settings2);

        mChartContainer.showTrendView();

        // request Trend Data
        requestTrendDataAndSet();
        // request Kline Data
        requestKlineDataAndSet();
    }

    private void requestTrendDataAndSet() {
        API.getTrendData(mProduct.getVarietyType())
                .setCallback(new Callback<String>() {
                    @Override
                    public void onReceive(String s) {
                        TrendView trendView = mChartContainer.getTrendView();
                        if (trendView == null) return;
                        TrendView.Settings settings = trendView.getSettings();
                        trendView.setDataList(TrendView.Util.createDataList(s, settings.getOpenMarketTimes()));
                    }
                }).fireSync();
    }

    private void requestKlineDataAndSet() {
        API.getKlineData(mProduct.getContractsCode(), "")
                .setCallback(new Callback2<Resp<List<KlineViewData>>, List<KlineViewData>>() {
                    @Override
                    public void onRespSuccess(List<KlineViewData> klineDataList) {
                        if (klineDataList != null && klineDataList.size() > 0) {
                            KlineView klineView = mChartContainer.getKlineView();
                            Collections.reverse(klineDataList);
                            if (klineView == null) return;
                            klineView.setDataList(klineDataList);
                        }
                    }
                }).fireSync();
    }

    private void initSlidingMenu() {
        mMenu = new SlidingMenu(this);
        mMenu.setMode(SlidingMenu.RIGHT);
        mMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
        mMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        mMenu.setBehindWidthRes(R.dimen.sliding_menu_width);
        mMenu.setMenu(R.layout.sm_behind_menu);
        mMenu.setSecondaryOnOpenListner(new SlidingMenu.OnOpenListener() {
            @Override
            public void onOpen() {
                mUpdateRealTimeData = false;
            }
        });
        mMenu.setOnClosedListener(new SlidingMenu.OnClosedListener() {
            @Override
            public void onClosed() {
                if (mProductChanged) {
                    NettyClient.getInstance().stop();

                    hideFragmentOfContainer();
                    updateChartView(); // based on product

                    mHoldingOrderPresenter.clearData();
                    mHoldingOrderPresenter.loadHoldingOrderList(mProduct.getVarietyId(), mFundType);

                    NettyClient.getInstance().start(mProduct.getContractsCode());
                    mProductChanged = false;
                }
                mUpdateRealTimeData = true;
            }
        });
        ListView listView = (ListView) mMenu.getMenu();
        MenuAdapter menuAdapter = new MenuAdapter(this);
        menuAdapter.addAll(mProductList);
        listView.setAdapter(menuAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Product product = (Product) adapterView.getItemAtPosition(position);
                if (product != null) {
                    if (product.getVarietyId() == mProduct.getVarietyId()) {
                        mProductChanged = false;
                        mMenu.toggle();
                    } else {
                        mProduct = product;
                        mProductChanged = true;
                        mMenu.toggle();

                        updateTitleBar(); // based on product
                        updateExchangeStatusView(); // based on product
                        mTradePageHeader.setTotalProfitUnit(mProduct.getCurrencyUnit()); // based on product
                        updateLightningOrderView(); // based on product
                    }
                }
            }
        });
    }

    private void placeOrder(int longOrShort) {
        if (!LocalUser.getUser().isLogin()) {
            Launcher.with(getActivity(), SignInActivity.class).executeForResult(REQ_CODE_LOGIN);
            return;
        }

        if (mFundType == Product.FUND_TYPE_CASH) {
            String userPhone = LocalUser.getUser().getPhone();
            if (Preference.get().hadShowTradeAgreement(userPhone, mProduct.getVarietyType())) {
                showPlaceOrderFragment(longOrShort);
            } else {
                showAgreementFragment(longOrShort);
            }
        } else {
            showPlaceOrderFragment(longOrShort);
        }
    }

    private void showAgreementFragment(int longOrShort) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.placeOrderContainer, AgreementFragment.newInstance(longOrShort))
                    .commit();
        }
    }

    private void showPlaceOrderFragment(int longOrShort) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.placeOrderContainer,
                            PlaceOrderFragment.newInstance(longOrShort, mProduct, mFundType, mFullMarketData))
                    .commit();
        }
    }

    private void hideFragmentOfContainer() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    private void submitOrder(final SubmittedOrder submittedOrder) {
        API.Order.submitOrder(submittedOrder).setTag(TAG).setIndeterminate(this)
                .setCallback(new Callback<Resp<JsonObject>>() {
                    @Override
                    public void onReceive(Resp<JsonObject> jsonObjectResp) {
                        if (jsonObjectResp.isSuccess()) {
                            hideFragmentOfContainer();
                            SmartDialog.with(getActivity(), jsonObjectResp.getMsg())
                                    .setCancelListener(new SmartDialog.OnCancelListener() {
                                        @Override
                                        public void onCancel(Dialog dialog) {
                                            dialog.dismiss();
                                            openOrdersPage();
                                        }
                                    })
                                    .setPositive(R.string.ok, new SmartDialog.OnClickListener() {
                                        @Override
                                        public void onClick(Dialog dialog) {
                                            dialog.dismiss();
                                            openOrdersPage();
                                        }
                                    })
                                    .show();
                            mHoldingOrderPresenter.loadHoldingOrderList(mProduct.getVarietyId(), mFundType);
                        } else if (jsonObjectResp.getCode() == Resp.CODE_FUND_NOT_ENOUGH) {
                            showFundNotEnoughDialog(jsonObjectResp);
                        } else if (jsonObjectResp.getCode() == Resp.CODE_LIGHTNING_ORDER_INVALID) {
                            enableLightningOrderView(false);
                            showLightningOrderInvalidDialog();
                            removeLightningOrder();
                        } else {
                            SmartDialog.with(getActivity(), jsonObjectResp.getMsg())
                                    .setPositive(R.string.ok)
                                    .show();
                        }
                    }
                }).fire();
    }

    private void showFundNotEnoughDialog(Resp<JsonObject> jsonObjectResp) {
        if (mFundType == Product.FUND_TYPE_CASH) {
            SmartDialog.with(getActivity(), jsonObjectResp.getMsg())
                    .setPositive(R.string.go_to_recharge,
                            new SmartDialog.OnClickListener() {
                                @Override
                                public void onClick(Dialog dialog) {
                                    dialog.dismiss();
                                    Launcher.with(getActivity(), RechargeActivity.class)
                                            .execute();
                                }
                            }).setNegative(R.string.cancel)
                    .show();
        } else {
            SmartDialog.with(getActivity(), jsonObjectResp.getMsg())
                    .setPositive(R.string.ok)
                    .show();
        }
    }

    @Override
    public void onPlaceOrderFragmentConfirmBtnClick(SubmittedOrder submittedOrder) {
        submittedOrder.setPayType(mFundType);
        submitOrder(submittedOrder);
    }

    @Override
    public void onPlaceOrderFragmentEmptyAreaClick() {
        hideFragmentOfContainer();
    }

    @Override
    public void onPlaceOrderFragmentShow() {
        mUpdateRealTimeData = false;
    }

    @Override
    public void onPlaceOrderFragmentExited() {
        mUpdateRealTimeData = true;
    }

    @Override
    public void onAgreementFragmentAgreeBtnClick(int longOrShort) {
        String userPhone = LocalUser.getUser().getPhone();
        Preference.get().setTradeAgreementShowed(userPhone, mProduct.getVarietyType());
        if (longOrShort == LightningOrderAsset.TAG_OPEN_ARRGE_FRAGMENT_PAGE) {
            hideFragmentOfContainer();
            openLightningOrdersPage();
            return;
        }
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.placeOrderContainer, PlaceOrderFragment.newInstance(longOrShort, mProduct, mFundType, mFullMarketData))
                    .commit();
        }
    }

    @Override
    public void onAgreementFragmentEmptyAreaClick() {
        hideFragmentOfContainer();
    }

    @Override
    public void onAgreementFragmentShow() {
        mUpdateRealTimeData = false;
    }

    @Override
    public void onAgreementFragmentExited() {
        mUpdateRealTimeData = true;
    }

    @Override
    public void onShowHoldingOrderList(List<HoldingOrder> holdingOrderList) {
        updateUsableMoneyScore(new LocalUser.Callback() {
            @Override
            public void onUpdateCompleted() {
            }
        });
    }

    @Override
    public void onShowTotalProfit(boolean hasHoldingOrders, double totalProfit, double ratio) {
        if (hasHoldingOrders) {
            mTradePageHeader.showView(TradePageHeader.HEADER_HOLDING_POSITION);
            mTradePageHeader.setTotalProfit(totalProfit, mProduct.isForeign(),
                    mProduct.getLossProfitScale(), ratio, mFundUnit);
        } else {
            updateSignTradePagerHeader();
        }
    }

    @Override
    public void onSubmitAllHoldingOrdersCompleted(String message) {
        SmartDialog.with(getActivity(),
                getString(R.string.sell_order_submit_successfully) + "\n" + message)
                .setPositive(R.string.ok)
                .show();
    }

    @Override
    public void onSubmitHoldingOrderCompleted(HoldingOrder holdingOrder) {
    }

    @Override
    public void onRiskControlTriggered(String closingOrders, String orderSplit, String stopLossSplit) {
    }


    static class MenuAdapter extends ArrayAdapter<Product> {

        public MenuAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_sliding_menu, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.bindingData(getItem(position));
            return convertView;
        }

        static class ViewHolder {
            @BindView(R.id.productName)
            TextView mProductName;
            @BindView(R.id.productCode)
            TextView mProductCode;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            public void bindingData(Product item) {
                mProductName.setText(item.getVarietyName());
                mProductCode.setText(item.getVarietyType());
            }
        }
    }
}
