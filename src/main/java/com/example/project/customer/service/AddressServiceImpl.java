package com.example.project.customer.service;

import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses() {
        return addressRepository.findAllByOrderByIsDefaultDescCreatedAtDesc()
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Integer id) {
        return mapToResponse(findAddress(id));
    }

    @Override
    public AddressResponse createAddress(AddressRequest request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByIsDefaultTrue().ifPresent(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
        }

        Address address = Address.builder()
                .siteName(request.getSiteName())
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .landmark(request.getLandmark())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .hasHeavyVehicleAccess(request.getHasHeavyVehicleAccess() != null ? request.getHasHeavyVehicleAccess() : true)
                .build();

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    public AddressResponse updateAddress(Integer id, AddressRequest request) {
        Address address = findAddress(id);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByIsDefaultTrue().ifPresent(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
        }

        address.setSiteName(request.getSiteName());
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
        if (request.getHasHeavyVehicleAccess() != null) {
            address.setHasHeavyVehicleAccess(request.getHasHeavyVehicleAccess());
        }

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    public void deleteAddress(Integer id) {
        Address address = findAddress(id);
        addressRepository.delete(address);
    }

    private Address findAddress(Integer id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
    }

    private AddressResponse mapToResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .siteName(a.getSiteName())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .landmark(a.getLandmark())
                .isDefault(Boolean.TRUE.equals(a.getIsDefault()))
                .hasHeavyVehicleAccess(Boolean.TRUE.equals(a.getHasHeavyVehicleAccess()))
                .createdAt(a.getCreatedAt())
                .build();
    }
}
