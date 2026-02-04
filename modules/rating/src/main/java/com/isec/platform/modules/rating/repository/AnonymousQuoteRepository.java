package com.isec.platform.modules.rating.repository;

import com.isec.platform.modules.rating.domain.AnonymousQuote;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnonymousQuoteRepository extends CrudRepository<AnonymousQuote, String> {
}
