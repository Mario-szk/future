package com.jnhyxx.html5.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jnhyxx.chart.TrendView;
import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.order.OrderActivity;
import com.jnhyxx.html5.domain.local.User;
import com.jnhyxx.html5.domain.market.Product;
import com.jnhyxx.html5.domain.order.ExchangeStatus;
import com.jnhyxx.html5.fragment.PlaceOrderFragment;
import com.jnhyxx.html5.net.API;
import com.jnhyxx.html5.net.Callback;
import com.jnhyxx.html5.view.BuySellVolumeLayout;
import com.jnhyxx.html5.view.ChartContainer;
import com.jnhyxx.html5.view.TitleBar;
import com.jnhyxx.html5.view.TradePageHeader;
import com.johnz.kutils.Launcher;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TradeActivity extends BaseActivity implements PlaceOrderFragment.Callback {

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

    @BindView(R.id.placeOrderContainer)
    FrameLayout mPlaceOrderContainer;

    private SlidingMenu mMenu;

    private Product mProduct;
    private int mFundType;
    private List<Product> mProductList;
    private ExchangeStatus mExchangeStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trade);
        ButterKnife.bind(this);

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

            }

            @Override
            public void onOrderListButtonClick() {
                Launcher.with(getActivity(), OrderActivity.class)
                        .putExtra(Product.EX_PRODUCT, mProduct)
                        .putExtra(Product.EX_FUND_TYPE, mFundType)
                        .execute();
            }

            @Override
            public void onOneKeyClosePosButtonClick() {

            }

            @Override
            public void onProfitAreaClick() {

            }
        });

        updateChartView();
        updateExchangeStatusView();
    }

    private void updateExchangeStatusView() {
        if (mExchangeStatus.isTradeable()) {
            mMarketCloseArea.setVisibility(View.GONE);
            mMarketOpenArea.setVisibility(View.VISIBLE);
            mHoldingPositionTimeTo.setText(getString(R.string.prompt_holding_position_time_to,
                    mExchangeStatus.getNextTime()));
        } else {
            mMarketCloseArea.setVisibility(View.VISIBLE);
            mMarketOpenArea.setVisibility(View.GONE);
            mNextTradeTime.setText(getString(R.string.next_trade_time_is,
                    mExchangeStatus.getNextTime()));
        }
    }

    private void initData(Intent intent) {
        mProduct = (Product) intent.getSerializableExtra(Product.EX_PRODUCT);
        mFundType = intent.getIntExtra(Product.EX_FUND_TYPE, 0);
        mProductList = intent.getParcelableArrayListExtra(Product.EX_PRODUCT_LIST);
        mExchangeStatus = (ExchangeStatus) intent.getSerializableExtra(ExchangeStatus.EX_EXCHANGE_STATUS);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (User.getUser().isLogin()) {
            mTradePageHeader.showView(TradePageHeader.HEADER_AVAILABLE_BALANCE);
            mTradePageHeader.setAvailableBalance(User.getUser().getAvailableBalance());
        } else {
            mTradePageHeader.showView(TradePageHeader.HEADER_UNLOGIN);
        }

        startScheduleJob(60 * 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScheduleJob();
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
        TrendView.Settings settings = new TrendView.Settings();
        settings.setBaseLines(mProduct.getBaseline());
        settings.setNumberScale(mProduct.getDecimalScale());
        settings.setOpenMarketTimes(mProduct.getOpenMarketTime());
        settings.setDisplayMarketTimes(mProduct.getDisplayMarketTimes());
        settings.setLimitUpPercent((float) mProduct.getLimitUpPercent());
        settings.setCalculateXAxisFromOpenMarketTime(true);
        trendView.setSettings(settings);
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
                }).fire();
    }

    private void initSlidingMenu() {
        mMenu = new SlidingMenu(this);
        mMenu.setMode(SlidingMenu.RIGHT);
        mMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
        mMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        mMenu.setBehindWidthRes(R.dimen.sliding_menu_width);
        mMenu.setMenu(R.layout.sm_behind_menu);
        ListView listView = (ListView) mMenu.getMenu();
        MenuAdapter menuAdapter = new MenuAdapter(this);
        menuAdapter.addAll(mProductList);
        listView.setAdapter(menuAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Product product = (Product) adapterView.getItemAtPosition(position);
                if (product != null) {
                    mProduct = product;
                    updateChartView();
                }
            }
        });
    }

    @OnClick({R.id.buyLongBtn, R.id.sellShortBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buyLongBtn:
                showPlaceOrderFragment(PlaceOrderFragment.TYPE_BUY_LONG);
                break;
            case R.id.sellShortBtn:
                showPlaceOrderFragment(PlaceOrderFragment.TYPE_SELL_SHORT);
                break;
        }
    }

    private void showPlaceOrderFragment(int type) {
        mPlaceOrderContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.placeOrderContainer, PlaceOrderFragment.newInstance(type))
                .commit();
    }

    private void hidePlaceOrderFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.placeOrderContainer);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }
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

    @Override
    public void onConfirmBtnClick() {
        // TODO: 8/23/16 下单接口 实现

        hidePlaceOrderFragment();
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
