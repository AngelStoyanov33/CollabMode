package com.nullpointerexception.collabmode.util;

import java.util.regex.Pattern;

public class EmailUtils {
    public static boolean isValid(String email){
        if (email == null) {
            return false;
        }
        final String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);

        return pat.matcher(email).matches();
    }

}