package edu.junnikym.springaop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

@Component
public class NameToUpperCaseAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final String val = (String) invocation.proceed();

        if(invocation.getMethod().getName().equals("getName"))
            return val.toUpperCase();

        return val;
    }

}
