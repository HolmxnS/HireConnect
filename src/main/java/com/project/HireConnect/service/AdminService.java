package com.project.HireConnect.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.HireConnect.model.Admin;
import com.project.HireConnect.repository.AdminRepository;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;
    
    //buscar por email
    public Optional<Admin> buscarPorEmail(String email) {
        return adminRepository.findByEmail(email); 
    }

    //buscar por id
    public Optional<Admin> buscarPorId(String id) {
        return adminRepository.findById(id); 
    }

    //guardar usuario
    public Admin guardar(Admin admin) {
        return adminRepository.save(admin);
    }
}