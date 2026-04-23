package com.company.companyservice.unit.domain.model;

import com.company.companyservice.domain.model.Address;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressTest {

    @Test
    void createsValidAddress() {
        Address address = new Address("123 Main St", "Paris", "75001", "France");

        assertThat(address.street()).isEqualTo("123 Main St");
        assertThat(address.city()).isEqualTo("Paris");
        assertThat(address.postalCode()).isEqualTo("75001");
        assertThat(address.country()).isEqualTo("France");
    }

    @Test
    void rejectsNullStreet() {
        assertThatThrownBy(() -> new Address(null, "Paris", "75001", "France"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("street");
    }

    @Test
    void rejectsBlankStreet() {
        assertThatThrownBy(() -> new Address("   ", "Paris", "75001", "France"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("street");
    }

    @Test
    void rejectsNullCity() {
        assertThatThrownBy(() -> new Address("123 Main St", null, "75001", "France"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city");
    }

    @Test
    void rejectsBlankCity() {
        assertThatThrownBy(() -> new Address("123 Main St", "", "75001", "France"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city");
    }

    @Test
    void rejectsNullPostalCode() {
        assertThatThrownBy(() -> new Address("123 Main St", "Paris", null, "France"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postalCode");
    }

    @Test
    void rejectsBlankPostalCode() {
        assertThatThrownBy(() -> new Address("123 Main St", "Paris", " ", "France"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postalCode");
    }

    @Test
    void rejectsNullCountry() {
        assertThatThrownBy(() -> new Address("123 Main St", "Paris", "75001", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country");
    }

    @Test
    void rejectsBlankCountry() {
        assertThatThrownBy(() -> new Address("123 Main St", "Paris", "75001", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country");
    }

    @Test
    void isEqualByValue() {
        Address a1 = new Address("123 Main St", "Paris", "75001", "France");
        Address a2 = new Address("123 Main St", "Paris", "75001", "France");

        assertThat(a1).isEqualTo(a2);
    }
}
