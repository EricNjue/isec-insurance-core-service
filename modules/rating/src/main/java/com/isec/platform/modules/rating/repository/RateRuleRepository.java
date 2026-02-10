package com.isec.platform.modules.rating.repository;

import com.isec.platform.modules.rating.domain.RateRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateRuleRepository extends JpaRepository<RateRule, Long> {
    List<RateRule> findAllByRateBookId(Long rateBookId);
}
