package com.example.project.customer.service;

import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Integer userId) {
        Integer uid = userId != null ? userId : 101;
        List<Address> addresses = addressRepository.findByUserIdOrderByCreatedAtDesc(uid);
        return addresses.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Integer userId, Integer addressId) {
        Integer uid = userId != null ? userId : 101;
        Address address = addressRepository.findByAddressIdAndUserId(addressId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        return toResponse(address);
    }

    @Override
    public AddressResponse addAddress(Integer userId, AddressRequest request) {
        Integer uid = userId != null ? userId : 101;

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaults(uid);
        }

        Address address = new Address();
        address.setUserId(uid);
        applyRequest(address, request);

        return toResponse(addressRepository.save(address));
    }

    @Override
    public AddressResponse updateAddress(Integer userId, Integer addressId, AddressRequest request) {
        Integer uid = userId != null ? userId : 101;
        Address address = addressRepository.findByAddressIdAndUserId(addressId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            unsetOtherDefaults(uid);
        }

        applyRequest(address, request);
        return toResponse(addressRepository.save(address));
    }

    @Override
    public void deleteAddress(Integer userId, Integer addressId) {
        Integer uid = userId != null ? userId : 101;
        Address address = addressRepository.findByAddressIdAndUserId(addressId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        addressRepository.delete(address);
    }

    private void unsetOtherDefaults(Integer userId) {
        addressRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                .ifPresent(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setSiteName(request.getSiteName());
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        address.setDefault(request.getIsDefault());
        address.setHasHeavyVehicleAccess(request.getHasHeavyVehicleAccess());
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(
                a.getAddressId(),
                a.getUserId(),
                a.getSiteName(),
                a.getRecipientName(),
                a.getPhone(),
                a.getAddressLine1(),
                a.getCity(),
                a.getState(),
                a.getPincode(),
                a.getLandmark(),
                a.isDefault(),
                a.isHasHeavyVehicleAccess(),
                a.getCreatedAt()
        );
    }
}
