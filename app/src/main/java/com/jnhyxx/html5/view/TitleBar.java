package com.jnhyxx.html5.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jnhyxx.html5.R;

public class TitleBar extends LinearLayout {

    private CharSequence mTitle;
    private int mTitleSize;
    private ColorStateList mTitleColor;
    private CharSequence mRightText;
    private int mRightTextSize;
    private ColorStateList mRightTextColor;
    private Drawable mRightImage;
    private boolean mBackFeature;
    private Drawable mBackIcon;

    private TextView mTitleView;
    private TextView mLeftView;
    private TextView mRightView;

    public TitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        processAttrs(attrs);

        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measureWidth = getMeasuredWidth();
        int measureHeight = getMeasuredHeight();

        int fixedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                getResources().getDisplayMetrics());

        Log.d("TEST", "onMeasure: " + measureWidth + ", " + measureHeight);

        setMeasuredDimension(measureWidth, fixedHeight);
    }

    private void processAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TitleBar);

        int defaultTitleSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18,
                getResources().getDisplayMetrics());
        int defaultFontSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                getResources().getDisplayMetrics());

        mTitle = typedArray.getText(R.styleable.TitleBar_titleText);
        mTitleSize = typedArray.getDimensionPixelOffset(R.styleable.TitleBar_titleTextSize, defaultTitleSize);
        mTitleColor = typedArray.getColorStateList(R.styleable.TitleBar_titleTextColor);
        mRightText = typedArray.getText(R.styleable.TitleBar_rightText);
        mRightTextSize = typedArray.getDimensionPixelOffset(R.styleable.TitleBar_rightTextSize, defaultFontSize);
        mRightTextColor = typedArray.getColorStateList(R.styleable.TitleBar_rightTextColor);
        mRightImage = typedArray.getDrawable(R.styleable.TitleBar_rightImage);
        mBackFeature = typedArray.getBoolean(R.styleable.TitleBar_backFeature, false);
        mBackIcon = typedArray.getDrawable(R.styleable.TitleBar_backIcon);

        typedArray.recycle();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundResource(R.color.colorPrimary);

        // left view
        int paddingHorizontal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics());
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mLeftView = new TextView(getContext());
        mLeftView.setGravity(Gravity.CENTER);
        mLeftView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        addView(mLeftView, params);

        // center view
        mTitleView = new TextView(getContext());
        mTitleView.setGravity(Gravity.CENTER);
        params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        params.weight = 1;
        addView(mTitleView, params);

        // right view
        mRightView = new TextView(getContext());
        mRightView.setGravity(Gravity.CENTER);
        mRightView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        addView(mRightView, params);
        if (mBackFeature) {
            setBackButtonIcon(mBackIcon);
            mRightView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackClick(view);
                }
            });
        }
        setTitle(mTitle);
        setTitleSize(mTitleSize);
        setTitleColor(mTitleColor);
        setRightText(mRightText);
        setRightTextSize(mRightTextSize);
        setRightTextColor(mRightTextColor);
        setRightImage(mRightImage);
    }

    private void onBackClick(View view) {
        if (getContext() instanceof Activity) {
            Activity activity = (Activity) getContext();
            activity.onBackPressed();
        }
    }

    public void setBackButtonIcon(Drawable backIcon) {
        if (backIcon != null) {
            mLeftView.setCompoundDrawables(backIcon, null, null, null);
        } else { // default icon
            mLeftView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tb_back, 0, 0, 0);
        }
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        if (TextUtils.isEmpty(mTitle)) return;
        mTitleView.setText(mTitle);
    }

    public void setTitleSize(int titleSize) {
        mTitleSize = titleSize;
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize);
    }

    public void setRightText(CharSequence rightText) {
        mRightText = rightText;
        mRightView.setText(rightText);
    }

    public void setRightTextSize(int rightTextSize) {
        mRightTextSize = rightTextSize;
        mRightView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mRightTextSize);
    }

    public void setRightImage(Drawable rightImage) {
        mRightImage = rightImage;
        mRightView.setCompoundDrawables(mRightImage, null, null, null);
    }

    public void setTitleColor(ColorStateList titleColor) {
        mTitleColor = titleColor;
        if (mTitleColor != null) {
            mTitleView.setTextColor(mTitleColor);
        } else {
            mTitleView.setTextColor(ColorStateList.valueOf(Color.WHITE));
        }
    }

    public void setRightTextColor(ColorStateList rightTextColor) {
        mRightTextColor = rightTextColor;
        if (mRightTextColor != null) {
            mRightView.setTextColor(mRightTextColor);
        } else {
            mRightView.setTextColor(ColorStateList.valueOf(Color.WHITE));
        }
    }
}
