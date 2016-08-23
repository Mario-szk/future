package com.jnhyxx.html5.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.jnhyxx.html5.R;

import java.util.List;

public class OrderConfigurationSelector extends LinearLayout {

    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int DEFAULT_ITEMS = 5;
    private static final int DEFAULT_PADDING_DP = 5;
    private static final int KEY_POSITION = -1;

    private int mMaximum;
    private int mNumberOfItems;
    private int mTextSize;
    private int mSelectedIndex;

    private OnItemClickListener mOnItemClickListener;

    private List<? extends OrderConfiguration> mOrderConfigurationList;

    private PopupWindow mPopupWindow;

    public interface OrderConfiguration {
        String getValue();
    }

    public interface OnItemClickListener {
        void onItemClick(OrderConfiguration configuration, int position);
    }

    public OrderConfigurationSelector(Context context) {
        super(context);
        init();
    }

    public OrderConfigurationSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        processAttrs(attrs);
        init();
    }

    private void processAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.OrderConfigurationSelector);

        int defaultFontSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_FONT_SIZE,
                getResources().getDisplayMetrics());

        mMaximum = typedArray.getInt(R.styleable.OrderConfigurationSelector_maximum, DEFAULT_ITEMS);
        mTextSize = typedArray.getDimensionPixelOffset(R.styleable.OrderConfigurationSelector_android_textSize, defaultFontSize);

        typedArray.recycle();
    }

    private void init() {
        mMaximum = mMaximum != 0 ? mMaximum : DEFAULT_ITEMS;
        mTextSize = mTextSize != 0 ? mTextSize :
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_FONT_SIZE,
                        getResources().getDisplayMetrics());

        setOrientation(HORIZONTAL);
        setBackgroundResource(R.drawable.bg_order_config_selector);

        initSelectorItems();
        initPopupWindow();
    }

    private void initSelectorItems() {
        addView(createItem(0));
        for (int i = 1; i < mMaximum; i++) {
            addView(createSplitLine());
            addView(createItem(i));
        }
    }

    private void showPopupWindow(final View v) {
        if (mPopupWindow != null) {
            if (!mPopupWindow.isShowing()) {
                int offsetX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                        getResources().getDisplayMetrics());
                View popupView = mPopupWindow.getContentView();
                popupView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                mPopupWindow.setWidth(v.getWidth() + offsetX);
                mPopupWindow.showAsDropDown(v, -offsetX, 0);
                mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        v.setBackgroundResource(R.drawable.bg_order_config_selector_item);
                        selectItem(mSelectedIndex);
                    }
                });
                v.setBackgroundResource(R.drawable.bg_order_config_selector_last_item);
                getSplitLine((Integer) v.getTag(KEY_POSITION)).setVisibility(VISIBLE);
            } else {
                mPopupWindow.dismiss();
            }
        }
    }

    private void initPopupWindow() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.popup_window_order_config, null);
        mPopupWindow = new PopupWindow(view,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
        mPopupWindow.setClippingEnabled(true);
    }

    public void setOrderConfigurationList(List<? extends OrderConfiguration> configurationList) {
        if (configurationList == null || configurationList.size() == 0) return;

        mNumberOfItems = configurationList.size();
        mOrderConfigurationList = configurationList;
        if (mNumberOfItems <= mMaximum) {
            for (int i = 0; i < mNumberOfItems; i++) {
                TextView item = getItem(i);
                item.setText(mOrderConfigurationList.get(i).getValue());
            }
            for (int i = mNumberOfItems; i < mMaximum; i++) {
                hideItem(i);
            }
        } else {
            for (int i = 0; i < mMaximum - 1; i++) {
                TextView item = getItem(i);
                item.setText(mOrderConfigurationList.get(i).getValue());
            }
            TextView item = getItem(mMaximum - 1);
            item.setText("...");
            List<? extends OrderConfiguration> hiddenList = mOrderConfigurationList.subList(
                    mMaximum - 1, mOrderConfigurationList.size());
            HiddenConfigurationAdapter adapter = new HiddenConfigurationAdapter(getContext());
            adapter.addAll(hiddenList);
            ListView listView = (ListView) mPopupWindow.getContentView().findViewById(android.R.id.list);
            listView.setAdapter(adapter);
        }
    }

    private void selectItem(int index) {
        mSelectedIndex = index;
        int visualSize = Math.min(mNumberOfItems, mMaximum);
        for (int i = 0; i < visualSize; i++) {
            unHighLightItem(i);
        }
        highLightItem(index);
    }

    private void highLightItem(int index) {
        getItem(index).setSelected(true);
        int visualSize = Math.min(mNumberOfItems, mMaximum);
        if (index == 0) {
            getSplitLine(index + 1).setVisibility(INVISIBLE);
        } else if (index == visualSize - 1) {
            getSplitLine(index).setVisibility(INVISIBLE);
        } else {
            getSplitLine(index).setVisibility(INVISIBLE);
            getSplitLine(index + 1).setVisibility(INVISIBLE);
        }
    }

    private void unHighLightItem(int index) {
        getItem(index).setSelected(false);
        int visualSize = Math.min(mNumberOfItems, mMaximum);
        if (index == 0) {
            getSplitLine(index + 1).setVisibility(VISIBLE);
        } else if (index == visualSize - 1) {
            getSplitLine(index).setVisibility(VISIBLE);
        } else {
            getSplitLine(index).setVisibility(VISIBLE);
            getSplitLine(index + 1).setVisibility(VISIBLE);
        }
    }

    private void hideItem(int index) {
        if (index > 0) {
            getItem(index).setVisibility(GONE);
            getSplitLine(index).setVisibility(GONE);
        }
    }

    private TextView getItem(int index) {
        return (TextView) getChildAt(index * 2);
    }

    private View getSplitLine(int index) {
        return getChildAt(index * 2 - 1);
    }

    private class HiddenConfigurationAdapter extends ArrayAdapter<OrderConfiguration> {

        public HiddenConfigurationAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createHideItem(position);
                TextView item = (TextView) convertView;
                item.setText(getItem(position).getValue());
            }
            return convertView;
        }
    }

    private TextView createHideItem(int position) {
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_PADDING_DP,
                getResources().getDisplayMetrics());
        TextView textView = new TextView(getContext());
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setOnClickListener(mHideItemClickListener);
        textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        textView.setBackgroundResource(R.drawable.bg_order_config_selector_item);
        textView.setPadding(0, padding, 0, padding);
        textView.setTag(KEY_POSITION, position);
        return textView;
    }

    private TextView createItem(int position) {
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_PADDING_DP,
                getResources().getDisplayMetrics());
        TextView textView = new TextView(getContext());
        textView.setGravity(Gravity.CENTER);
        textView.setOnClickListener(mListener);
        textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        textView.setBackgroundResource(R.drawable.bg_order_config_selector_item);
        textView.setPadding(0, padding, 0, padding);
        textView.setTag(KEY_POSITION, position);
        LayoutParams params = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        textView.setLayoutParams(params);
        return textView;
    }

    private View createSplitLine() {
        View view = new View(getContext());
        view.setBackgroundResource(android.R.color.white);
        int splineLineWith = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                getResources().getDisplayMetrics());
        LayoutParams params = new LayoutParams(splineLineWith, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        return view;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private OnClickListener mHideItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mPopupWindow.dismiss();
            int lastIndex = mMaximum - 1;
            int position = (int) view.getTag(KEY_POSITION);
            position = position + mMaximum - 1;
            OrderConfiguration configuration = mOrderConfigurationList.get(position);
            selectItem(lastIndex);
            getItem(lastIndex).setText(configuration.getValue());
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(configuration, position);
            }
        }
    };

    private OnClickListener mListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (int) view.getTag(KEY_POSITION);
            if (position == mMaximum - 1 && mNumberOfItems > mMaximum) {
                showPopupWindow(view);
            } else {
                selectItem(position);
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(mOrderConfigurationList.get(position), position);
                }
            }
        }
    };
}
