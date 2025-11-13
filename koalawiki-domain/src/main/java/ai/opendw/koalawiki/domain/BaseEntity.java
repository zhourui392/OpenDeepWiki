package ai.opendw.koalawiki.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础实体类
 * 所有领域实体都应该继承这个类
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID (String类型)
     */
    private String id;

    /**
     * 创建时间
     */
    private Date createdAt;
}
