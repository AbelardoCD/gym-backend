package com.gym.billing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipReminderRepository extends JpaRepository<MembershipReminder, MembershipReminder.Key> {
}
