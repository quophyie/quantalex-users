package com.quantal.exchange.users.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by dman on 09/10/2017.
 */
@Aspect
@Component
public class LoggerAspect {



    @Around("execution(* com.quantal.shared.logger.QuantalJsonLogger.*(..))")
    public Object createAndPopulateLogLine(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("args: "+pjp.getArgs());
        Object result = pjp.proceed();
        pjp.proceed();
        return result;

    }

    @Pointcut("execution(* com.quantal.exchange.users.services.implementations.LoginServiceImpl.*(..))")
    public void allUserRepoMethods(){}

}
