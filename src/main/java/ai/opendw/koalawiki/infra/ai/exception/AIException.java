package ai.opendw.koalawiki.infra.ai.exception;

/**
 * AI服务异常基类
 */
public class AIException extends RuntimeException {

    public AIException(String message) {
        super(message);
    }

    public AIException(String message, Throwable cause) {
        super(message, cause);
    }
}
