package judamov.sipoh.service;

import jakarta.mail.internet.MimeMessage;
import judamov.sipoh.dto.EmailRequestDTO;
import judamov.sipoh.entity.EmailTemplate;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IEmailRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.impl.EmailServiceImpl;
import judamov.sipoh.service.impl.UserRolServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl - Tests unitarios")
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private IEmailRepository emailRepository;
    @Mock private IUserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmailServiceImpl emailService;

    private User adminUser;
    private User teacherUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fakeDestination", "fake@test.com");
        ReflectionTestUtils.setField(emailService, "url_access", "http://localhost:4200");
        ReflectionTestUtils.setField(emailService, "from", "noreply@test.com");

        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
    }

    private EmailTemplate buildTemplate(String code) {
        EmailTemplate template = new EmailTemplate();
        template.setCode(code);
        template.setSubject("Bienvenido {{nombre_destinatario}}");
        template.setBody("Tu documento es {{documento_destinatario}}, clave: {{password_destinatario}}, link: {{link_acceso}}");
        return template;
    }

    @Test
    @DisplayName("sendEmail(DTO) - envía correo exitosamente con plantilla existente")
    void shouldSendEmailSuccessfully() {
        EmailRequestDTO dto = EmailRequestDTO.builder()
                .nombre("Juan Test")
                .documento("11111111")
                .password("pass123")
                .email("juan@test.com")
                .fake(true)
                .build();

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(emailRepository.findByCode("credenciales_acceso"))
                .thenReturn(Optional.of(buildTemplate("credenciales_acceso")));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Boolean result = emailService.sendEmail(dto);

        assertThat(result).isTrue();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail(DTO) - lanza NOT_FOUND cuando la plantilla no existe")
    void shouldThrowWhenTemplateNotFound() {
        EmailRequestDTO dto = EmailRequestDTO.builder()
                .nombre("Juan")
                .documento("11111111")
                .password("pass")
                .email("juan@test.com")
                .fake(false)
                .build();

        when(emailRepository.findByCode("credenciales_acceso")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailService.sendEmail(dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Plantilla 'credenciales_acceso' no encontrada");
    }

    @Test
    @DisplayName("sendEmail(userId, DTO) - lanza FORBIDDEN cuando el userId no es admin")
    void shouldThrowForbiddenWhenUserIsNotAdmin() {
        EmailRequestDTO dto = EmailRequestDTO.builder()
                .nombre("Otro")
                .documento("22222222")
                .password("pass")
                .email("otro@test.com")
                .fake(false)
                .build();

        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> emailService.sendEmail(teacherUser.getId(), dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("sendRecoveryPassword - lanza NOT_FOUND cuando el usuario no existe")
    void shouldThrowWhenUserNotFoundForRecovery() {
        when(userRepository.findOneByDocumento("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailService.sendRecoveryPassword("99999999", true))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("sendRecoveryPassword - envía correo de recuperación en modo fake sin guardar en DB")
    void shouldSendRecoveryEmailInFakeModeWithoutSavingToDb() {
        MimeMessage mimeMessage = mock(MimeMessage.class);

        when(userRepository.findOneByDocumento(teacherUser.getDocumento()))
                .thenReturn(Optional.of(teacherUser));
        when(emailRepository.findByCode("password_reset"))
                .thenReturn(Optional.of(buildTemplate("password_reset")));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Boolean result = emailService.sendRecoveryPassword(teacherUser.getDocumento(), true);

        assertThat(result).isTrue();
        verify(userRepository, never()).save(any());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendRecoveryPassword - guarda nueva contraseña cuando isFake=false")
    void shouldSaveNewPasswordWhenNotFake() {
        MimeMessage mimeMessage = mock(MimeMessage.class);

        when(userRepository.findOneByDocumento(teacherUser.getDocumento()))
                .thenReturn(Optional.of(teacherUser));
        when(emailRepository.findByCode("password_reset"))
                .thenReturn(Optional.of(buildTemplate("password_reset")));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedNewPass");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRecoveryPassword(teacherUser.getDocumento(), false);

        verify(userRepository).save(teacherUser);
        verify(passwordEncoder).encode(anyString());
    }
}
