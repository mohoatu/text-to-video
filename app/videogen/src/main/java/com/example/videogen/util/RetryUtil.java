package com.example.videogen.util;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal fixed-delay retry helper for transient failures in pipeline stages.
 */
public final class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private RetryUtil() {
    }

    public static <T> T withRetry(String stageName, int maxAttempts, long backoffMillis, Callable<T> action) throws Exception {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return action.call();
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    throw ex;
                }
                log.warn("Stage '{}' attempt {}/{} failed: {} — retrying", stageName, attempt, maxAttempts, ex.getMessage());
                Thread.sleep(backoffMillis);
            }
        }
    }
}
