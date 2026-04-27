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
import java.util.UUID;

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
    private Client createTestUser(UUID id, String username, boolean isActive, ApplicationRole role) {
        Client user = new Client();
        user.setId(id);
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
        @DisplayName("Happy Path: Should return CustomUserDetails when user is found and active")
        void shouldReturnUserDetails_WhenUserIsFoundAndActive() {

            // Arrange
            String username = "activeuser";
            UUID userId = UUID.randomUUID();
            Client userEntity = createTestUser(userId, username, true, ApplicationRole.CLIENT);
            given(userRepository.findByUsername(username)).willReturn(Optional.of(userEntity));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(username);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(CustomUserDetails.class);
            
            CustomUserDetails customUserDetails = (CustomUserDetails) result;
            assertThat(customUserDetails.getId()).isEqualTo(userId);
            assertThat(customUserDetails.getApplicationRole()).isEqualTo(ApplicationRole.CLIENT);
            
            assertThat(customUserDetails.getUsername()).isEqualTo(username);
            assertThat(customUserDetails.getPassword()).isEqualTo("hashed-password");
            assertThat(customUserDetails.isEnabled()).isTrue();
            assertThat(customUserDetails.getAuthorities())
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
        @DisplayName("Edge Case: Should return CustomUserDetails with isEnabled=false when user is inactive")
        void shouldReturnDisabledUserDetails_WhenUserIsInactive() {

            // Arrange
            String username = "inactiveuser";
            UUID userId = UUID.randomUUID();
            Client userEntity = createTestUser(userId, username, false, ApplicationRole.ADMIN);
            given(userRepository.findByUsername(username)).willReturn(Optional.of(userEntity));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(username);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(CustomUserDetails.class);
            
            CustomUserDetails customUserDetails = (CustomUserDetails) result;
            assertThat(customUserDetails.getId()).isEqualTo(userId);
            assertThat(customUserDetails.getApplicationRole()).isEqualTo(ApplicationRole.ADMIN);

            assertThat(customUserDetails.getUsername()).isEqualTo(username);
            assertThat(customUserDetails.isEnabled()).isFalse();
            assertThat(customUserDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");

            verify(userRepository).findByUsername(username);
            verifyNoMoreInteractions(userRepository);
        }
    }
}