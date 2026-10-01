package com.project.HireConnect.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.project.HireConnect.service.VacanteService;

public class VacanteScheduler {

    @Autowired
    private VacanteService vacanteService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void revisarVacantes() {
        vacanteService.actualizarEstadosVacantes();
    }

}
