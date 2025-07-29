package com.example.bankcards.service;

import com.example.bankcards.dto.user.BalanceDto;
import com.example.bankcards.dto.user.EditUserRequest;
import com.example.bankcards.dto.user.UserDto;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDto findByUsername(String username);
    UserDto findById(UUID id);
    List<UserDto> findAllUsers(Integer pageNumber, Integer pageSize, boolean ascending);
    void deleteUser(UUID userId);
    void updateUser(UUID id, EditUserRequest request);
    BalanceDto getBalanceForUser(UUID id);
}