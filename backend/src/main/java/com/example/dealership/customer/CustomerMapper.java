package com.example.dealership.customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static Customer toEntity(CustomerRequest request) {
        return new Customer(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.nip());
    }

    public static void updateEntity(Customer customer, CustomerRequest request) {
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setNip(request.nip());
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getNip());
    }
}
