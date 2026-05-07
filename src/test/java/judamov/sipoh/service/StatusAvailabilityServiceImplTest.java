package judamov.sipoh.service;

import judamov.sipoh.dto.StatusAvailabilityDTO;
import judamov.sipoh.entity.StatusAvailability;
import judamov.sipoh.repository.IStatusAvailabilityRepository;
import judamov.sipoh.service.impl.StatusAvailabilityServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatusAvailabilityServiceImpl - Tests unitarios")
class StatusAvailabilityServiceImplTest {

    @Mock
    private IStatusAvailabilityRepository statusAvailabilityRepository;

    @InjectMocks
    private StatusAvailabilityServiceImpl statusAvailabilityService;

    @Test
    @DisplayName("getAll - mapea estados a StatusAvailabilityDTO")
    void shouldMapAllStatusesToDto() {
        StatusAvailability s1 = TestDataFactory.buildStatusPending();
        StatusAvailability s2 = TestDataFactory.buildStatusApproved();
        when(statusAvailabilityRepository.findAll()).thenReturn(List.of(s1, s2));

        List<StatusAvailabilityDTO> result = statusAvailabilityService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(s1.getId());
        assertThat(result.get(0).getDescription()).isEqualTo(s1.getDescription());
        assertThat(result.get(1).getDescription()).isEqualTo(s2.getDescription());
    }

    @Test
    @DisplayName("getAll - retorna lista vacía cuando no hay estados")
    void shouldReturnEmptyListWhenNoStatuses() {
        when(statusAvailabilityRepository.findAll()).thenReturn(List.of());

        List<StatusAvailabilityDTO> result = statusAvailabilityService.getAll();

        assertThat(result).isEmpty();
    }
}
