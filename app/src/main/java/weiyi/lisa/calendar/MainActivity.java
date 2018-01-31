package weiyi.lisa.calendar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bigkoo.pickerview.OptionsPickerView;
import com.bigkoo.pickerview.listener.CustomListener;
import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.CalendarView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements CalendarView.OnDateSelectedListener, CalendarView.OnDateChangeListener {

    private TextView mTextMonthDay;

    private TextView mTextYear;

    private TextView mTextLunar;

    private TextView mTextCurrentDay;

    private CalendarView mCalendarView;

    private RelativeLayout mRelativeTool;
    private int mYear;

    // 基准日历实体
    private java.util.Calendar beginCalendar;
    // 基准日期的工作状态
    private String beginCalendarWorkState;
    // 记录第一次获取的年月
    private int firstGetYear;
    private int firstGetMonth;
    // 记录月份是否改变过
    private boolean isMonthModify;
    // 工作状态选择弹框
    private OptionsPickerView workStatePicker;
    // 工作状态集合
    private List<String> workStateList;
    // sp
    private SharedPreferences sp;
    // 记录当前选中日期的实体
    private Calendar selectedCalendar;
    // 相识日期
    private java.util.Calendar firstMeetingCalendar;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        setStatusBarDarkMode();
        mTextMonthDay = (TextView) findViewById(R.id.tv_month_day);
        mTextYear = (TextView) findViewById(R.id.tv_year);
        mTextLunar = (TextView) findViewById(R.id.tv_lunar);
        mRelativeTool = (RelativeLayout) findViewById(R.id.rl_tool);
        mCalendarView = (CalendarView) findViewById(R.id.calendarView);
        mTextCurrentDay = (TextView) findViewById(R.id.tv_current_day);
        mTextMonthDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCalendarView.showSelectLayout(mYear);
                mTextLunar.setVisibility(View.GONE);
                mTextYear.setVisibility(View.GONE);
                mTextMonthDay.setText(String.valueOf(mYear));
            }
        });
        findViewById(R.id.fl_current).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCalendarView.scrollToCurrent();
            }
        });

        findViewById(R.id.fl_current).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 弹出日期状态选择框
                workStatePicker.show();
                return true;
            }
        });

        mCalendarView.setOnDateChangeListener(this);
        mCalendarView.setOnDateSelectedListener(this);
        mTextYear.setText(String.valueOf(mCalendarView.getCurYear()));
        mYear = mCalendarView.getCurYear();
        mTextMonthDay.setText(mCalendarView.getCurMonth() + "月" + mCalendarView.getCurDay() + "日");
        mTextLunar.setText("今日");
        mTextCurrentDay.setText(String.valueOf(mCalendarView.getCurDay()));
    }

    @Override
    protected void initData() {
        // sp
        sp = getSharedPreferences("sp_begin_data", Context.MODE_PRIVATE);
        // 相识日期实例化
        firstMeetingCalendar = java.util.Calendar.getInstance();
        firstMeetingCalendar.set(2017, 3, 25, 0, 0, 0);
        // 显示相识天数
        ((TextView) findViewById(R.id.daysCount)).setText("第" + calculateLoveDays() + "天");
        // 基准日期实例化
        beginCalendar = java.util.Calendar.getInstance();
        int spYear = sp.getInt("beginYear", 0);
        int spMonth = sp.getInt("beginMonth", 0);
        int spDay = sp.getInt("beginDay", 0);
        String spWorkState = sp.getString("workState", null);
        if (spYear != 0 && spMonth != 0 && spDay != 0 && spWorkState != null) {
            beginCalendar.set(spYear, spMonth - 1, spDay, 0, 0, 0);
            beginCalendarWorkState = spWorkState;
        } else {
            // 默认基准日期2018年1月5日 白班
            beginCalendar.set(2018, 0, 5, 0, 0, 0);
            beginCalendarWorkState = "白班";
        }
        // 当前日期实例化
        Calendar cal = new Calendar();
        cal.setYear(mCalendarView.getCurYear());
        cal.setMonth(mCalendarView.getCurMonth());
        cal.setDay(mCalendarView.getCurDay());
        // 计算当前日期所在月的工作状态
        setMonthWorkStatus(cal);

        // 工作状态集合
        workStateList = new ArrayList<>();
        workStateList.add("白班");
        workStateList.add("夜班");
        workStateList.add("下夜");
        workStateList.add("休息");

        // 工作状态选择框
        workStatePicker = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int option2, int options3, View v) {
                //返回的分别是三个级别的选中位置
                String result = workStateList.get(options1);
                // 将数据持久化到sp
                if (!TextUtils.isEmpty(result)) {
                    sp.edit().putInt("beginYear", selectedCalendar.getYear())
                            .putInt("beginMonth", selectedCalendar.getMonth())
                            .putInt("beginDay", selectedCalendar.getDay())
                            .putString("workState", result).apply();
                    // 更新基准日期
                    beginCalendar.set(selectedCalendar.getYear(), selectedCalendar.getMonth() - 1, selectedCalendar.getDay(), 0, 0, 0);
                    // 更新基准日期的工作状态
                    beginCalendarWorkState = result;
                    // 更改当前日历视图的状态
                    setMonthWorkStatus(selectedCalendar);
                }


            }
        })
                .setLayoutRes(R.layout.pickerview_custom_options, new CustomListener() {
                    @Override
                    public void customLayout(View v) {
                        final TextView tvSubmit = (TextView) v.findViewById(R.id.tv_finish);
                        ImageView ivCancel = (ImageView) v.findViewById(R.id.iv_cancel);
                        tvSubmit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                workStatePicker.returnData();
                                workStatePicker.dismiss();
                            }
                        });

                        ivCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                workStatePicker.dismiss();
                            }
                        });

                    }
                })
                .isDialog(true)
                .build();
        workStatePicker.setPicker(workStateList);

    }

    private Calendar getSchemeCalendar(int year, int month, int day, int color, String text) {
        Calendar calendar = new Calendar();
        calendar.setYear(year);
        calendar.setMonth(month);
        calendar.setDay(day);
        calendar.setSchemeColor(color);//如果单独标记颜色、则会使用这个颜色
        calendar.setScheme(text);
        return calendar;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onDateChange(Calendar calendar) {
        mTextLunar.setVisibility(View.VISIBLE);
        mTextYear.setVisibility(View.VISIBLE);
        mTextMonthDay.setText(calendar.getMonth() + "月" + calendar.getDay() + "日");
        mTextYear.setText(String.valueOf(calendar.getYear()));
        mTextLunar.setText(calendar.getLunar());
        mYear = calendar.getYear();

        // 记录第一次获取的年月
        if (firstGetYear == 0) {
            firstGetYear = calendar.getYear();
        }
        if (firstGetMonth == 0) {
            firstGetMonth = calendar.getMonth();
        }
        if (firstGetYear != calendar.getYear() || firstGetMonth != calendar.getMonth()) {
            // 记录月份是否改变过
            isMonthModify = true;
        }
        if (beginCalendar != null && isMonthModify) {
            // 月份改变则更新工作状态标记
            setMonthWorkStatus(calendar);
        }
    }

    @Override
    public void onDateSelected(Calendar calendar) {
        onDateChange(calendar);
        // 记录当前选中的日期
        selectedCalendar = calendar;
        if (firstMeetingCalendar != null) {
            // 显示相识天数
            ((TextView) findViewById(R.id.daysCount)).setText("第" + calculateLoveDays() + "天");
        }
    }


    @Override
    public void onYearChange(int year) {
        mTextMonthDay.setText(String.valueOf(year));
    }

    /**
     * 计算某天的工作状态
     */
    private String calculateDayStatus(java.util.Calendar cal) {
        if (beginCalendar != null && beginCalendarWorkState != null) {
            // 工作状态
            String workStatus = null;
            // 用计算出的时间减去基准时间
            long val = cal.getTimeInMillis() - beginCalendar.getTimeInMillis();
            if (cal.getTimeInMillis() < beginCalendar.getTimeInMillis()) {
                // 目标时间在基准时间之前，需要取绝对值
                val = Math.abs(val);
            }
            // 换算后得到天数
            long days = val / (1000 * 60 * 60 * 24);
            long remainder = days % 4L;
            // 目标时间在基准时间之前，余数的处理
            if (cal.getTimeInMillis() < beginCalendar.getTimeInMillis()) {
                // 不足一天扔按照一天计算
                remainder++;
                remainder = 4L - remainder;
                if (remainder == 4L) {
                    remainder = 0L;
                }
            }
            if (remainder == 0L) {
                workStatus = beginCalendarWorkState;
            } else if (remainder == 1L) {
                switch (beginCalendarWorkState) {
                    case "白班":
                        workStatus = "夜班";
                        break;
                    case "夜班":
                        workStatus = "下夜";
                        break;
                    case "下夜":
                        workStatus = "休息";
                        break;
                    case "休息":
                        workStatus = "白班";
                        break;
                }
            } else if (remainder == 2L) {
                switch (beginCalendarWorkState) {
                    case "白班":
                        workStatus = "下夜";
                        break;
                    case "夜班":
                        workStatus = "休息";
                        break;
                    case "下夜":
                        workStatus = "白班";
                        break;
                    case "休息":
                        workStatus = "夜班";
                        break;
                }

            } else if (remainder == 3L) {
                switch (beginCalendarWorkState) {
                    case "白班":
                        workStatus = "休息";
                        break;
                    case "夜班":
                        workStatus = "白班";
                        break;
                    case "下夜":
                        workStatus = "夜班";
                        break;
                    case "休息":
                        workStatus = "下夜";
                        break;
                }
            }
            return workStatus;
        }
        return null;
    }

    /**
     * 计算当前日期所在月的工作状态
     */
    private void setMonthWorkStatus(Calendar calendar) {
        List<Calendar> schemes = new ArrayList<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (cal != null && calendar != null) {
            // 设置年份
            cal.set(java.util.Calendar.YEAR, calendar.getYear());
            // 设置月份
            cal.set(java.util.Calendar.MONTH, calendar.getMonth() - 1);
            // 每个月有多少天
            int maxDay = cal.getActualMaximum(java.util.Calendar.DATE);
            // 遍历该月的每一天
            for (int i = 1; i <= maxDay; i++) {
                cal.set(java.util.Calendar.DATE, i);
                // 计算当天的工作状态
                String workStatus = calculateDayStatus(cal);
                if (!TextUtils.isEmpty(workStatus)) {
                    switch (workStatus) {
                        case "白班":
                            schemes.add(getSchemeCalendar(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DATE), 0xFFE0E0E0, "白"));
                            break;
                        case "夜班":
                            schemes.add(getSchemeCalendar(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DATE), 0xFFE0E0E0, "夜"));
                            break;
                        case "下夜":
                            schemes.add(getSchemeCalendar(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DATE), 0xFFE0E0E0, "下"));
                            break;
                        case "休息":
                            schemes.add(getSchemeCalendar(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DATE), 0xFFdf1356, "休"));
                            break;
                    }
                }

            }
            mCalendarView.setSchemeDate(schemes);
        }
    }

    /**
     * 计算相识了多少天
     */
    private long calculateLoveDays() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (selectedCalendar != null && firstMeetingCalendar != null) {
            cal.set(java.util.Calendar.YEAR, selectedCalendar.getYear());
            cal.set(java.util.Calendar.MONTH, selectedCalendar.getMonth() - 1);
            cal.set(java.util.Calendar.DAY_OF_MONTH, selectedCalendar.getDay());
            // 获得微秒级时间差
            long val = cal.getTimeInMillis() - firstMeetingCalendar.getTimeInMillis();
            // 换算后得到天数
            return val / (1000 * 60 * 60 * 24) + 1L;
        } else {
            return 0L;
        }
    }

}