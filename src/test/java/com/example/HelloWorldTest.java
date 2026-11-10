package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloWorldTest {

    @Test
    void testGetMessage() {
        assertEquals(
            "Hello from Hemanth App!",
            HelloWorld.getMessage()
        );
    }
}
