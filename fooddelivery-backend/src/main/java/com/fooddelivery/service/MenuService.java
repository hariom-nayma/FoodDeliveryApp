package com.fooddelivery.service;

import com.fooddelivery.dto.request.MenuItemRequest;
import com.fooddelivery.dto.request.OptionGroupRequest;
import com.fooddelivery.dto.request.OptionRequest;
import com.fooddelivery.dto.response.MenuItemResponse;
import com.fooddelivery.dto.response.OptionDto;
import com.fooddelivery.dto.response.OptionGroupDto;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.CategoryRepository;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public MenuItemResponse createMenuItem(String restaurantId, MenuItemRequest request, MultipartFile image) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getRestaurant().getId().equals(restaurantId)) {
            throw new RuntimeException("Category does not belong to this restaurant");
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(image);
        }

        MenuItem menuItem = MenuItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .foodType(request.getFoodType() != null ? FoodType.valueOf(request.getFoodType()) : null)
                .isAvailable(request.isAvailable())
                .imageUrl(imageUrl)
                .build();

        // Handle Options
        if (request.getOptionGroups() != null) {
            for (OptionGroupRequest groupRequest : request.getOptionGroups()) {
                MenuItemOptionGroup group = MenuItemOptionGroup.builder()
                        .name(groupRequest.getName())
                        .isMultiSelect("MULTI_SELECT".equalsIgnoreCase(groupRequest.getType()))
                        .isRequired(groupRequest.isRequired())
                        .menuItem(menuItem) // Link parent
                        .build();

                if (groupRequest.getOptions() != null) {
                    for (OptionRequest optReq : groupRequest.getOptions()) {
                        MenuItemOption option = MenuItemOption.builder()
                                .label(optReq.getLabel())
                                .extraPrice(optReq.getExtraPrice())
                                .optionGroup(group) // Link parent
                                .build();
                        group.addOption(option);
                    }
                }
                menuItem.addOptionGroup(group);
            }
        }

        MenuItem saved = menuItemRepository.save(menuItem);
        return mapToResponse(saved);
    }

    public List<MenuItemResponse> getMenuItems(String restaurantId) {
        List<MenuItem> items = menuItemRepository.findByRestaurantId(restaurantId);
        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private MenuItemResponse mapToResponse(MenuItem item) {
        List<OptionGroupDto> groups = item.getOptionGroups().stream().map(g ->
                OptionGroupDto.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .multiSelect(g.isMultiSelect())
                        .required(g.isRequired())
                        .options(g.getOptions().stream().map(o ->
                                OptionDto.builder()
                                        .id(o.getId())
                                        .label(o.getLabel())
                                        .extraPrice(o.getExtraPrice())
                                        .build()
                        ).collect(Collectors.toList()))
                        .build()
        ).collect(Collectors.toList());

        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .basePrice(item.getBasePrice())
                .foodType(item.getFoodType() != null ? item.getFoodType().name() : null)
                .available(item.isAvailable())
                .imageUrl(item.getImageUrl())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .optionGroups(groups)
                .build();
    }
}
