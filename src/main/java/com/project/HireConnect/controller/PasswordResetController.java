package com.project.HireConnect.controller;

import com.project.HireConnect.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "Recuperacion_contraseña";
    }
    
    
    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            HttpServletRequest request,
            Model model) {
        
        try {
            String appUrl = request.getScheme() + "://" + request.getServerName();
            if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                appUrl += ":" + request.getServerPort();
            }
            
            passwordResetService.createPasswordResetToken(email, appUrl);
            
            model.addAttribute("emailSent", true);
            model.addAttribute("email", email);
            return "Recuperacion_contraseña";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "Recuperacion_contraseña";
        }
    }
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        try {
            passwordResetService.validateToken(token);
            model.addAttribute("token", token);
            return "restablecer_contraseña";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("invalidToken", true);
            return "restablecer_contraseña";
        }
    }
    

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (!password.equals(confirmPassword)) {
                throw new Exception("Las contraseñas no coinciden");
            }
            
            if (password.length() < 6) {
                throw new Exception("La contraseña debe tener al menos 6 caracteres");
            }
            passwordResetService.resetPassword(token, password);
            
            redirectAttributes.addFlashAttribute("success", "Contraseña restablecida exitosamente. Ya puedes iniciar sesión.");
            return "redirect:/login";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}