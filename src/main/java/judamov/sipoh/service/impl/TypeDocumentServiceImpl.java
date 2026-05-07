package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.SiglaDTO;
import judamov.sipoh.dto.TypeDocumentDTO;
import judamov.sipoh.entity.Sigla;
import judamov.sipoh.entity.TypeDocument;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.mappers.SiglaMapper;
import judamov.sipoh.repository.ISiglaRepository;
import judamov.sipoh.repository.ITypeDocumentRepository;
import judamov.sipoh.service.interfaces.ITypeDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypeDocumentServiceImpl  implements ITypeDocumentService {
    private final ITypeDocumentRepository typeDocumentRepository;
    private final ISiglaRepository siglaRepository;

    @Override
    public List<TypeDocumentDTO> getAllTypeDocuments(){
        List<TypeDocument> typeDocumentList= typeDocumentRepository.findAll();
        return typeDocumentRepository.findAll()
                .stream()
                .map(typeDocument -> new TypeDocumentDTO(typeDocument.getId(),typeDocument.getDescription(), typeDocument.getSigla().getSigla(),typeDocument.getSigla().getId()))
                .toList();
    }
    public List<SiglaDTO> getAllSiglas(){
        return siglaRepository.findAll()
                .stream()
                .map(SiglaMapper::siglaToSiglaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public Boolean createTypeDocument(TypeDocumentDTO typeDocumentDTO) {
        TypeDocument newTypeDocument= new TypeDocument();
        newTypeDocument.setDescription(typeDocumentDTO.getDescription().toUpperCase());
        if (typeDocumentDTO.getIdSigla() != null) {
            Sigla sigla = siglaRepository.findOneById(typeDocumentDTO.getIdSigla())
                    .orElseThrow(() -> {
                        log.warn("Sigla no encontrada con id={}", typeDocumentDTO.getIdSigla());
                        return new GenericAppException(HttpStatus.NOT_FOUND, "Sigla no encontrada");
                    });
            newTypeDocument.setSigla(sigla);
        }else if (typeDocumentDTO.getSigla() != null) {

            siglaRepository.findOneBySigla(typeDocumentDTO.getSigla().toUpperCase()).ifPresent(existing -> {
                throw new GenericAppException(HttpStatus.BAD_REQUEST,
                        "Ya existe una sigla registrada como: " + typeDocumentDTO.getSigla().toUpperCase());
            });
            try {
                String siglaTexto = typeDocumentDTO.getSigla().toUpperCase();
                Sigla sigla = siglaRepository.findOneBySigla(siglaTexto)
                        .orElseGet(() -> {
                            Sigla nuevaSigla = Sigla.builder().sigla(siglaTexto).build();
                            return siglaRepository.save(nuevaSigla);
                        });

                newTypeDocument.setSigla(sigla);
            } catch (Exception e) {
                log.error("Error al guardar tipo de documento con sigla='{}'", typeDocumentDTO.getSigla(), e);
                throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al guardar el tipo de documento");
            }
        }
        else {
            throw new GenericAppException(HttpStatus.BAD_REQUEST,
                    "Es necesaria la sigla nueva o el id de una sigla ya existente");
        }

        try {
            typeDocumentRepository.save(newTypeDocument);
        } catch (Exception e) {
            log.error("Error al guardar tipo de documento descripción='{}'", typeDocumentDTO.getDescription(), e);
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el tipo de documento");
        }
        return true;
    }
    @Transactional
    @Override
    public Boolean updateTypeDocument(Long id, TypeDocumentDTO typeDocumentDTO){
        TypeDocument typeDocument=typeDocumentRepository.findOneById(id)
                .orElseThrow(()-> {
                    log.warn("Tipo de documento no encontrado con id={}", id);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Tipo de documento no encontrado");
                });
        if(typeDocumentDTO.getIdSigla()== null || typeDocumentDTO.getDescription().isEmpty()){
            throw new GenericAppException(HttpStatus.BAD_REQUEST,
                    "Es necesario enviar la descripcion y el Id Sigla");
        }
        typeDocument.setDescription(typeDocumentDTO.getDescription());
        Sigla sigla=siglaRepository.findOneById(typeDocumentDTO.getIdSigla())
                        .orElseThrow(()-> {
                            log.warn("Sigla no encontrada con id={}", typeDocumentDTO.getIdSigla());
                            return new GenericAppException(HttpStatus.BAD_REQUEST, "Sigla no encontrada");
                        });

        typeDocument.setSigla(sigla);
        try{
            typeDocumentRepository.save(typeDocument);
        }catch (Exception e){
            log.error("Error al actualizar tipo de documento id={}", id, e);
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar el tipo de documento");
        }
        return true;

    }
}
