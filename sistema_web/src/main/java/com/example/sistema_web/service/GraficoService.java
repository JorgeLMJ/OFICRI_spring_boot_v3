package com.example.sistema_web.service;

import com.example.sistema_web.dto.*;
import com.example.sistema_web.repository.GraficoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GraficoService {

    private final GraficoRepository graficoRepo;

    public List<GraficoSustanciaDTO> obtenerAnalisisPorSustancia() {
        return graficoRepo.findAnalisisPorSustancia();
    }

    public List<GraficoEstadoDTO> obtenerEstados() {
        return graficoRepo.findEstados();
    }

    public List<GraficoTiempoDTO> obtenerTiempos() {
        return graficoRepo.findTiempos();
    }

    public List<GraficoEmpleadoDTO> obtenerProductividadPorEmpleado() {
        return graficoRepo.findProductividadPorEmpleado();
    }
    public Long obtenerTotalEmpleados() {
        return graficoRepo.findTotalEmpleados();
    }

    public List<Object[]> obtenerDocumentosPorDia() {
        return graficoRepo.findDocumentosPorDia();
    }
}