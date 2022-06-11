package cn.doitedu.rulemk.marketing.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-08-02
 * @desc 规则管理平台（web系统） 模拟器
 */
public class RuleManagerPlatformSimulator {

    public static void main(String[] args) throws Exception {
        insertRule();
    }

    // 插入新规则到mysql
    public static void insertRule() throws Exception {
        // 读规则控制器的drools代码
        String drlString = FileUtils.readFileToString(new File("rule_engine/rules/rule2.drl"), "utf-8");
        String jsonString = FileUtils.readFileToString(new File("rule_engine/rules/rule2.json"), "utf-8");

        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://hdp01:3306/abc?useUnicode=true&characterEncoding=utf8", "root", "ABC123abc.123");
        PreparedStatement st = conn.prepareStatement("insert into rule_demo (rule_name,rule_condition_json,rule_controller_drl,rule_status,create_time,modify_time,author) values (?,?,?,?,?,?,?)");
        st.setString(1,"rule_2");
        st.setString(2,jsonString);
        st.setString(3,drlString);
        st.setString(4,"1");
        st.setString(5,"2021-08-02 10:15:30");
        st.setString(6,"2021-08-02 10:15:30");
        st.setString(7,"doitedu");

        st.execute();
        st.close();
        conn.close();

    }


    // 更新已存在的规则


    // 删除规则


    // 启用规则


    // 停用规则


}
