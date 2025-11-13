package ai.opendw.koalawiki.core.exception;

/**
 * 未授权异常
 */
public class UnauthorizedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException() {
        super("UNAUTHORIZED", "Unauthorized access");
    }
}
