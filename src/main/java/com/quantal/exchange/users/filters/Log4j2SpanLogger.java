package com.quantal.exchange.users.filters;

/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

        import java.util.regex.Pattern;

        import org.apache.logging.log4j.ThreadContext;
        import org.slf4j.Logger;
        import org.slf4j.MDC;
        import org.springframework.cloud.sleuth.Span;
        import org.springframework.cloud.sleuth.log.SpanLogger;
        import org.springframework.stereotype.Component;

/**
 * Span listener that logs to the console when a span got started / stopped / continued.
 *
 * @author Spencer Gibb
 * @since 1.0.0
 */
public class Log4j2SpanLogger implements SpanLogger {

    private final Logger log;
    private final Pattern nameSkipPattern;


    public Log4j2SpanLogger(String nameSkipPattern) {
        this.nameSkipPattern = Pattern.compile(nameSkipPattern);
        this.log = org.slf4j.LoggerFactory.getLogger(Log4j2SpanLogger.class);
    }

    Log4j2SpanLogger(String nameSkipPattern, Logger log) {
        this.nameSkipPattern = Pattern.compile(nameSkipPattern);
        this.log = log;
    }

    @Override
    public void logStartedSpan(Span parent, Span span) {
        MDC.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
        MDC.put(Span.SPAN_EXPORT_NAME, String.valueOf(span.isExportable()));
        MDC.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));

        /*ThreadContext.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
        ThreadContext.put(Span.SPAN_EXPORT_NAME, String.valueOf(span.isExportable()));
        ThreadContext.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));*/

        log("Starting span: {}", span);
        if (parent != null) {
            log("With parent: {}", parent);
            MDC.put(Span.PARENT_ID_NAME, Span.idToHex(parent.getSpanId()));
            ThreadContext.put(Span.PARENT_ID_NAME, Span.idToHex(parent.getSpanId()));
        }
    }

    @Override
    public void logContinuedSpan(Span span) {
        MDC.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
        MDC.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));
        MDC.put(Span.SPAN_EXPORT_NAME, String.valueOf(span.isExportable()));

       /* ThreadContext.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
        ThreadContext.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));
        ThreadContext.put(Span.SPAN_EXPORT_NAME, String.valueOf(span.isExportable()));
        */

        setParentIdIfPresent(span);
        log("Continued span: {}", span);
    }

    private void setParentIdIfPresent(Span span) {
        if (!span.getParents().isEmpty()) {
            MDC.put(Span.PARENT_ID_NAME, Span.idToHex(span.getParents().get(0)));
           // ThreadContext.put(Span.PARENT_ID_NAME, Span.idToHex(span.getParents().get(0)));
        }
    }

    @Override
    public void logStoppedSpan(Span parent, Span span) {
        if (span != null) {
            log("Stopped span: {}", span);
        }
        if (span != null && parent != null) {
            log("With parent: {}", parent);
            MDC.put(Span.SPAN_ID_NAME, Span.idToHex(parent.getSpanId()));
            MDC.put(Span.SPAN_EXPORT_NAME, String.valueOf(parent.isExportable()));

            /*
            ThreadContext.put(Span.SPAN_ID_NAME, Span.idToHex(parent.getSpanId()));
            ThreadContext.put(Span.SPAN_EXPORT_NAME, String.valueOf(parent.isExportable()));
            */

            setParentIdIfPresent(parent);
        }
        else {
            MDC.remove(Span.SPAN_ID_NAME);
            MDC.remove(Span.SPAN_EXPORT_NAME);
            MDC.remove(Span.TRACE_ID_NAME);
            MDC.remove(Span.PARENT_ID_NAME);

            /*
            ThreadContext.remove(Span.SPAN_ID_NAME);
            ThreadContext.remove(Span.SPAN_EXPORT_NAME);
            ThreadContext.remove(Span.TRACE_ID_NAME);
            ThreadContext.remove(Span.PARENT_ID_NAME);
            */
        }
    }

    private void log(String text, Span span) {
        if (span != null && this.nameSkipPattern.matcher(span.getName()).matches()) {
            return;
        }
        if (this.log.isTraceEnabled()) {
            this.log.trace(text, span);
        }
        //this.log.info(text, span);
    }

}