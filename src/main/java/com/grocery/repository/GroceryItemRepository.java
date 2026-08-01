package com.grocery.repository;

import com.grocery.model.GroceryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    List<GroceryItem> findByUsernameAndItemDateOrderByIdAsc(String username, LocalDate itemDate);

    Optional<GroceryItem> findByIdAndUsername(Long id, String username);
}
