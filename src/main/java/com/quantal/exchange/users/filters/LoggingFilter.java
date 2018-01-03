package com.quantal.exchange.users.filters;

import com.quantal.javashared.annotations.logger.InjectLogger;
import com.quantal.javashared.dto.LogEvent;
import com.quantal.javashared.logger.LoggerFactory;
import com.quantal.javashared.logger.QuantalLogger;
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

import static com.quantal.javashared.constants.CommonConstants.EVENT_KEY;
import static com.quantal.javashared.constants.CommonConstants.REQUEST_KEY;
import static com.quantal.javashared.constants.CommonConstants.RESPONSE_KEY;
import static com.quantal.javashared.constants.CommonConstants.TRACE_ID_HEADER_KEY;
import static com.quantal.javashared.constants.CommonConstants.TRACE_ID_MDC_KEY;

/**
 * Created by dman on 08/07/2017.
 */


@Component
public class LoggingFilter extends GenericFilterBean {


    /**
     * Logging todo o request da aplicação para auditoria
     */

    @InjectLogger
    private QuantalLogger logger;

    @Override
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

            logger.with(REQUEST_KEY,request.toString())
                  .with(EVENT_KEY, "REQUEST_RECEIVED")
                  .info("request received successfully");
            chain.doFilter(request, response);
            traceId = ((HttpServletResponse) response).getHeader(TRACE_ID_HEADER_KEY);
            if (StringUtils.isEmpty(traceId)) {
                ((HttpServletResponse) response).addHeader(TRACE_ID_HEADER_KEY, ThreadContext.get(TRACE_ID_MDC_KEY));
            }

            logger.with(RESPONSE_KEY, response.toString()).debug("response sent successfully ", new LogEvent("RESPONSE_SENT"));
        } finally {
            ThreadContext.remove(TRACE_ID_MDC_KEY);
        }
    }

    @Override
    public void destroy() {
        logger.with(EVENT_KEY, "FILTER_DESTROY").warn("Destroying LoggingFilter ...");
    }
}
