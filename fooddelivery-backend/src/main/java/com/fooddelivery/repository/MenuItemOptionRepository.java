package com.fooddelivery.repository;

import com.fooddelivery.entity.MenuItemOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemOptionRepository extends JpaRepository<MenuItemOption, String> {
}
