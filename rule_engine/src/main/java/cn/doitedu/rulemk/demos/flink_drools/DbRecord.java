package cn.doitedu.rulemk.demos.flink_drools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbRecord {
    private String id;
    private String rule_name;
    private String drl_string;
    private String online;

}
