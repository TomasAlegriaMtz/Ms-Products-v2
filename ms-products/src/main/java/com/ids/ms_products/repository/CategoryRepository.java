package com.ids.ms_products.repository;

import com.ids.ms_products.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Page<Category> findByActiveTrue(Pageable pageable);

}