package bg.nbu.cscb532.shared.config;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.company.Company;
import bg.nbu.cscb532.company.CompanyRepository;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.EmployeeRepository;
import bg.nbu.cscb532.employee.OfficeClerk;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final CityRepository cityRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Executing DataSeeder...");

        seedCompany();
        seedStaff();
    }

    private void seedCompany() {
        if (companyRepository.count() == 0) {
            List<City> sofiaList = cityRepository.findAllByName("Sofia");
            City sofia = sofiaList.stream().findFirst().orElseGet(() -> {
                City city = new City();
                city.setName("Sofia");
                city.setPostcode("1000");
                return cityRepository.save(city);
            });

            Company company = new Company();
            company.setName("LogisticsCo");
            company.setRegistrationNumber("123456789");
            AddressDetails address = new AddressDetails();
            address.setCity(sofia);
            address.setStreet("Tsarigradsko Shose 115");
            company.setAddressDetails(address);
            companyRepository.save(company);
            log.info("Seeded default company.");
        }
    }

    private void seedStaff() {
        String defaultPassword = passwordEncoder.encode("password123");

        // 1. Admin
        if (userRepository.findByUsername("admin").isEmpty()) {
            Client adminUser = new Client();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@logistics.com");
            adminUser.setPassword(defaultPassword);
            adminUser.setFirstName("System");
            adminUser.setLastName("Admin");
            adminUser.setPhoneNumber("0888000000"); // Added default phone number
            adminUser.setApplicationRole(ApplicationRole.ADMIN);
            adminUser.setActive(true);
            adminUser.setEmailVerified(true);
            userRepository.save(adminUser);
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
