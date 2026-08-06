package com.minecompanion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MineCompanionApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring application context starts without errors.
    }
}
