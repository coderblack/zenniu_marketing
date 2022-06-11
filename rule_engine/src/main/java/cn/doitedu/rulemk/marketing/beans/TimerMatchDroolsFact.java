package cn.doitedu.rulemk.marketing.beans;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimerMatchDroolsFact {
    private String keyByValue;
    private TimerCondition timerCondition;
    private long queryStartTime;
    private long queryEndTime;

    private boolean timerMatchResult;

}
