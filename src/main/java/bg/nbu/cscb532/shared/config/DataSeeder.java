package bg.nbu.cscb532.shared.config;

import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.EmployeeRepository;
import bg.nbu.cscb532.employee.OfficeClerk;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.User;
import bg.nbu.cscb532.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Executing DataSeeder to ensure default staff users exist...");

        String defaultPassword = passwordEncoder.encode("password123");

        // 1. Admin
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User() {};
            Courier adminEmployee = new Courier();
            adminEmployee.setUsername("admin");
            adminEmployee.setEmail("admin@logistics.com");
            adminEmployee.setPassword(defaultPassword);
            adminEmployee.setFirstName("System");
            adminEmployee.setLastName("Admin");
            adminEmployee.setApplicationRole(ApplicationRole.ADMIN);
            adminEmployee.setActive(true);
            adminEmployee.setEmailVerified(true);
            adminEmployee.setEmployeeNumber("EMP-ADMIN-01");
            adminEmployee.setHireDate(LocalDate.now());
            adminEmployee.setSalary(BigDecimal.valueOf(5000.00));
            employeeRepository.save(adminEmployee);
            log.info("Seeded Admin user.");
        }

        // 2. Clerk
        if (userRepository.findByUsername("clerk").isEmpty()) {
            Office office = officeRepository.findById(1L).orElse(null);
            if (office != null) {
                OfficeClerk clerk = new OfficeClerk();
                clerk.setUsername("clerk");
                clerk.setEmail("clerk@logistics.com");
                clerk.setPassword(defaultPassword);
                clerk.setFirstName("Office");
                clerk.setLastName("Clerk");
                clerk.setApplicationRole(ApplicationRole.CLERK);
                clerk.setActive(true);
                clerk.setEmailVerified(true);
                clerk.setEmployeeNumber("EMP-CLERK-01");
                clerk.setHireDate(LocalDate.now());
                clerk.setSalary(BigDecimal.valueOf(2500.00));
                clerk.setOffice(office);
                employeeRepository.save(clerk);
                log.info("Seeded Clerk user.");
            } else {
                log.warn("Cannot seed clerk: Office ID 1 not found. Run V8 migration first.");
            }
        }

        // 3. Courier
        if (userRepository.findByUsername("courier").isEmpty()) {
            Courier courier = new Courier();
            courier.setUsername("courier");
            courier.setEmail("courier@logistics.com");
            courier.setPassword(defaultPassword);
            courier.setFirstName("Speedy");
            courier.setLastName("Courier");
            courier.setApplicationRole(ApplicationRole.COURIER);
            courier.setActive(true);
            courier.setEmailVerified(true);
            courier.setEmployeeNumber("EMP-COUR-01");
            courier.setHireDate(LocalDate.now());
            courier.setSalary(BigDecimal.valueOf(3000.00));
            employeeRepository.save(courier);
            log.info("Seeded Courier user.");
        }
    }
}
