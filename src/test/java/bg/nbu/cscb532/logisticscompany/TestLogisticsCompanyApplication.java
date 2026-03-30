package bg.nbu.cscb532.logisticscompany;

import org.springframework.boot.SpringApplication;

public class TestLogisticsCompanyApplication {

    public static void main(String[] args) {
        SpringApplication.from(LogisticsCompanyApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
