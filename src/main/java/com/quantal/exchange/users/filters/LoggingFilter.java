package com.quantal.exchange.users.filters;

import com.quantal.javashared.logger.LogField;
import com.quantal.javashared.logger.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

import static com.quantal.exchange.users.constants.CommonConstants.TRACE_ID_HEADER_KEY;
import static com.quantal.exchange.users.constants.CommonConstants.TRACE_ID_MDC_KEY;

/**
 * Created by dman on 08/07/2017.
 */


@Component
//@Order(1)
//@DependsOn("loggerAspect")
//@Configurable(autowire= Autowire.BY_TYPE,dependencyCheck=true, preConstruction=true)
//public class LoggingFilter implements Filter {
public class LoggingFilter extends GenericFilterBean {


    /**
     * Logging todo o request da aplicação para auditoria
     */
    //private final Logger logger = LogManager.getLogger();
   // private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    //private XLogger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final XLogger logger = XLoggerFactory.getXLogger(this.getClass().getName());

    /*@Override
    public void init(final FilterConfig filterConfig) throws ServletException {
      logger.info("Initiating LoggingFilter ...");
    }*/

    @Override
    //@Async
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        try {
            String traceId = ((HttpServletRequest) request).getHeader(TRACE_ID_HEADER_KEY);
            if (!StringUtils.isEmpty(traceId)) {
                MDC.put(TRACE_ID_MDC_KEY, traceId);
                ThreadContext.put(TRACE_ID_MDC_KEY, traceId);
            } else {
                MDC.put(TRACE_ID_MDC_KEY, UUID.randomUUID().toString());
                ThreadContext.put(TRACE_ID_MDC_KEY, UUID.randomUUID().toString());
            }
            //logger.debug("request received successfully {}", request);
            logger.info("request received successfully", new LogField("request",request.toString()));
            chain.doFilter(request, response);
            traceId = ((HttpServletResponse) response).getHeader(TRACE_ID_HEADER_KEY);
            if (StringUtils.isEmpty(traceId)) {
                ((HttpServletResponse) response).addHeader(TRACE_ID_HEADER_KEY, ThreadContext.get(TRACE_ID_MDC_KEY));
            }

            //logger.debug("response sent successfully {}", response);
            logger.debug("response sent successfully ", new LogField("response", response.toString()));
        } finally {
            ThreadContext.remove(TRACE_ID_MDC_KEY);
        }
    }

    @Override
    public void destroy() {
        logger.warn("Destroying LoggingFilter ...");
    }
}