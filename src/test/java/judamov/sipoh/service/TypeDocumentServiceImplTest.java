package judamov.sipoh.service;

import judamov.sipoh.dto.SiglaDTO;
import judamov.sipoh.dto.TypeDocumentDTO;
import judamov.sipoh.entity.Sigla;
import judamov.sipoh.entity.TypeDocument;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.ISiglaRepository;
import judamov.sipoh.repository.ITypeDocumentRepository;
import judamov.sipoh.service.impl.TypeDocumentServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TypeDocumentServiceImpl - Tests unitarios")
class TypeDocumentServiceImplTest {

    @Mock private ITypeDocumentRepository typeDocumentRepository;
    @Mock private ISiglaRepository siglaRepository;

    @InjectMocks
    private TypeDocumentServiceImpl typeDocumentService;

    private TypeDocument typeDocument;
    private Sigla sigla;

    @BeforeEach
    void setUp() {
        sigla = TestDataFactory.buildSigla();
        typeDocument = TestDataFactory.buildTypeDocument();
        typeDocument.setSigla(sigla);
    }

    @Test
    @DisplayName("getAllTypeDocuments - retorna la lista de tipos de documento")
    void shouldReturnAllTypeDocuments() {
        when(typeDocumentRepository.findAll()).thenReturn(List.of(typeDocument));

        List<TypeDocumentDTO> result = typeDocumentService.getAllTypeDocuments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("CEDULA DE CIUDADANIA");
        assertThat(result.get(0).getSigla()).isEqualTo("CC");
    }

    @Test
    @DisplayName("getAllSiglas - retorna lista de siglas")
    void shouldReturnAllSiglas() {
        when(siglaRepository.findAll()).thenReturn(List.of(sigla));

        List<SiglaDTO> result = typeDocumentService.getAllSiglas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSigla()).isEqualTo("CC");
    }

    @Test
    @DisplayName("createTypeDocument - crea tipo de documento usando idSigla existente")
    void shouldCreateTypeDocumentWithExistingSigla() {
        TypeDocumentDTO dto = new TypeDocumentDTO();
        dto.setDescription("Tarjeta de identidad");
        dto.setIdSigla(sigla.getId());

        when(siglaRepository.findOneById(sigla.getId())).thenReturn(Optional.of(sigla));

        Boolean result = typeDocumentService.createTypeDocument(dto);

        assertThat(result).isTrue();
        verify(typeDocumentRepository).save(any(TypeDocument.class));
    }

    @Test
    @DisplayName("createTypeDocument - crea tipo de documento con sigla nueva")
    void shouldCreateTypeDocumentWithNewSigla() {
        TypeDocumentDTO dto = new TypeDocumentDTO();
        dto.setDescription("Pasaporte");
        dto.setSigla("PA");

        when(siglaRepository.findOneBySigla("PA")).thenReturn(Optional.empty());
        when(siglaRepository.save(any(Sigla.class))).thenReturn(sigla);

        Boolean result = typeDocumentService.createTypeDocument(dto);

        assertThat(result).isTrue();
        verify(typeDocumentRepository).save(any(TypeDocument.class));
    }

    @Test
    @DisplayName("createTypeDocument - lanza BAD_REQUEST cuando la sigla nueva ya está registrada")
    void shouldThrowWhenNewSiglaAlreadyExists() {
        TypeDocumentDTO dto = new TypeDocumentDTO();
        dto.setDescription("Pasaporte");
        dto.setSigla("CC");

        when(siglaRepository.findOneBySigla("CC")).thenReturn(Optional.of(sigla));

        assertThatThrownBy(() -> typeDocumentService.createTypeDocument(dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Ya existe una sigla registrada como");
    }

    @Test
    @DisplayName("createTypeDocument - lanza BAD_REQUEST cuando no se provee ni idSigla ni sigla")
    void shouldThrowWhenNeitherSiglaNorIdSiglaProvided() {
        TypeDocumentDTO dto = new TypeDocumentDTO();
        dto.setDescription("Algo");

        assertThatThrownBy(() -> typeDocumentService.createTypeDocument(dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("sigla nueva o el id de una sigla ya existente");
    }

    @Test
    @DisplayName("updateTypeDocument - actualiza tipo de documento correctamente")
    void shouldUpdateTypeDocumentSuccessfully() {
        TypeDocumentDTO dto = new TypeDocumentDTO();
        dto.setDescription("Cédula Actualizada");
        dto.setIdSigla(sigla.getId());

        when(typeDocumentRepository.findOneById(typeDocument.getId())).thenReturn(Optional.of(typeDocument));
        when(siglaRepository.findOneById(sigla.getId())).thenReturn(Optional.of(sigla));

        Boolean result = typeDocumentService.updateTypeDocument(typeDocument.getId(), dto);

        assertThat(result).isTrue();
        verify(typeDocumentRepository).save(typeDocument);
    }

    @Test
    @DisplayName("updateTypeDocument - lanza NOT_FOUND cuando el tipo de documento no existe")
    void shouldThrowWhenTypeDocumentNotFound() {
        TypeDocumentDTO dto = new TypeDocumentDTO();
        dto.setDescription("Algo");
        dto.setIdSigla(1L);

        when(typeDocumentRepository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> typeDocumentService.updateTypeDocument(99L, dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Tipo de documento no encontrado");
    }

    @Test
    @DisplayName("updateTypeDocument - lanza BAD_REQUEST cuando la descripción es vacía o idSigla es null")
    void shouldThrowWhenDescriptionEmptyOrIdSiglaNull() {
        TypeDocumentDTO dto = new TypeDocumentDTO();
        dto.setDescription("");
        dto.setIdSigla(null);

        when(typeDocumentRepository.findOneById(typeDocument.getId())).thenReturn(Optional.of(typeDocument));

        assertThatThrownBy(() -> typeDocumentService.updateTypeDocument(typeDocument.getId(), dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("descripcion y el Id Sigla");
    }
}
