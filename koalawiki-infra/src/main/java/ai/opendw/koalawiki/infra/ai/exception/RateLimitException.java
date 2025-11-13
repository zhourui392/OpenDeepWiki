package ai.opendw.koalawiki.infra.ai.exception;

/**
 * API限流异常
 */
public class RateLimitException extends AIException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
