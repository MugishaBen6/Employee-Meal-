package com.emeal;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    @Test
    public void printHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("BCRYPT_HASH_FOR_PASSWORD123: " + encoder.encode("Password123!"));
    }
}
