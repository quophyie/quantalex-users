package com.quantal.exchange.users.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import retrofit2.http.Header;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//@Aspect
public class RequestHeadersAspect {

    private final Set<String> headersToCheckFor;
    private final Map<String, Boolean> foundHeaders;
    private String pointcutPattern = "";

    public RequestHeadersAspect(){
        headersToCheckFor = new HashSet<>();
        foundHeaders = new HashMap<>();
    }
    public RequestHeadersAspect(Set<String> headersToCheckFor){

        this.headersToCheckFor = headersToCheckFor;
        foundHeaders = new HashMap<>();
        if (headersToCheckFor != null){
            headersToCheckFor.forEach( header ->foundHeaders.put(header.toUpperCase(), false));
        }
    }
    //@Around("allRetrofitInterfaces()")
    public Object checkHeaders(ProceedingJoinPoint pjp) throws Throwable{

        Object args[] = pjp.getArgs();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Annotation[][] annotations = method.getParameterAnnotations();

        if (args != annotations){
            for(Annotation[] annotation: annotations) {
                Annotation header = annotation[0];
                if (header != null && header instanceof Header){
                    String heaverAnnotionName = String.valueOf(AnnotationUtils.getValue(header));
                    foundHeaders.computeIfPresent(heaverAnnotionName.toUpperCase(), (key, val) -> true);
                }

            }
        }

        boolean bAllHeadersFound = foundHeaders.values()
                    .stream()
                    .reduce((previous, current) -> previous && current)
                    .orElse(false);

        Object result = pjp.proceed();

        return result;

    }

    //@Pointcut("execution (* com.quantal..services.api..*.*(..))")
    @Pointcut("execution(* com.quantal..services.api..*(..))")
    public void allRetrofitInterfaces(){

    }
}
