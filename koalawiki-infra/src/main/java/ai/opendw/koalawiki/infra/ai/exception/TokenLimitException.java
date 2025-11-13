package ai.opendw.koalawiki.infra.ai.exception;

/**
 * Token超限异常
 */
public class TokenLimitException extends AIException {

    public TokenLimitException(String message) {
        super(message);
    }

    public TokenLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
