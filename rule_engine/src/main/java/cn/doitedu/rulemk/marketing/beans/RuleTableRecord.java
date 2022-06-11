package cn.doitedu.rulemk.marketing.beans;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleTableRecord {
    private String id;
    private String rule_name;
    private String rule_condition_json;
    private String rule_controller_drl;
    private String rule_status;
    private String create_time;
    private String modify_time;
    private String author;

}