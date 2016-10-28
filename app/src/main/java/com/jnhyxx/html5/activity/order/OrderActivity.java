package com.jnhyxx.html5.activity.order;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;

import com.jnhyxx.html5.R;
import com.jnhyxx.html5.activity.BaseActivity;
import com.jnhyxx.html5.domain.market.Product;
import com.jnhyxx.html5.fragment.order.HoldingFragment;
import com.jnhyxx.html5.fragment.order.SettlementFragment;
import com.jnhyxx.html5.view.SlidingTabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OrderActivity extends BaseActivity implements HoldingFragment.Callback {

    @BindView(R.id.slidingTabLayout)
    SlidingTabLayout mSlidingTabLayout;
    @BindView(R.id.viewPager)
    ViewPager mViewPager;

    private Product mProduct;
    private int mFundType;
    private OrderAdapter mOrderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        ButterKnife.bind(this);

        initData(getIntent());

        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setDividerColors(ContextCompat.getColor(this, android.R.color.transparent));
        mOrderAdapter = new OrderAdapter(getSupportFragmentManager(), this, mProduct, mFundType);
        mViewPager.setAdapter(mOrderAdapter);
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    private void initData(Intent intent) {
        mProduct = (Product) intent.getSerializableExtra(Product.EX_PRODUCT);
        mFundType = intent.getIntExtra(Product.EX_FUND_TYPE, 0);
    }

    @Override
    public void onClosePositionButtonsClick() {
        SettlementFragment fragment = (SettlementFragment) mOrderAdapter.getFragment(1);
        if (fragment != null) {
            fragment.setHoldingFragmentClosedPositions(true);
        }
    }

    static class OrderAdapter extends FragmentPagerAdapter {

        private Context mContext;
        private Product mProduct;
        private int mFundType;
        private FragmentManager mFragmentManager;

        public OrderAdapter(FragmentManager fm, Context context, Product product, int fundType) {
            super(fm);
            mFragmentManager = fm;
            mContext = context;
            mProduct = product;
            mFundType = fundType;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return mContext.getString(R.string.holding);
                case 1:
                    return mContext.getString(R.string.settlement);
            }
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return HoldingFragment.newInstance(mProduct, mFundType);
                case 1:
                    return SettlementFragment.newInstance(mProduct, mFundType);
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        public Fragment getFragment(int position) {
            return mFragmentManager.findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + position);
        }
    }
}
