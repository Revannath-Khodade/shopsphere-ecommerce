package com.shopsphere.service;

import com.shopsphere.dto.request.AddressRequest;
import com.shopsphere.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    AddressResponse addAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    List<AddressResponse> getAddressesForUser(Long userId);

    AddressResponse getDefaultAddress(Long userId);

    /** Marks an existing address as the user's default, clearing any previous default. */
    AddressResponse setDefaultAddress(Long userId, Long addressId);
}
