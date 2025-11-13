package ai.opendw.koalawiki.core.exception;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found: %s", resourceType, resourceId));
    }
}
