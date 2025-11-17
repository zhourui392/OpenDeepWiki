package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.AIPromptTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI提示词模板仓储
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-18
 */
@Repository
public interface AIPromptTemplateRepository extends JpaRepository<AIPromptTemplateEntity, String> {

    /**
     * 根据提示词类型和Agent类型查询默认模板
     */
    Optional<AIPromptTemplateEntity> findByPromptTypeAndAgentTypeAndIsActiveAndIsDefault(
            String promptType, String agentType, Boolean isActive, Boolean isDefault);

    /**
     * 根据提示词类型和Agent类型查询所有启用的模板
     */
    List<AIPromptTemplateEntity> findByPromptTypeAndAgentTypeAndIsActive(
            String promptType, String agentType, Boolean isActive);

    /**
     * 根据提示词类型查询所有启用的模板
     */
    List<AIPromptTemplateEntity> findByPromptTypeAndIsActive(String promptType, Boolean isActive);

    /**
     * 根据Agent类型查询所有启用的模板
     */
    List<AIPromptTemplateEntity> findByAgentTypeAndIsActive(String agentType, Boolean isActive);
}
