package cn.doitedu.rulemk.demos;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class DroolsDemo2 {
    public static void main(String[] args) {

        KieServices kieServices = KieServices.Factory.get();
        //默认自动加载 META-INF/kmodule.xml
        KieContainer kieContainer = kieServices.getKieClasspathContainer();
        //kmodule.xml 中定义的 ksession name
        KieSession kieSession = kieContainer.newKieSession("s1");


        Student s = new Student(18);
        Teacher t = new Teacher(20);
        kieSession.insert(s);
        kieSession.insert(t);

        kieSession.fireAllRules();
        kieSession.dispose();


        System.out.println("年龄之和： " + s.getAge());


    }
}
