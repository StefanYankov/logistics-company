package bg.nbu.cscb532.logisticscompany;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LogisticsCompanyApplicationTests {

    @Test
    void contextLoads() {
    }

}
