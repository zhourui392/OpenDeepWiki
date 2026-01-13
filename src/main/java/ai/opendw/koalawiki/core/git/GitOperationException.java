package ai.opendw.koalawiki.core.git;

/**
 * Git操作异常
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
public class GitOperationException extends RuntimeException {

    public GitOperationException(String message) {
        super(message);
    }

    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}