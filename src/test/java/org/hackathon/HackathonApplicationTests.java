package org.hackathon;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HackathonApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(BCrypt.hashpw("123456", BCrypt.gensalt(10)));
    }

}
