package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色Repository
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    /**
     * 根据角色名称查询
     *
     * @param name 角色名称
     * @return 角色信息
     */
    Optional<RoleEntity> findByName(String name);

    /**
     * 判断角色名称是否存在
     *
     * @param name 角色名称
     * @return true-存在，false-不存在
     */
    boolean existsByName(String name);
}
