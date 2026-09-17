package com.smis.security;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;

class ErrorReferencesTest {
    @Test void notificationMessageContainsOnlyGenericTextAndLoggedReference() {
        var logger = (Logger) LoggerFactory.getLogger(ErrorReferences.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            String message = ErrorReferences.userMessage(new IllegalStateException("SQL secret-password"));
            assertThat(message).matches("Unable to complete the request\\. Reference: [0-9a-f-]{36}");
            assertThat(message).doesNotContain("SQL", "secret-password", "IllegalStateException");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .contains(message.substring(message.lastIndexOf(' ') + 1), "IllegalStateException")
                    .doesNotContain("secret-password");
        } finally { logger.detachAppender(appender); }
    }

    @Test void logsReferenceAndOriginWithoutExceptionMessagesAndSuppressesDebugAttributes() {
        var logger = (Logger) LoggerFactory.getLogger(ErrorReferences.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var exception = new IllegalArgumentException("secret-csrf-token", new RuntimeException("secret-password"));
            var request = new MockHttpServletRequest();
            var attributes = new SafeErrorAttributes();
            attributes.resolveException(request, new org.springframework.mock.web.MockHttpServletResponse(), null, exception);
            var web = new ServletWebRequest(request);
            var result = attributes.getErrorAttributes(web, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.values()));
            assertThat(result).doesNotContainKeys("exception", "trace", "errors");
            assertThat(result.get("message")).isEqualTo("Unable to complete the request");
            assertThat(attributes.getErrorAttributes(web, ErrorAttributeOptions.defaults()).get("reference"))
                    .isEqualTo(result.get("reference"));
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage()).contains(result.get("reference").toString(),
                    "IllegalArgumentException", "ErrorReferencesTest").doesNotContain("secret-csrf-token", "secret-password");
            assertThat(appender.list.get(0).getThrowableProxy()).isNull();
        } finally {
            logger.detachAppender(appender);
        }
    }
}
