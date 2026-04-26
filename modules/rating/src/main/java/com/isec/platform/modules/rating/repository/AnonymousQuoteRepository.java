package com.isec.platform.modules.rating.repository;

import com.isec.platform.modules.rating.domain.AnonymousQuote;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnonymousQuoteRepository extends ReactiveCrudRepository<AnonymousQuote, String> {
}
