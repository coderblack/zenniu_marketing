package cn.doitedu.rulemk.marketing.beans;


import cn.doitedu.rulemk.marketing.service.TriggerModeRulelMatchServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleControllerFact {

    private MarketingRule marketingRule;
    private TriggerModeRulelMatchServiceImpl triggerModeRulelMatchService;
    private EventBean eventBean;
    private boolean matchResult;

    private TimerMatchDroolsFact timerMatchDroolsFact;

}