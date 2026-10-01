package com.project.HireConnect.service;
 
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
 
@Service
@RequiredArgsConstructor
public class EmailService {
 
    private final JavaMailSender mailSender;
 
    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Tu código de verificación – HireConnect");
        mensaje.setText(
            "Hola,\n\n" +
            "Tu código de verificación es: " + codigo + "\n\n" +
            "Este código expira en 10 minutos.\n" +
            "Si no solicitaste este código, ignora este mensaje.\n\n" +
            "– El equipo de HireConnect"
        );
        mailSender.send(mensaje);
    }
}