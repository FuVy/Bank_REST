package com.example.bankcards.service.jpa;

import com.example.bankcards.dto.user.BalanceDto;
import com.example.bankcards.dto.user.EditUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.RoleConsts;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.user.UserAlreadyExistsException;
import com.example.bankcards.exception.user.UserNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JpaUserService implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CardRepository cardRepository;

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE = 0;

    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "') or " +
            "(#username.equals(authentication.principal.username))")
    public UserDto findByUsername(String username) {
        return userMapper.toDto(userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username)));
    }

    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers(Integer pageNumber, Integer pageSize, boolean ascendingCreationDate) {
        Sort.Order creationDateOrder = new Sort.Order(ascendingCreationDate ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        return userRepository.findAll(
                        buildPageRequest(pageNumber, pageSize, creationDateOrder))
                .getContent().stream().map(userMapper::toDto).toList();
    }

    @Transactional
    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    public void updateUser(UUID id, EditUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        changeUsername(user, request.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "') or " +
            "(#id.equals(authentication.principal.uuid))")
    public BalanceDto getBalanceForUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        return new BalanceDto(id, cardRepository.sumBalanceByOwnerId(id));
    }

    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    @Transactional(propagation = Propagation.REQUIRED)
    private void changeUsername(User user, String newUsername) {
        if (newUsername == null) return;
        if (user.getUsername().equals(newUsername)) return;
        if (userRepository.existsByUsername(newUsername)) {
            throw new UserAlreadyExistsException(newUsername);
        }
        user.setUsername(newUsername);
    }

    private PageRequest buildPageRequest(Integer pageNumber, Integer pageSize, Sort.Order... orders) {
        int queryPageNumber;
        int queryPageSize;

        if (pageNumber != null && pageNumber > 0) {
            queryPageNumber = pageNumber - 1;
        } else {
            queryPageNumber = DEFAULT_PAGE;
        }

        if (pageSize == null || pageSize < 1) {
            queryPageSize = DEFAULT_PAGE_SIZE;
        } else {
            queryPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        }
        Sort sort = Sort.by(orders);

        return PageRequest.of(queryPageNumber, queryPageSize, sort);
    }
}