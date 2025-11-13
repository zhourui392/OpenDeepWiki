package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户Repository
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * 根据用户名查询用户
     *
     * @param name 用户名
     * @return 用户信息
     */
    Optional<UserEntity> findByName(String name);

    /**
     * 判断邮箱是否存在
     *
     * @param email 邮箱
     * @return true-存在，false-不存在
     */
    boolean existsByEmail(String email);

    /**
     * 判断用户名是否存在
     *
     * @param name 用户名
     * @return true-存在，false-不存在
     */
    boolean existsByName(String name);
}
