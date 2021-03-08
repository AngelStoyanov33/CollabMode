package com.nullpointerexception.collabmode.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;
import java.util.regex.Pattern;

public class PasswordUtils {
    public static boolean isSecure(String passwordInput, String passwordConfirmInput, List<String> errorList) {

        Pattern UpperCasePatten = Pattern.compile("[A-Z ]");
        Pattern lowerCasePatten = Pattern.compile("[a-z ]");


        Pattern digitCasePatten = Pattern.compile("[0-9 ]");
        errorList.clear();

        boolean flag = true;

        if (!passwordInput.equals(passwordConfirmInput)) {
            errorList.add("Passwords don't match.");
            flag = false;
        }
        if (passwordInput.length() < 8) {
            errorList.add("Invalid password, minimum length is 8 characters.");
            flag = false;
        }
        if (!UpperCasePatten.matcher(passwordInput).find()) {
            errorList.add("Password must contain an uppercase character.");
            flag = false;
        }
        if (!lowerCasePatten.matcher(passwordInput).find()) {
            errorList.add("Password must contain a lowercase character.");
            flag = false;
        }
        if (!digitCasePatten.matcher(passwordInput).find()) {
            errorList.add("Password must contain a digit");
            flag = false;
        }

        return flag;

    }

    public static String hash(String password){
        return DigestUtils.sha256Hex(password);
    }
}
