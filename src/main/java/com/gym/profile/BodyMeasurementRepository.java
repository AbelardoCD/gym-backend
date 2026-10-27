package com.gym.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, Long> {
    List<BodyMeasurement> findByUserIdOrderByMeasuredOnAscIdAsc(Long userId);

    List<BodyMeasurement> findAllByOrderByMeasuredOnAscIdAsc();
}
