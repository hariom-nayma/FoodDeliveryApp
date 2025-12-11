package com.fooddelivery.service;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordValidationService {

    private final Zxcvbn zxcvbn = new Zxcvbn();

    public void validatePassword(String password, String name, String email, String phone) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }

        // Check for personal info
        List<String> personalInfo = new ArrayList<>();
        if (name != null)
            personalInfo.addAll(Arrays.asList(name.split(" ")));
        if (email != null)
            personalInfo.add(email.split("@")[0]);
        if (phone != null)
            personalInfo.add(phone);

        String lowercasePassword = password.toLowerCase();
        for (String info : personalInfo) {
            if (info.length() > 2 && lowercasePassword.contains(info.toLowerCase())) {
                throw new RuntimeException("Password contains personal information (" + info + ")");
            }
        }

        // Check entropy
        Strength strength = zxcvbn.measure(password, personalInfo);
        if (strength.getScore() < 2) { // Score 0-4. 2 is "fair".
            throw new RuntimeException("Password is too weak. Try adding more unique words or characters.");
        }
    }
}
