package com.gym.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByQrToken(String qrToken);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRoles_Name(String roleName);

    List<User> findDistinctByRoles_NameOrderByFullName(String roleName);

    List<User> findAllByOrderByFullName();

    long countByRoles_Id(Long roleId);

    @Query("select count(distinct u) from User u join u.roles r where r.name = :roleName and u.active = true")
    long countActiveWithRole(String roleName);

    List<User> findByActiveTrueAndFullNameContainingIgnoreCaseOrderByFullName(String name);
}
