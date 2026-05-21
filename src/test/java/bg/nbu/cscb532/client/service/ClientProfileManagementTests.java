package bg.nbu.cscb532.client.service;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.dto.ClientUpdateDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService: Profile Management Tests")
public class ClientProfileManagementTests extends AbstractClientUnitTestBase {

    @Nested
    @DisplayName("updateClientProfile(UUID, ClientUpdateDto) Tests")
    class UpdateClientProfileTests {

        @Test
        @DisplayName("Happy Path: Should successfully update client profile")
        void shouldUpdateClientProfileSuccessfully() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            ClientUpdateDto dto = ClientUpdateDto.builder()
                    .firstName("NewFirstName")
                    .lastName("NewLastName")
                    .phoneNumber("0888111222")
                    .build();

            Client existingClient = createMockSavedClient();
            existingClient.setId(clientId);
            existingClient.setPhoneNumber("0888000000");

            given(clientRepository.findById(clientId)).willReturn(Optional.of(existingClient));
            given(clientRepository.findByPhoneNumber(dto.phoneNumber())).willReturn(Optional.empty());
            given(clientRepository.save(any(Client.class))).willReturn(existingClient);

            // Act
            ClientViewDto result = clientService.updateClientProfile(clientId, dto);

            // Assert
            assertThat(result).isNotNull();
            verify(clientRepository).save(clientCaptor.capture());

            Client savedClient = clientCaptor.getValue();
            assertThat(savedClient.getFirstName()).isEqualTo("NewFirstName");
            assertThat(savedClient.getLastName()).isEqualTo("NewLastName");
            assertThat(savedClient.getPhoneNumber()).isEqualTo("0888111222");
        }

        @Test
        @DisplayName("Edge Case: Should bypass duplicate phone check if phone number hasn't changed")
        void shouldBypassDuplicateCheckIfPhoneUnchanged() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            ClientUpdateDto dto = ClientUpdateDto.builder()
                    .firstName("NewFirstName")
                    .lastName("NewLastName")
                    .phoneNumber("0888123456")
                    .build();

            Client existingClient = createMockSavedClient();
            existingClient.setId(clientId);

            given(clientRepository.findById(clientId)).willReturn(Optional.of(existingClient));
            given(clientRepository.save(any(Client.class))).willReturn(existingClient);

            // Act
            clientService.updateClientProfile(clientId, dto);

            // Assert
            verify(clientRepository, never()).findByPhoneNumber(anyString());
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("Error Case: Should throw PHONE_DUPLICATE if new phone number is taken by someone else")
        void shouldThrowIfNewPhoneNumberIsDuplicate() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            ClientUpdateDto dto = ClientUpdateDto.builder()
                    .firstName("NewFirstName")
                    .lastName("NewLastName")
                    .phoneNumber("0888111222")
                    .build();

            Client existingClient = createMockSavedClient();
            existingClient.setId(clientId);
            existingClient.setPhoneNumber("0888000000");

            Client otherClient = new Client();
            otherClient.setId(UUID.randomUUID());

            given(clientRepository.findById(clientId)).willReturn(Optional.of(existingClient));
            given(clientRepository.findByPhoneNumber(dto.phoneNumber())).willReturn(Optional.of(otherClient));

            // Act and Assert
            assertThatThrownBy(() -> clientService.updateClientProfile(clientId, dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PHONE_DUPLICATE);

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw RESOURCE_NOT_FOUND if client does not exist")
        void shouldThrowIfClientNotFound() {
            UUID clientId = UUID.randomUUID();
            ClientUpdateDto dto = ClientUpdateDto.builder().build();

            given(clientRepository.findById(clientId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateClientProfile(clientId, dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getAllClients(Pageable) Tests")
    class GetAllClientsTests {

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list of all clients")
        void shouldGetAllClientsSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Client mockClient = createMockSavedClient();
            Page<Client> pagedResponse = new PageImpl<>(List.of(mockClient), pageable, 1);

            given(clientRepository.findAll(pageable)).willReturn(pagedResponse);

            // Act
            Page<ClientViewDto> result = clientService.getAllClients(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(clientRepository).findAll(pageable);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Edge Case: Should return empty page if no clients exist")
        void shouldReturnEmptyPageWhenNoClients() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Client> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(clientRepository.findAll(pageable)).willReturn(emptyPage);

            // Act
            Page<ClientViewDto> result = clientService.getAllClients(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();

            verify(clientRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Error Case: Defense in depth - should throw NullPointerException if Pageable is null")
        void shouldFailFastIfPageableNull() {
            assertThatThrownBy(() -> clientService.getAllClients(null))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(clientRepository);
            verifyNoInteractions(emailService);

        }
    }

    @Nested
    @DisplayName("searchClients(String, Pageable) Tests")
    class SearchClientsTests {

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list of matching clients")
        void shouldSearchClientsSuccessfully() {
            // Arrange
            String term = "Doe";
            Pageable pageable = PageRequest.of(0, 10);
            Client mockClient = createMockSavedClient();
            Page<Client> pagedResponse = new PageImpl<>(List.of(mockClient), pageable, 1);

            given(clientRepository.searchClients(term, pageable)).willReturn(pagedResponse);

            // Act
            Page<ClientViewDto> result = clientService.searchClients(term, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().lastName()).isEqualTo("Doe");

            verify(clientRepository).searchClients(term, pageable);
        }

        @Test
        @DisplayName("Edge Case: Should trim term and search")
        void shouldTrimTermAndSearch() {
            // Arrange
            String dirtyTerm = "  0888  ";
            String cleanTerm = "0888";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Client> pagedResponse = new PageImpl<>(List.of(createMockSavedClient()), pageable, 1);

            given(clientRepository.searchClients(cleanTerm, pageable)).willReturn(pagedResponse);

            // Act
            clientService.searchClients(dirtyTerm, pageable);

            // Assert
            verify(clientRepository).searchClients(cleanTerm, pageable);
        }

        @Test
        @DisplayName("Edge Case: Should return empty page if term is null or blank")
        void shouldReturnEmptyPageIfTermBlank() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<ClientViewDto> result = clientService.searchClients("   ", pageable);

            // Assert
            assertThat(result.isEmpty()).isTrue();
            verifyNoInteractions(clientRepository);
        }
    }
}
