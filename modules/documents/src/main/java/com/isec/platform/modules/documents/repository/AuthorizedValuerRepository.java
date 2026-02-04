package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuthorizedValuerRepository extends JpaRepository<AuthorizedValuer, Long> {
    List<AuthorizedValuer> findByActiveTrue();
}
