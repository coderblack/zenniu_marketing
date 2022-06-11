package cn.doitedu.rulemk.demos.flink_drools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class RuleInjector {

    public static void main(String[] args) throws Exception {


        String drlString = FileUtils.readFileToString(new File("rule_engine/src/main/resources/rules/demo2.drl"), "utf-8");

        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://hdp01:3306/abc?useUnicode=true&characterEncoding=utf8", "root", "ABC123abc.123");
        PreparedStatement st = conn.prepareStatement("insert into rule_demo (rule_name,drl_String,online) values (?,?,?)");
        st.setString(1,"demo2");
        st.setString(2,drlString);
        st.setString(3,"1");

        st.execute();
        st.close();
        conn.close();


    }

}
