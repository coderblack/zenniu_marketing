package cn.doitedu.rulemk.demos;

import org.apache.commons.io.FileUtils;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.io.File;
import java.io.IOException;

public class DroolsDemo3 {

    public static void main(String[] args) throws IOException {
        String drlString = FileUtils.readFileToString(new File("rule_engine/rules/rule2.drl"), "utf-8");


        // 用规则字符串来构造一个kieSession
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drlString, ResourceType.DRL);
        KieSession kieSession = kieHelper.build().newKieSession();

        /*Student s = new Student(38);
        Teacher t = new Teacher(20);



        // 调用规则引擎
        kieSession.insert(s);
        kieSession.insert(t);

        kieSession.fireAllRules();
        kieSession.dispose();


        System.out.println("年龄之和： " + s.getAge());*/
    }
}
