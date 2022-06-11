package cn.doitedu.rulemk.marketing.utils;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-24
 * @desc 分段查询的辅助工具
 */
public class CrossTimeQueryUtil {

    public static long getSegmentPoint(long timeStamp){

        // 给定时间向上取整，倒退2小时
        // 给定时间倒退2小时，然后向上取整
        Date dt = DateUtils.ceiling(new Date(timeStamp - 2 * 60 * 60 * 1000), Calendar.HOUR);

        return dt.getTime();
    }



}
