package com.quantal.exchange.users.aspects;

import com.quantal.shared.logger.LogField;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by dman on 09/10/2017.
 */
@Aspect
//@Scope("singleton")
@Configurable(autowire= Autowire.BY_TYPE,dependencyCheck=true, preConstruction=true)
@Component
public class LoggerAspect  {


    @Autowired
    private Environment env;

    @Value("${spring.version}")
    private String springVersion;

    @Value("${spring.application.name}")
    private String moduleName;

    @Value("${spring.application.version}")
    private String moduleVersion;

    @Autowired
    private LocaleResolver localeResolver;

    @Pointcut("execution(* com.quantal.shared.logger.QuantalJsonLogger.*(..))")
    public void allLoggerMethods() {}

    @Pointcut("cflow(within(LoggerAspect))")
    public void codeWithinAspect() {}

    private LogField hostname;

    private Map<String, Method> methodMap = new HashMap<>();

    public LoggerAspect() {

        try {
            this.hostname = new LogField("hostname", InetAddress.getLocalHost().getHostName());
        } catch (java.net.UnknownHostException unknownHostException) {

        }
    }

  @Around("allLoggerMethods() && !codeWithinAspect()")
    public Object createAndPopulateLogLine(ProceedingJoinPoint pjp) throws Throwable {
        List<Object> logLineFields = new ArrayList<>();
        LogField event = null, field;
        if (pjp.getArgs().length > 1) {
            for (int idx = 1; idx < pjp.getArgs().length; idx++) {
                Object arg = pjp.getArgs()[idx];

                if (arg instanceof String && idx == 1) {
                    event = new LogField("event", arg);
                } else if (arg instanceof Exception) {
                    event = new LogField("event", ((Exception) arg).getCause().getClass().getSimpleName());
                } else if (arg instanceof LogField) {
                    event = (LogField) arg;
                } else {
                    field = new LogField(arg.getClass().getSimpleName(), arg);
                    logLineFields.add(field);
                }

            }
        } else {
            event = new LogField("event", "UNKNOWN");
        }

        if(StringUtils.isEmpty(this.hostname)){
            this.hostname = new LogField("hostname", InetAddress.getLocalHost().getHostName());
        }

        String msg = (String) pjp.getArgs()[0];
        for(int argIdx = 0; argIdx < pjp.getArgs().length; argIdx++){
            if (pjp.getArgs()[argIdx] instanceof Throwable) {
                Throwable throwable = ((Exception) pjp.getArgs()[argIdx]);
                msg = throwable.getMessage();
                LogField stack = new LogField("stack", throwable.getStackTrace());
                logLineFields.add(stack);
                break;
            }
        }

        logLineFields.add(event);

        LogField msgField = new LogField("message",msg);
        LogField proglang = new LogField("proglang", "java");
        LogField framework = new LogField("framework", "spring-boot");
        LogField frameworkVersion = new LogField("frameworkVersion", springVersion);
        LogField moduleVer = new LogField("moduleVersion", moduleVersion);
        LogField name = new LogField("name", moduleName);


        LogField lang = new LogField("lang", Locale.UK);
        LogField time = new LogField("time", LocalDateTime.now().toString());

        logLineFields.add(msgField);
        logLineFields.add(proglang);
        logLineFields.add(framework);
        logLineFields.add(frameworkVersion);
        logLineFields.add(moduleVer);
        logLineFields.add(name);
        logLineFields.add(hostname);
        logLineFields.add(lang);
        logLineFields.add(time);

        String methodName = pjp.getSignature().getName();

        methodMap.putIfAbsent(methodName, ReflectionUtils.findMethod(pjp.getTarget().getClass(), methodName, new Class[]{String.class, Object[].class}));

        Method logMethod = methodMap.get(methodName);
        Object result = ReflectionUtils.invokeMethod(logMethod, pjp.getTarget(),msg, logLineFields.toArray());

        return result;

    }

    @Pointcut("execution(* com.quantal.exchange.users.services.implementations.LoginServiceImpl.*(..))")
    public void allUserRepoMethods(){}

}
