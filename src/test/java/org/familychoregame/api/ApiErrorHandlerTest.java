package org.familychoregame.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

class ApiErrorHandlerTest {
    @AfterEach
    void clearLoggingContext() {
        MDC.clear();
    }

    @Test
    void unexpectedErrorsReturnASafeMessageAndCorrelationId() {
        MDC.put(RequestLoggingFilter.REQUEST_ID_MDC_KEY, "error-request-123");

        var response = new ApiErrorHandler().unexpectedError(
                new IllegalStateException("sensitive database and filesystem details"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("code", "internal_error")
                .containsEntry("message", "An unexpected error occurred")
                .containsEntry("requestId", "error-request-123");
        assertThat(response.getBody().toString()).doesNotContain("sensitive", "filesystem");
    }
}
