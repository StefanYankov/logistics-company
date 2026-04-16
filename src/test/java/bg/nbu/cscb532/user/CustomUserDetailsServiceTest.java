package bg.nbu.cscb532.user;

import bg.nbu.cscb532.client.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsServiceImpl userDetailsService;

    // --- TEST DATA FACTORY ---
    private Client createTestUser(String username, boolean isActive, ApplicationRole role) {
        Client user = new Client();
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setActive(isActive);
        user.setApplicationRole(role);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(username + "@example.com");
        user.setPhoneNumber("1234567890");
        return user;
    }

    @Nested
    @DisplayName("loadUserByUsername(String) Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Happy Path: Should return UserDetails when user is found and active")
        void shouldReturnUserDetails_WhenUserIsFoundAndActive() {

            // Arrange
            String username = "activeuser";
            Client userEntity = createTestUser(username, true, ApplicationRole.CLIENT);
            given(userRepository.findByUsername(username)).willReturn(Optional.of(userEntity));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(username);
            assertThat(userDetails.getPassword()).isEqualTo("hashed-password");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_CLIENT");

            verify(userRepository).findByUsername(username);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw UsernameNotFoundException when user is not found")
        void shouldThrowException_WhenUserNotFound() {
            // Arrange
            String username = "nonexistent";
            given(userRepository.findByUsername(username)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: " + username);

            verify(userRepository).findByUsername(username);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Edge Case: Should return UserDetails with isEnabled=false when user is inactive")
        void shouldReturnDisabledUserDetails_WhenUserIsInactive() {

            // Arrange
            String username = "inactiveuser";
            Client userEntity = createTestUser(username, false, ApplicationRole.ADMIN);
            given(userRepository.findByUsername(username)).willReturn(Optional.of(userEntity));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(username);
            assertThat(userDetails.isEnabled()).isFalse();
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");

            verify(userRepository).findByUsername(username);
            verifyNoMoreInteractions(userRepository);
        }
    }
}