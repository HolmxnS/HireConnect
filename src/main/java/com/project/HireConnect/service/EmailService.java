package com.project.HireConnect.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        Context context = new Context(Locale.forLanguageTag("es"));
        context.setVariable("codigo", codigo);
        String html = templateEngine.process("email/codigo_verificacion", context);
        String texto = "Hola,\n\n"
                + "Gracias por unirte a HireConnect.\n"
                + "Tu código de verificación es: " + codigo + "\n\n"
                + "Ingresa el código en la pantalla de verificación para completar tu registro.\n"
                + "Este código expira en 10 minutos. No lo compartas con nadie.\n"
                + "Si no solicitaste este registro, ignora este mensaje.\n\n"
                + "El equipo de HireConnect";

        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setTo(destinatario);
            helper.setSubject("Tu código de verificación – HireConnect");
            helper.setText(texto, html);
        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo preparar el correo de verificación", e);
        }
        mailSender.send(mensaje);
    }
}
