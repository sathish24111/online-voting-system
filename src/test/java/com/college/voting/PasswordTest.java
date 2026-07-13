package com.college.voting;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {

    @Test
    public void testPasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("------------------------------------------------------------------");
        System.out.println("GENERATED 12345678: " + encoder.encode("12345678"));
        System.out.println("------------------------------------------------------------------");
    }
}
