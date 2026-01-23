package com.example.sistema_web.dto;

import lombok.Data;

// Este DTO representa el objeto JSON que viene del frontend
@Data
public class AnexosDTO {
    private boolean cadenaCustodia;
    private boolean rotulo;
    private boolean actaTomaMuestra;
    private boolean actaConsentimiento;
    private boolean actaDenunciaVerbal;
    private boolean actaIntervencionPolicial;
    private boolean copiaDniSidpol;
    private boolean actaObtencionMuestra;
}