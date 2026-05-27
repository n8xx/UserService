package com.example.userservice.repository.specification;

import com.example.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return null;
            return cb.like(cb.lower(root.<String>get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, cb) -> {
            if (surname == null || surname.isBlank()) return null;
            return cb.like(cb.lower(root.<String>get("surname")), "%" + surname.toLowerCase() + "%");
        };
    }
}