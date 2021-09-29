package com.gradle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static com.gradle.Utils.stripUserInfo;

public class UtilsTest {

    @Test
    public void test_stripUserInfo_givenUriContainingUsernameAndPassword_strips() {
        URI uri = URI.create("https://username:password@example.com");
        URI result = stripUserInfo(uri);
        Assertions.assertEquals("https://example.com", result.toString());
    }

    @Test
    public void test_stripUserInfo_givenUriWithNoUserInfo_doesNothing() {
        URI uri = URI.create("https://example.com");
        URI result = stripUserInfo(uri);
        Assertions.assertEquals(uri.toString(), result.toString());
    }
}
