package com.vslbackend.repository;

import com.vslbackend.entity.AttemptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttemptHistoryRepository extends JpaRepository<AttemptHistory, Long> {
}
