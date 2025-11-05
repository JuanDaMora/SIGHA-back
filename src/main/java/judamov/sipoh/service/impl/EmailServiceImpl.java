package judamov.sipoh.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import judamov.sipoh.dto.EmailRequestDTO;
import judamov.sipoh.entity.EmailTemplate;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.notifications.TemplateProcessor;
import judamov.sipoh.repository.IEmailRepository;
import judamov.sipoh.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl {

    @Value("${email.fake.destination}")
    private String fakeDestination;

    @Value("${sigha.url.access}")
    private String url_access;

    @Value("${spring.mail.username}")
    private String from;

    private final JavaMailSender mailSender;
    private final UserRolServiceImpl userRolService;
    private final IEmailRepository emailRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Boolean sendEmail(Long userId, EmailRequestDTO emailRequestDTO) {
        User user = getUserById(userId);

        if (!userRolService.hasAdminPrivileges(user)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "No autorizado para enviar correos");
        }

        return sendEmail(emailRequestDTO);
    }

    public Boolean sendEmail(EmailRequestDTO emailRequestDTO) {
        EmailTemplate template = emailRepository.findByCode("credenciales_acceso")
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "Plantilla 'credenciales_acceso' no encontrada."));

        Map<String, String> variables = Map.of(
                "nombre_destinatario", emailRequestDTO.getNombre(),
                "documento_destinatario", emailRequestDTO.getDocumento(),
                "password_destinatario", emailRequestDTO.getPassword(),
                "link_acceso", url_access
        );

        String subject = TemplateProcessor.render(template.getSubject(), variables);
        String body = TemplateProcessor.render(template.getBody(), variables);

        return sendMail(emailRequestDTO.getEmail(), emailRequestDTO.getFake(), subject, body);
    }

    public Boolean sendRecoveryPassword(String documento, boolean isFake) {
        User user = userRepository.findOneByDocumento(documento)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 1. Generar nueva contraseña
        String newPassword = generateRandomPassword();

        if(!isFake){
            // 2. Guardar en DB (hasheada)
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }

        // 3. Obtener plantilla
        EmailTemplate template = emailRepository.findByCode("password_reset")
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "Plantilla 'password_reset' no encontrada."));

        Map<String, String> variables = Map.of(
                "nombre_destinatario", user.getFirstName() + " " + user.getLastName(),
                "nueva_password", newPassword,
                "link_acceso", url_access
        );

        String subject = TemplateProcessor.render(template.getSubject(), variables);
        String body = TemplateProcessor.render(template.getBody(), variables);

        // 4. Enviar correo al email real del usuario
        return sendMail(user.getEmail(), isFake, subject, body);
    }

    private Boolean sendMail(String emailDestino, boolean fake, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String destinoFinal = fake ? fakeDestination : emailDestino;
            String contenido = fake
                    ? "<p><b>[FAKE EMAIL]</b><br>Simulando envío a: "
                    + emailDestino + "</p><hr>" + body
                    : body;

            helper.setTo(destinoFinal);
            helper.setSubject(subject);
            helper.setText(contenido, true);
            helper.setFrom(from);

            mailSender.send(message);
            return true;

        } catch (MessagingException e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al enviar el correo: " + e.getMessage());
        }
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private String generateRandomPassword() {
        int PASSWORD_LENGTH = 10;
        String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
        SecureRandom RANDOM = new SecureRandom();

        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(CHAR_POOL.length());
            password.append(CHAR_POOL.charAt(index));
        }
        return password.toString();
    }
    private void validateAdminAccess(Long userId) {
        User user = getUserById(userId);
        if (!userRolService.hasAdminPrivileges(user)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "No autorizado para esta solicitud");
        }
    }
}
