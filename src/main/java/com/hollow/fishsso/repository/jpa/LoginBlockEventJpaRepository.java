package com.hollow.fishsso.repository.jpa;

import com.hollow.fishsso.model.LoginBlockEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginBlockEventJpaRepository extends JpaRepository<LoginBlockEvent, Long> {
}
