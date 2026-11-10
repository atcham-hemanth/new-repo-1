package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloWorldTest {

    @Test
    void testGetMessage() {
        assertEquals(
            "Hello from Hemanth App!\n" +
            "Build, Test, Scan and Deploy - Automated through Jenkins CI/CD!",
            HelloWorld.getMessage()
        );
    }
}
