package com.company.officerservice.domain.model;

public record Address(String street, String city, String postalCode, String country) {

    public Address {
        street = requireNonBlank(street, "street");
        city = requireNonBlank(city, "city");
        postalCode = requireNonBlank(postalCode, "postalCode");
        country = requireNonBlank(country, "country");
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
