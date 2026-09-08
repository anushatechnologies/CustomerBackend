package com.example.project.customer.service;

import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Integer userId) {
        log.debug("Fetching addresses for customerId: {}", userId);
        return addressRepository.findByCustomer_CustomerIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Integer userId, Integer id) {
        log.debug("Fetching address id: {} for customerId: {}", id, userId);
        return mapToResponse(findUserAddress(userId, id));
    }

    @Override
    public AddressResponse createAddress(Integer userId, AddressRequest request) {
        log.info("Creating address for customerId: {}, type: {}, hasCoords: {}",
                userId, request.getAddressType(), request.getLatitude() != null);

        // Check if customer already has any address
        List<Address> existingAddresses = addressRepository.findByCustomer_CustomerIdOrderByIsDefaultDescCreatedAtDesc(userId);
        boolean isFirstAddress = existingAddresses.isEmpty();

        boolean shouldBeDefault = isFirstAddress || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault) {
            clearCustomerDefault(userId);
        }

        Customer customer = customerRepository.findById(userId)
                .orElseGet(() -> Customer.builder().customerId(userId).name("Primary Customer").phone("9876543210").build());

        String recipientName = request.getRecipientName() != null && !request.getRecipientName().isBlank()
                ? request.getRecipientName() : customer.getName();
        String phone = request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone() : (customer.getPhone() != null ? customer.getPhone() : "9876543210");
        String siteName = resolveSiteName(request);

        Address address = Address.builder()
                .customer(Customer.builder().customerId(userId).build())
                .siteName(siteName)
                .recipientName(recipientName)
                .phone(phone)
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .houseFlatNo(request.getHouseFlatNo())
                .areaLocality(request.getAreaLocality())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry() : "India")
                .pincode(request.getPincode())
                .landmark(request.getLandmark())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .addressType(request.getAddressType() != null ? request.getAddressType().toUpperCase() : "OTHER")
                .isDefault(shouldBeDefault)
                .hasHeavyVehicleAccess(request.getHasHeavyVehicleAccess() != null ? request.getHasHeavyVehicleAccess() : true)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address created successfully with id: {} for customer: {}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    @Override
    public AddressResponse updateAddress(Integer userId, Integer id, AddressRequest request) {
        log.info("Updating address id: {} for customerId: {}", id, userId);
        Address address = findUserAddress(userId, id);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearCustomerDefault(userId);
            address.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        if (request.getSiteName() != null && !request.getSiteName().isBlank()) {
            address.setSiteName(request.getSiteName());
        }
        if (request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
            address.setRecipientName(request.getRecipientName());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            address.setPhone(request.getPhone());
        }
        if (request.getAddressLine1() != null && !request.getAddressLine1().isBlank()) {
            address.setAddressLine1(request.getAddressLine1());
        }
        address.setAddressLine2(request.getAddressLine2());
        address.setHouseFlatNo(request.getHouseFlatNo());
        address.setAreaLocality(request.getAreaLocality());
        if (request.getCity() != null && !request.getCity().isBlank()) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null && !request.getState().isBlank()) {
            address.setState(request.getState());
        }
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            address.setCountry(request.getCountry());
        }
        if (request.getPincode() != null && !request.getPincode().isBlank()) {
            address.setPincode(request.getPincode());
        }
        address.setLandmark(request.getLandmark());
        if (request.getLatitude() != null) {
            address.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            address.setLongitude(request.getLongitude());
        }
        if (request.getAddressType() != null && !request.getAddressType().isBlank()) {
            address.setAddressType(request.getAddressType().toUpperCase());
        }
        if (request.getHasHeavyVehicleAccess() != null) {
            address.setHasHeavyVehicleAccess(request.getHasHeavyVehicleAccess());
        }

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Override
    public void deleteAddress(Integer userId, Integer id) {
        log.info("Deleting address id: {} for customerId: {}", id, userId);
        Address address = findUserAddress(userId, id);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        addressRepository.delete(address);

        // If default address was deleted, promote most recent remaining address to default
        if (wasDefault) {
            addressRepository.findFirstByCustomer_CustomerIdOrderByCreatedAtDesc(userId)
                    .ifPresent(next -> {
                        log.info("Promoting address id: {} to default for customerId: {}", next.getId(), userId);
                        next.setIsDefault(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Override
    public AddressResponse setDefaultAddress(Integer userId, Integer id) {
        log.info("Setting address id: {} as default for customerId: {}", id, userId);
        Address address = findUserAddress(userId, id);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            return mapToResponse(address);
        }

        clearCustomerDefault(userId);
        address.setIsDefault(true);
        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    private void clearCustomerDefault(Integer userId) {
        addressRepository.findByCustomer_CustomerIdAndIsDefaultTrue(userId).ifPresent(addr -> {
            addr.setIsDefault(false);
            addressRepository.save(addr);
        });
    }

    private Address findUserAddress(Integer userId, Integer id) {
        return addressRepository.findByCustomer_CustomerIdAndId(userId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
    }

    private String resolveSiteName(AddressRequest req) {
        if (req.getSiteName() != null && !req.getSiteName().isBlank()) {
            return req.getSiteName();
        }
        if (req.getAddressType() != null && !req.getAddressType().isBlank()) {
            return req.getAddressType().toUpperCase() + " Address";
        }
        if (req.getAreaLocality() != null && !req.getAreaLocality().isBlank()) {
            return req.getAreaLocality() + " Site";
        }
        return "Delivery Site";
    }

    private AddressResponse mapToResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .siteName(a.getSiteName())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .houseFlatNo(a.getHouseFlatNo())
                .areaLocality(a.getAreaLocality())
                .city(a.getCity())
                .state(a.getState())
                .country(a.getCountry())
                .pincode(a.getPincode())
                .landmark(a.getLandmark())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .addressType(a.getAddressType() != null ? a.getAddressType() : "OTHER")
                .isDefault(Boolean.TRUE.equals(a.getIsDefault()))
                .hasHeavyVehicleAccess(Boolean.TRUE.equals(a.getHasHeavyVehicleAccess()))
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
