// src/main/java/com/example/sistema_web/service/ReporteServiceImpl.java
package com.example.sistema_web.service;

import com.example.sistema_web.dto.ReporteDTO;
import com.example.sistema_web.model.AsignacionDosaje;
import com.example.sistema_web.model.Notification;
import com.example.sistema_web.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final EmpleadoRepository empleadoRepository;
    private final DocumentoRepository documentoRepository;
    private final AsignacionDosajeRepository dosajeRepository;
    private final AsignacionToxicologiaRepository toxicologiaRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public Long getTotalEmpleados() {
        return empleadoRepository.count();
    }

    @Override
    public Long getTotalDocumentos() {
        return documentoRepository.count();
    }

    @Override
    public List<ReporteDTO.SustanciaDTO> getAnalisisPorSustancia() {
        // ✅ Contar solo resultados POSITIVOS
        long marihuanaPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"marihuana\":\"Positivo\"");
        long cocainaPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"cocaina\":\"Positivo\"");
        long benzodiacepinasPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"benzodiacepinas\":\"Positivo\"");
        long barbituricosPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"barbituricos\":\"Positivo\"");
        long carbamatosPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"carbamatos\":\"Positivo\"");
        long estricninaPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"estricnina\":\"Positivo\"");
        long cumarinasPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"cumarinas\":\"Positivo\"");
        long organofosforadosPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"organofosforados\":\"Positivo\"");
        long misoprostolPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"misoprostol\":\"Positivo\"");
        long piretrinasPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"piretrinas\":\"Positivo\"");

        // ✅ Contar solo resultados NEGATIVOS
        long marihuanaNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"marihuana\":\"Negativo\"");
        long cocainaNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"cocaina\":\"Negativo\"");
        long benzodiacepinasNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"benzodiacepinas\":\"Negativo\"");
        long barbituricosNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"barbituricos\":\"Negativo\"");
        long carbamatosNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"carbamatos\":\"Negativo\"");
        long estricninaNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"estricnina\":\"Negativo\"");
        long cumarinasNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"cumarinas\":\"Negativo\"");
        long organofosforadosNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"organofosforados\":\"Negativo\"");
        long misoprostolNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"misoprostol\":\"Negativo\"");
        long piretrinasNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"piretrinas\":\"Negativo\"");

        // ✅ Total por sustancia (positivos + negativos)
        long marihuanaTotal = marihuanaPos + marihuanaNeg;
        long cocainaTotal = cocainaPos + cocainaNeg;
        long benzodiacepinasTotal = benzodiacepinasPos + benzodiacepinasNeg;
        long barbituricosTotal = barbituricosPos + barbituricosNeg;
        long carbamatosTotal = carbamatosPos + carbamatosNeg;
        long estricninaTotal = estricninaPos + estricninaNeg;
        long cumarinasTotal = cumarinasPos + cumarinasNeg;
        long organofosforadosTotal = organofosforadosPos + organofosforadosNeg;
        long misoprostolTotal = misoprostolPos + misoprostolNeg;
        long piretrinasTotal = piretrinasPos + piretrinasNeg;

        return Arrays.asList(
                new ReporteDTO.SustanciaDTO("Marihuana", marihuanaTotal),
                new ReporteDTO.SustanciaDTO("Cocaína", cocainaTotal),
                new ReporteDTO.SustanciaDTO("Benzodiacepinas", benzodiacepinasTotal),
                new ReporteDTO.SustanciaDTO("Barbitúricos", barbituricosTotal),
                new ReporteDTO.SustanciaDTO("Carbamatos", carbamatosTotal),
                new ReporteDTO.SustanciaDTO("Estricnina", estricninaTotal),
                new ReporteDTO.SustanciaDTO("Cumarinas", cumarinasTotal),
                new ReporteDTO.SustanciaDTO("Organofosforados", organofosforadosTotal),
                new ReporteDTO.SustanciaDTO("Misoprostol", misoprostolTotal),
                new ReporteDTO.SustanciaDTO("Piretrinas", piretrinasTotal)
        );
    }

    @Override
    public List<ReporteDTO.EstadoDTO> getEstados() {
        // ✅ Contar resultados reales en JSON
        long toxPos = toxicologiaRepository.countByResultadoToxicologicoContaining("\"Positivo\"");
        long dosajePos = dosajeRepository.countByCualitativoContaining("Positivo");

        long toxNeg = toxicologiaRepository.countByResultadoToxicologicoContaining("\"Negativo\"");
        long dosajeNeg = dosajeRepository.countByCualitativoContaining("Negativo");

        return Arrays.asList(
                new ReporteDTO.EstadoDTO("Positivo", toxPos + dosajePos),
                new ReporteDTO.EstadoDTO("Negativo", toxNeg + dosajeNeg)
        );
    }

    @Override
    public List<ReporteDTO.DocumentoDiaDTO> getDocumentosPorDia() {
        List<ReporteDTO.DocumentoDiaDTO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 6; i >= 0; i--) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            long total = 0;

            // ✅ Contar notificaciones por fecha (real actividad)
            total = notificationRepository.countByTimestampBetween(
                    fecha.atStartOfDay(),
                    fecha.plusDays(1).atStartOfDay()
            );

            result.add(new ReporteDTO.DocumentoDiaDTO(fecha.format(formatter), total));
        }

        return result;
    }

    @Override
    public List<ReporteDTO.EmpleadoDTO> getEmpleadosProductividad() {
        // ✅ Contar notificaciones por emisor (quien completó la tarea)
        List<Object[]> resultados = notificationRepository.countByEmisorGrouped();

        return resultados.stream()
                .map(row -> new ReporteDTO.EmpleadoDTO(
                        (String) row[1],  // nombre
                        (String) row[2],  // apellido
                        ((Number) row[3]).longValue() // total
                ))
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] generarExcel(int mes, int año) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Métrica");
            header.createCell(1).setCellValue("Valor");

            sheet.createRow(1).createCell(0).setCellValue("Total Empleados");
            sheet.getRow(1).createCell(1).setCellValue(getTotalEmpleados());

            sheet.createRow(2).createCell(0).setCellValue("Total Documentos");
            sheet.getRow(2).createCell(1).setCellValue(getTotalDocumentos());

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }

    @Override
    public byte[] generarPdf(int mes, int año) throws IOException {
        String content = "Reporte Mensual\nMes: " + mes + "\nAño: " + año +
                "\nTotal Empleados: " + getTotalEmpleados() +
                "\nTotal Documentos: " + getTotalDocumentos();
        return content.getBytes();
    }
    @Override
    public List<ReporteDTO.RangoCualitativoDTO> getRangosCualitativos() {
        List<AsignacionDosaje> asignaciones = dosajeRepository.findAll();

        long bajo = 0, medio = 0, alto = 0;

        for (AsignacionDosaje a : asignaciones) {
            if (a.getCualitativo() != null) {
                try {
                    double valor = Double.parseDouble(a.getCualitativo());
                    if (valor < 1) {
                        bajo++;
                    } else if (valor <= 2) {
                        medio++;
                    } else {
                        alto++;
                    }
                } catch (NumberFormatException e) {
                    // Ignorar valores no numéricos
                }
            }
        }

        return Arrays.asList(
                new ReporteDTO.RangoCualitativoDTO("Bajo (< 1)", bajo),
                new ReporteDTO.RangoCualitativoDTO("Medio (1-2)", medio),
                new ReporteDTO.RangoCualitativoDTO("Alto (> 2)", alto)
        );
    }
}