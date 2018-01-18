package com.mega.deviceapp.util;

import android.content.Context;

import com.mega.deviceapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author liuhao
 * @time 2017/10/31 11:30
 */

public class TimeUtil {
    static SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 时间转化为星期
     *
     * @param indexOfWeek
     *            星期的第几天
     */


    public static String getWeekDayStr(int indexOfWeek, Context context) {
        String weekDayStr = "";
        switch (indexOfWeek) {
            case 1:
                weekDayStr = context.getResources().getString(R.string.monday);
                break;
            case 2:
                weekDayStr = context.getResources().getString(R.string.tuesday);
                break;
            case 3:
                weekDayStr = context.getResources().getString(R.string.wednesday);
                break;
            case 4:
                weekDayStr = context.getResources().getString(R.string.thursday);
                break;
            case 5:
                weekDayStr = context.getResources().getString(R.string.friday);
                break;
            case 6:
                weekDayStr = context.getResources().getString(R.string.saturday);
                break;
            case 7:
                weekDayStr = context.getResources().getString(R.string.sunday);
                break;
        }
        return weekDayStr;
    }


    /**
     * 群发使用的时间转换
     */
    public static String multiSendTimeToStr(long timeStamp, Context context) {

        if (timeStamp == 0)
            return "";
        Calendar inputTime = Calendar.getInstance();
        String timeStr = timeStamp + "";
        if (timeStr.length() == 10) {
            timeStamp = timeStamp * 1000;
        }
        inputTime.setTimeInMillis(timeStamp);
        Date currenTimeZone = inputTime.getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.before(inputTime)) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return sdf.format(currenTimeZone);
        }
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        if (calendar.before(inputTime)) {
            @SuppressWarnings("unused")
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return context.getResources().getString(R.string.yesterday);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, -5);
            if (calendar.before(inputTime)) {
                return getWeekDayStr(inputTime.get(Calendar.DAY_OF_WEEK), context);
            } else {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                if (calendar.before(inputTime)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("M" + "/" + "d" + " ");
                    String temp1 = sdf.format(currenTimeZone);
                    SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
                    String temp2 = sdf1.format(currenTimeZone);
                    return temp1 + temp2;
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy" + "/" + "M" + "/" + "d" + " ");
                    String temp1 = sdf.format(currenTimeZone);
                    SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
                    String temp2 = sdf1.format(currenTimeZone);
                    return temp1 + temp2;
                }
            }
        }
    }

    /**
     * 格式化时间（输出类似于 刚刚, 4分钟前, 一小时前, 昨天这样的时间）
     *
     * @param time
     *            需要格式化的时间 如"2014-07-14 19:01:45"
     * @param pattern
     *            输入参数time的时间格式 如:"yyyy-MM-dd HH:mm:ss"
     *            <p/>
     *            如果为空则默认使用"yyyy-MM-dd HH:mm:ss"格式
     * @return time为null，或者时间格式不匹配，输出空字符""
     */
    public static String formatDisplayTime(String time, String pattern, Context context) {
        String display = "";
        int tMin = 60 * 1000;
        int tHour = 60 * tMin;
        int tDay = 24 * tHour;

        if (time != null) {
            if (pattern == null)
                pattern = "yyyy-MM-dd HH:mm:ss";
            try {
                Date tDate = new SimpleDateFormat(pattern).parse(time);
                Date today = new Date();
                SimpleDateFormat thisYearDf = new SimpleDateFormat("yyyy");
                SimpleDateFormat todayDf = new SimpleDateFormat("yyyy-MM-dd");
                Date thisYear = new Date(thisYearDf.parse(thisYearDf.format(today)).getTime());
                Date yesterday = new Date(todayDf.parse(todayDf.format(today)).getTime());
                Date beforeYes = new Date(yesterday.getTime() - tDay);
                if (tDate != null) {
                    long dTime = today.getTime() - tDate.getTime();
                    if (tDate.before(thisYear)) {
                        display = new SimpleDateFormat("yyyy/MM/dd").format(tDate);
                    } else {

                        if (dTime < tMin) {
                            display = context.getResources().getString(R.string.just_now);
                        } else if (dTime < tHour) {
                            display = (int) Math.ceil(dTime / tMin) + context.getResources().getString(R.string.minutes_ago);
                        } else if (dTime < tDay && tDate.after(yesterday)) {
                            display = (int) Math.ceil(dTime / tHour) + context.getResources().getString(R.string.hours_ago);
                        } else if (tDate.after(beforeYes) && tDate.before(yesterday)) {
                            display = context.getResources().getString(R.string.yesterday)+"  " + new SimpleDateFormat("HH:mm").format(tDate);
                        } else {
                            display = multiSendTimeToStr(tDate.getTime() / 1000, context);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return display;
    }

}
