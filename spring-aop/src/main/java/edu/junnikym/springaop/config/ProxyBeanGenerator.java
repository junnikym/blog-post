package edu.junnikym.springaop.config;

import edu.junnikym.springaop.interceptor.NameToUpperCaseAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyBeanGenerator implements BeanPostProcessor {

    public ProxyBeanGenerator(NameToUpperCaseAdvice nameToUpperCaseAdvice) {
        this.nameToUpperCaseAdvice = nameToUpperCaseAdvice;
    }

    private final NameToUpperCaseAdvice nameToUpperCaseAdvice;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if(beanName.equals("engineControlUnit")) {
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setTarget(bean);
            proxyFactory.addAdvice(new NameToUpperCaseAdvice());
            return proxyFactory.getProxy();
        }

        return bean;
    }

}