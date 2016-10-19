package com.jnhyxx.html5.fragment.order;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jnhyxx.html5.R;
import com.jnhyxx.html5.domain.market.FullMarketData;
import com.jnhyxx.html5.domain.market.Product;
import com.jnhyxx.html5.domain.order.HoldingOrder;
import com.jnhyxx.html5.fragment.BaseFragment;
import com.jnhyxx.html5.netty.NettyClient;
import com.jnhyxx.html5.netty.NettyHandler;
import com.jnhyxx.html5.utils.presenter.OrderPresenter;
import com.johnz.kutils.FinanceUtil;

import java.math.BigDecimal;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.jnhyxx.html5.R.id.buyOrSell;
import static com.jnhyxx.html5.R.id.hands;

public class HoldingFragment extends BaseFragment
        implements OrderPresenter.IHoldingOrderView {

    @BindView(android.R.id.list)
    ListView mList;
    @BindView(R.id.totalProfitAndUnit)
    TextView mTotalProfitAndUnit;
    @BindView(R.id.totalProfit)
    TextView mTotalProfit;
    @BindView(R.id.oneKeyClosePositionBtn)
    TextView mOneKeyClosePositionBtn;
    @BindView(android.R.id.empty)
    LinearLayout mEmpty;
    @BindView(R.id.lossProfitArea)
    RelativeLayout mLossProfitArea;
    @BindView(R.id.totalProfitRmb)
    TextView mTotalProfitRmb;

    private Unbinder mBinder;

    private Product mProduct;
    private int mFundType;
    private HoldingOrderAdapter mHoldingOrderAdapter;

    private NettyHandler mNettyHandler = new NettyHandler() {
        @Override
        protected void onReceiveData(FullMarketData data) {
            OrderPresenter.getInstance().setFullMarketData(data);
            if (mHoldingOrderAdapter != null) {
                mHoldingOrderAdapter.setFullMarketData(data);
            }
            updateHoldingOrderVisibleItems(data);
        }
    };

    private void updateHoldingOrderVisibleItems(FullMarketData data) {
        if (mList != null && mHoldingOrderAdapter != null) {
            int first = mList.getFirstVisiblePosition();
            int last = mList.getLastVisiblePosition();
            for (int i = first; i <= last; i++) {
                HoldingOrder holdingOrder = (HoldingOrder) mHoldingOrderAdapter.getItem(i);
                View itemView = mList.getChildAt(i - mList.getFirstVisiblePosition());
                TextView lastPrice = ButterKnife.findById(itemView, R.id.lastPrice);
                TextView lossProfit = ButterKnife.findById(itemView, R.id.lossProfit);
                TextView lossProfitRmb = ButterKnife.findById(itemView, R.id.lossProfitRmb);

                int priceScale = mProduct.getPriceDecimalScale();
                int profitScale = mProduct.getLossProfitScale();
                double ratio = holdingOrder.getRatio();
                BigDecimal eachPointMoney = new BigDecimal(holdingOrder.getEachPointMoney());
                BigDecimal diff;
                if (holdingOrder.getDirection() == HoldingOrder.DIRECTION_LONG) {
                    lastPrice.setText(FinanceUtil.formatWithScale(data.getBidPrice(), priceScale));
                    diff = FinanceUtil.subtraction(data.getBidPrice(), holdingOrder.getRealAvgPrice());
                } else {
                    lastPrice.setText(FinanceUtil.formatWithScale(data.getAskPrice(), priceScale));
                    diff = FinanceUtil.subtraction(holdingOrder.getRealAvgPrice(), data.getAskPrice());
                }
                diff = diff.multiply(eachPointMoney);

                String lossProfitStr;
                String lossProfitRmbStr;
                double diffRmb = diff.multiply(new BigDecimal(ratio)).doubleValue();
                if (diff.doubleValue() >= 0) {
                    lossProfit.setTextColor(ContextCompat.getColor(getContext(), R.color.redPrimary));
                    lossProfitStr = "+" + FinanceUtil.formatWithScale(diff.doubleValue(), profitScale);
                    lossProfitRmbStr = "(+" + FinanceUtil.formatWithScale(diffRmb) + ")";
                } else {
                    lossProfit.setTextColor(ContextCompat.getColor(getContext(), R.color.greenPrimary));
                    lossProfitStr = FinanceUtil.formatWithScale(diff.doubleValue(), profitScale);
                    lossProfitRmbStr = "(" + FinanceUtil.formatWithScale(diffRmb) + ")";
                }
                lossProfit.setText(lossProfitStr);
                lossProfitRmb.setText(lossProfitRmbStr);
            }
        }
    }

    public static HoldingFragment newInstance(Product product, int fundType) {
        HoldingFragment fragment = new HoldingFragment();
        Bundle args = new Bundle();
        args.putSerializable(Product.EX_PRODUCT, product);
        args.putInt(Product.EX_FUND_TYPE, fundType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProduct = (Product) getArguments().getSerializable(Product.EX_PRODUCT);
            mFundType = getArguments().getInt(Product.EX_FUND_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_holding, container, false);
        mBinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinder.unbind();
        NettyClient.getInstance().removeNettyHandler(mNettyHandler);
        mNettyHandler = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        OrderPresenter.getInstance().register(this);
        NettyClient.getInstance().start(mProduct.getContractsCode());
    }

    @Override
    public void onPause() {
        super.onPause();
        OrderPresenter.getInstance().unregister(this);
        NettyClient.getInstance().stop();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mList.setEmptyView(mEmpty);
        mTotalProfitAndUnit.setText(getString(R.string.holding_position_total_profit_and_unit,
                mProduct.getCurrencyUnit()));

        onShowHoldingOrderList(OrderPresenter.getInstance().getHoldingOrderList());
        OrderPresenter.getInstance().loadHoldingOrderList(mProduct.getVarietyId(), mFundType);

        NettyClient.getInstance().addNettyHandler(mNettyHandler);
    }

    @Override
    public void onShowHoldingOrderList(List<HoldingOrder> holdingOrderList) {
        if (holdingOrderList != null) {
            if (mHoldingOrderAdapter == null) {
                mHoldingOrderAdapter = new HoldingOrderAdapter(getContext(), mProduct, holdingOrderList);
                mHoldingOrderAdapter.setCallback(new HoldingOrderAdapter.Callback() {
                    @Override
                    public void onItemClosePositionClick(HoldingOrder order) {
                        OrderPresenter.getInstance().closePosition(mFundType, order);
                    }
                });
                mList.setAdapter(mHoldingOrderAdapter);
            } else {
                mHoldingOrderAdapter.setHoldingOrderList(holdingOrderList);
            }
        }
    }

    @Override
    public void onShowTotalProfit(boolean hasHoldingOrders, double totalProfit, double ratio) {
        if (hasHoldingOrders) {
            if (mLossProfitArea.getVisibility() == View.GONE) {
                mLossProfitArea.setVisibility(View.VISIBLE);
            }

            int scale = mProduct.getLossProfitScale();
            String totalProfitStr;
            String totalProfitRmbStr;
            if (totalProfit >= 0) {
                mTotalProfit.setTextColor(ContextCompat.getColor(getContext(), R.color.redPrimary));
                mOneKeyClosePositionBtn.setBackgroundResource(R.drawable.btn_red_one_key_close);
                totalProfitStr = "+" + FinanceUtil.formatWithScale(totalProfit, scale);
                totalProfitRmbStr = "+" + FinanceUtil.formatWithScale(totalProfit * ratio);
            } else {
                mTotalProfit.setTextColor(ContextCompat.getColor(getContext(), R.color.greenPrimary));
                mOneKeyClosePositionBtn.setBackgroundResource(R.drawable.btn_green_one_key_close);
                totalProfitStr = FinanceUtil.formatWithScale(totalProfit, scale);
                totalProfitRmbStr = FinanceUtil.formatWithScale(totalProfit * ratio);
            }
            mTotalProfit.setText(totalProfitStr);
            if (mProduct.isForeign()) {
                mTotalProfitRmb.setText("(" + totalProfitRmbStr + FinanceUtil.UNIT_YUAN + ")");
            }

        } else {
            if (mLossProfitArea.getVisibility() == View.VISIBLE) {
                mLossProfitArea.setVisibility(View.GONE);
            }
        }
    }

    @OnClick(R.id.oneKeyClosePositionBtn)
    public void onClick() {
        OrderPresenter.getInstance().closeAllHoldingPositions(mFundType);
    }

    static class HoldingOrderAdapter extends BaseAdapter {

        public interface Callback {
            void onItemClosePositionClick(HoldingOrder order);
        }

        private Context mContext;
        private Product mProduct;
        private List<HoldingOrder> mHoldingOrderList;
        private FullMarketData mFullMarketData;
        private Callback mCallback;

        public HoldingOrderAdapter(Context context, Product product, List<HoldingOrder> holdingOrderList) {
            mContext = context;
            mProduct = product;
            mHoldingOrderList = holdingOrderList;
        }

        public void setFullMarketData(FullMarketData fullMarketData) {
            mFullMarketData = fullMarketData;
        }

        public void setHoldingOrderList(List<HoldingOrder> holdingOrderList) {
            mHoldingOrderList = holdingOrderList;
            notifyDataSetChanged();
        }

        public void setCallback(Callback callback) {
            mCallback = callback;
        }

        @Override
        public int getCount() {
            return mHoldingOrderList.size();
        }

        @Override
        public Object getItem(int position) {
            return mHoldingOrderList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_holding_order, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.bindingData((HoldingOrder) getItem(position), mProduct, mFullMarketData,
                    mContext, mCallback);

            return convertView;
        }

        static class ViewHolder {
            @BindView(buyOrSell)
            TextView mBuyOrSell;
            @BindView(hands)
            TextView mHands;
            @BindView(R.id.lossProfit)
            TextView mLossProfit;
            @BindView(R.id.lossProfitRmb)
            TextView mLossProfitRmb;
            @BindView(R.id.buyPrice)
            TextView mBuyPrice;
            @BindView(R.id.stopProfit)
            TextView mStopProfit;
            @BindView(R.id.lastPrice)
            TextView mLastPrice;
            @BindView(R.id.stopLoss)
            TextView mStopLoss;
            @BindView(R.id.closePositionButton)
            TextView mClosePositionButton;
            @BindView(R.id.orderStatus)
            TextView mOrderStatus;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            public void bindingData(final HoldingOrder item, Product product, FullMarketData data,
                                    Context context, final Callback callback) {

                mBuyPrice.setText(FinanceUtil.formatWithScale(item.getRealAvgPrice(), product.getPriceDecimalScale()));
                mStopProfit.setText(FinanceUtil.formatWithScale(item.getStopWin(), product.getLossProfitScale())
                        + product.getCurrencyUnit());
                mStopLoss.setText(FinanceUtil.formatWithScale(item.getStopLoss(), product.getLossProfitScale())
                        + product.getCurrencyUnit());
                mHands.setText(item.getHandsNum() + "手");
                mClosePositionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (callback != null) {
                            callback.onItemClosePositionClick(item);
                        }
                    }
                });
                if (item.getDirection() == HoldingOrder.DIRECTION_LONG) {
                    mBuyOrSell.setText(R.string.buy_long);
                    mBuyOrSell.setTextColor(ContextCompat.getColor(context, R.color.redPrimary));
                    mHands.setTextColor(ContextCompat.getColor(context, R.color.redPrimary));
                } else {
                    mBuyOrSell.setText(R.string.sell_short);
                    mBuyOrSell.setTextColor(ContextCompat.getColor(context, R.color.greenPrimary));
                    mHands.setTextColor(ContextCompat.getColor(context, R.color.greenPrimary));
                }
                if (item.getOrderStatus() == HoldingOrder.ORDER_STATUS_HOLDING) {
                    mClosePositionButton.setVisibility(View.VISIBLE);
                    mOrderStatus.setVisibility(View.GONE);
                } else {
                    mClosePositionButton.setVisibility(View.GONE);
                    mOrderStatus.setVisibility(View.VISIBLE);
                    if (item.getOrderStatus() < HoldingOrder.ORDER_STATUS_HOLDING) {
                        mOrderStatus.setText(R.string.buying);
                    } else if (item.getOrderStatus() > HoldingOrder.ORDER_STATUS_HOLDING) {
                        mOrderStatus.setText(R.string.selling);
                    }
                }

                // views will change
                if (data != null) {
                    int priceScale = product.getPriceDecimalScale();
                    int profitScale = product.getLossProfitScale();
                    double ratio = item.getRatio();
                    BigDecimal eachPointMoney = new BigDecimal(item.getEachPointMoney());
                    BigDecimal diff;
                    if (item.getDirection() == HoldingOrder.DIRECTION_LONG) {
                        mLastPrice.setText(FinanceUtil.formatWithScale(data.getBidPrice(), priceScale));
                        diff = FinanceUtil.subtraction(data.getBidPrice(), item.getRealAvgPrice());
                    } else {
                        mLastPrice.setText(FinanceUtil.formatWithScale(data.getAskPrice(), priceScale));
                        diff = FinanceUtil.subtraction(item.getRealAvgPrice(), data.getAskPrice());
                    }
                    diff = diff.multiply(eachPointMoney);

                    String lossProfitStr;
                    String lossProfitRmbStr;
                    double diffRmb = diff.multiply(new BigDecimal(ratio)).doubleValue();
                    if (diff.doubleValue() >= 0) {
                        mLossProfit.setTextColor(ContextCompat.getColor(context, R.color.redPrimary));
                        lossProfitStr = "+" + FinanceUtil.formatWithScale(diff.doubleValue(), profitScale);
                        lossProfitRmbStr = "(+" + FinanceUtil.formatWithScale(diffRmb) + ")";
                    } else {
                        mLossProfit.setTextColor(ContextCompat.getColor(context, R.color.greenPrimary));
                        lossProfitStr = FinanceUtil.formatWithScale(diff.doubleValue(), profitScale);
                        lossProfitRmbStr = "(" + FinanceUtil.formatWithScale(diffRmb) + ")";
                    }
                    mLossProfit.setText(lossProfitStr);
                    mLossProfitRmb.setText(lossProfitRmbStr);
                } else {
                    mLastPrice.setText("");
                    mLossProfit.setText("");
                    mLossProfitRmb.setText("");
                }
            }
        }
    }
}
