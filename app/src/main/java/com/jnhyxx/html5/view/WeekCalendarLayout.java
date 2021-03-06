package com.jnhyxx.html5.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jnhyxx.html5.R;
import com.johnz.kutils.DateUtil;
import com.johnz.kutils.StrUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * Created by ${wangJie} on 2017/2/16.
 * 财经日历的顶部的显示一周星期的view
 */

public class WeekCalendarLayout extends LinearLayout implements View.OnClickListener {
    private static final String TAG = "WeekCalendarLayout";

    private static String[] weekData = new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /**
     * 今天的日期在星期列表中的索引
     */
    private int mTodayWeekIndex;
    private int mIndex;

    private static final String TODAY = "\n今天";

    private static final int DEFAULT_ITEM_HEIGHT = 30;

    private OnWeekSelectListener mOnWeekSelectListener;

    public interface OnWeekSelectListener {
        /**
         * @param index   对应的索引
         * @param week    所选择的星期
         * @param dayTime 所选择的日期
         */
        void onWeekSelected(int index, String week, String dayTime);
    }

    public WeekCalendarLayout(Context context) {
        super(context);
        initView();
    }

    public WeekCalendarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        processAttrs(attrs);

    }

    private void processAttrs(AttributeSet attrs) {

    }

    private void initView() {
        setBackgroundResource(android.R.color.white);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < weekData.length; i++) {
            addView(createLine());
            addView(createWeekView(weekData[i], i));
        }
        addView(createLine());
    }

    private View createWeekView(String week, int position) {
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_ITEM_HEIGHT,
                getResources().getDisplayMetrics());
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        TextView textView = new TextView(getContext());
        textView.setPadding(padding, 0, padding, 0);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        textView.setBackgroundResource(R.drawable.bg_week_calendar);
        if (isToadyWeek(week)) {
            textView.setSelected(true);
            mTodayWeekIndex = position;
        } else {
            textView.setSelected(false);
        }

        if (textView.isSelected()) {
            textView.setTextColor(Color.WHITE);
            textView.setText(StrUtil.mergeTextWithRatio(week, TODAY, 0.7f));
        } else {
            textView.setText(week);
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.blueAssist));
        }

        textView.setOnClickListener(this);
        textView.setTag(position);
        LayoutParams layoutParams = new LayoutParams(height, height);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    public boolean isToadyWeek(String week) {
        String dayOfWeek = DateUtil.getDayOfWeek(System.currentTimeMillis());
        String weekData = week.substring(week.length() - 1);
        Log.d(TAG, "今天的星期" + dayOfWeek + "截取的日期" + weekData);
        return dayOfWeek.equalsIgnoreCase(weekData);
    }


    private View createLine() {
        View view = new View(getContext());
        view.setBackgroundResource(R.color.blueAssist);
        int mLineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.7f, getResources().getDisplayMetrics());
        LayoutParams layoutParams = new LayoutParams(0, mLineHeight);
        layoutParams.weight = 1;
        view.setLayoutParams(layoutParams);
        return view;
    }

    public void setOnWeekSelectListener(OnWeekSelectListener onWeekSelectListener) {
        mOnWeekSelectListener = onWeekSelectListener;
    }

    @Override
    public void onClick(View v) {
        int position = (int) v.getTag();
        setSelectWeek(position);
    }

    public void setSelectWeek(int position) {
        if (position < 0 || position > 7) return;

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof TextView) {
                TextView textView = ((TextView) view);
                textView.setSelected(false);
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.blueAssist));
            }
        }
        TextView textView = getTextView(position);
        if (textView != null) {
            textView.setSelected(true);
            textView.setTextColor(Color.WHITE);
            mIndex = position;
            if (mOnWeekSelectListener != null) {
                mOnWeekSelectListener.onWeekSelected(position, weekData[position], getSelectDayTime(position));
            }
        }
    }

    public int getIndex() {
        return mIndex;
    }

    /**
     * 获取所选择星期对应的时间
     *
     * @param selectPosition
     * @return 2017-1-20
     */
    private String getSelectDayTime(int selectPosition) {
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.add(Calendar.DAY_OF_YEAR, selectPosition - mTodayWeekIndex);
        Date time = todayCalendar.getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(time);
    }


    public TextView getTextView(int index) {
        View view = getChildAt(index * 2 + 1);
        if (view instanceof TextView) {
            return (TextView) view;
        }
        return null;
    }

}
