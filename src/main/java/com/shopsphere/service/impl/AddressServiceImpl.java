package com.shopsphere.service.impl;

import com.shopsphere.dto.request.AddressRequest;
import com.shopsphere.dto.response.AddressResponse;
import com.shopsphere.entity.Address;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.exception.ForbiddenException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.mapper.AddressMapper;
import com.shopsphere.repository.AddressRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        log.info("Adding address for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || addressRepository.findByUserId(userId).isEmpty(); // first address is always default

        if (makeDefault) {
            clearExistingDefault(userId);
        }

        Address address = Address.builder()
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .addressType(request.getAddressType() == null ? AddressType.BOTH : request.getAddressType())
                .isDefault(makeDefault)
                .user(user)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address added: id={} for userId={}", saved.getId(), userId);

        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        log.info("Updating addressId={} for userId={}", addressId, userId);

        Address address = findAddressOrThrow(addressId);
        assertOwnership(address, userId);

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearExistingDefault(userId);
        }

        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }
        if (request.getIsDefault() != null) {
            address.setIsDefault(makeDefault);
        }

        Address updated = addressRepository.save(address);
        log.info("Address updated: id={}", updated.getId());

        return addressMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting addressId={} for userId={}", addressId, userId);
        Address address = findAddressOrThrow(addressId);
        assertOwnership(address, userId);
        addressRepository.delete(address);
        log.info("Address deleted: id={}", addressId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesForUser(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Default address", "userId", userId));
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private Address findAddressOrThrow(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
    }

    private void assertOwnership(Address address, Long userId) {
        if (!address.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify this address.");
        }
    }

    private void clearExistingDefault(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(existingDefault -> {
            existingDefault.setIsDefault(false);
            addressRepository.save(existingDefault);
        });
    }
}
