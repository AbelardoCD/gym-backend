package com.gym.billing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCase(String name);
}
