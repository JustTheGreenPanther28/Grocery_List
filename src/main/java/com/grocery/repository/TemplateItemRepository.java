package com.grocery.repository;

import com.grocery.model.TemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateItemRepository extends JpaRepository<TemplateItem, Long> {
    List<TemplateItem> findByUsernameOrderByIdAsc(String username);
    Optional<TemplateItem> findByIdAndUsername(Long id, String username);
}
