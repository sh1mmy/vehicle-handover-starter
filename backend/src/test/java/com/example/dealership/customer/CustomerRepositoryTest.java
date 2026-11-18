package com.example.dealership.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class CustomerRepositoryTest {

    private static final String EMAIL = "repository.first@example.test";
    private static final String OTHER_EMAIL = "repository.second@example.test";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void existsByEmailAndIdNotFindsEmailOfAnotherCustomer() {
        Customer first = customerRepository.save(
                new Customer("Ewa", "Lis", EMAIL, "600100200", null));
        Customer second = customerRepository.save(
                new Customer("Adam", "Wolny", OTHER_EMAIL, "600100201", null));
        entityManager.flush();

        assertThat(customerRepository.existsByEmailAndIdNot(EMAIL, second.getId())).isTrue();
        assertThat(customerRepository.existsByEmailAndIdNot(EMAIL, first.getId())).isFalse();
        assertThat(customerRepository.existsByEmail("repository.third@example.test")).isFalse();
    }

    @Test
    void saveStoresCustomerThatCanBeReadBack() {
        Customer saved = customerRepository.save(
                new Customer("Ewa", "Lis", EMAIL, "+48 600-100-200", "1234567890"));
        entityManager.flush();
        entityManager.clear();

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found).isNotSameAs(saved);
        assertThat(found.getFirstName()).isEqualTo("Ewa");
        assertThat(found.getLastName()).isEqualTo("Lis");
        assertThat(found.getEmail()).isEqualTo(EMAIL);
        assertThat(found.getPhone()).isEqualTo("+48 600-100-200");
        assertThat(found.getNip()).isEqualTo("1234567890");
        assertThat(customerRepository.existsByEmail(EMAIL)).isTrue();
        assertThat(customerRepository.existsByEmailAndIdNot(EMAIL, saved.getId())).isFalse();
    }
}
