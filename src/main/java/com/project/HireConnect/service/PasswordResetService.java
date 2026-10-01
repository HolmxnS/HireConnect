package com.project.HireConnect.service;

import com.project.HireConnect.model.Empresa;
import com.project.HireConnect.model.PasswordResetToken;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.repository.EmpresaRepository;
import com.project.HireConnect.repository.PasswordResetTokenRepository;
import com.project.HireConnect.repository.PostulanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    
    @Autowired
    private PostulanteRepository postulanteRepository;
    
    @Autowired
    private EmpresaRepository empresaRepository;
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;
    
    @Transactional
    public void createPasswordResetToken(String email, String appUrl) throws Exception {
        
        
        String userType = null;
        
        Optional<Postulante> postulante = postulanteRepository.findByEmail(email);
        Optional<Empresa> empresa = empresaRepository.findByEmail(email);
        
        if (postulante.isPresent()) {
            userType = "POSTULANTE";
        } else if (empresa.isPresent()) {
            userType = "EMPRESA";
        } else {
            throw new Exception("No existe un usuario registrado con ese correo.");
        }
        
        tokenRepository.deleteByEmail(email);
        
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, email, userType);
        tokenRepository.save(resetToken);
        sendEmail(email, token, appUrl);
    }
    
    private void sendEmail(String to, String token, String appUrl) {
        String resetUrl = appUrl + "/reset-password?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject("Recuperación de Contraseña - HireConnect");
        message.setText("Hola,\n\n"
                + "Hemos recibido una solicitud para restablecer tu contraseña en HireConnect.\n"
                + "Para continuar, haz clic en el siguiente enlace:\n\n"
                + resetUrl + "\n\n"
                + "Este enlace expirará en 24 horas.\n"
                + "Si no solicitaste esto, puedes ignorar este mensaje.\n\n"
                + "Saludos,\nEl equipo de HireConnect.");
        
        mailSender.send(message);
        System.out.println("📧 Correo enviado exitosamente a: " + to);
    }
    public PasswordResetToken validateToken(String token) throws Exception {
        Optional<PasswordResetToken> resetToken = tokenRepository.findByToken(token);
        
        if (resetToken.isEmpty()) {
            throw new Exception("El enlace de recuperación no es válido.");
        }
        
        PasswordResetToken passwordResetToken = resetToken.get();
        
        if (passwordResetToken.isExpired()) {
            throw new Exception("El enlace ha expirado. Por favor solicita uno nuevo.");
        }
        
        if (passwordResetToken.isUsed()) {
            throw new Exception("Este enlace ya fue utilizado anteriormente.");
        }
        
        return passwordResetToken;
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) throws Exception {
        PasswordResetToken resetToken = validateToken(token);
        
        String email = resetToken.getEmail();
        String userType = resetToken.getUserType();
        
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        if ("POSTULANTE".equals(userType)) {
            Postulante p = postulanteRepository.findByEmail(email).orElseThrow();
            p.setPassword(encodedPassword);
            postulanteRepository.save(p);
        } else if ("EMPRESA".equals(userType)) {
            Empresa e = empresaRepository.findByEmail(email).orElseThrow();
            e.setPassword(encodedPassword);
            empresaRepository.save(e);
        }
        
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}