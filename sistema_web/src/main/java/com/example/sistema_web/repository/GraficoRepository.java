package com.example.sistema_web.repository;

import com.example.sistema_web.model.Documento;
import com.example.sistema_web.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraficoRepository extends JpaRepository<Documento, Long> {

    // 📊 1. Análisis por sustancia
    @Query(value = """
        SELECT 
            d.tipo_muestra AS sustancia,
            COUNT(*) AS total_analisis
        FROM documentos d
        WHERE d.tipo_muestra IS NOT NULL AND d.tipo_muestra != ''
        GROUP BY d.tipo_muestra
        ORDER BY total_analisis DESC
        """, nativeQuery = true)
    List<GraficoSustanciaDTO> findAnalisisPorSustancia();

    // 📊 2. Estados: Positivo vs Negativo
    @Query(value = """
        SELECT 
            CASE 
                WHEN ad.cualitativo LIKE '%Positivo%' THEN 'Positivo'
                WHEN at.resultado_toxicologico LIKE '%Positivo%' THEN 'Positivo'
                ELSE 'Negativo'
            END AS estado,
            COUNT(*) AS cantidad
        FROM documentos d
        LEFT JOIN asignaciones_dosaje ad ON d.id = ad.documento_id
        LEFT JOIN asignaciones_toxicologia at ON d.id = at.documento_id
        WHERE ad.cualitativo IS NOT NULL OR at.resultado_toxicologico IS NOT NULL
        GROUP BY 
            CASE 
                WHEN ad.cualitativo LIKE '%Positivo%' THEN 'Positivo'
                WHEN at.resultado_toxicologico LIKE '%Positivo%' THEN 'Positivo'
                ELSE 'Negativo'
            END
        """, nativeQuery = true)
    List<GraficoEstadoDTO> findEstados();

    // 📊 3. Tiempo promedio por tipo de análisis (USANDO NOW() en lugar de created_at)
    @Query(value = """
        SELECT 
            'Dosaje' AS tipo_analisis,
            AVG(TIMESTAMPDIFF(HOUR, d.fecha_ingreso, NOW())) AS tiempo_promedio_horas
        FROM documentos d
        JOIN asignaciones_dosaje ad ON d.id = ad.documento_id
        WHERE d.fecha_ingreso IS NOT NULL
        
        UNION ALL
        
        SELECT 
            'Toxicología' AS tipo_analisis,
            AVG(TIMESTAMPDIFF(HOUR, d.fecha_ingreso, NOW())) AS tiempo_promedio_horas
        FROM documentos d
        JOIN asignaciones_toxicologia at ON d.id = at.documento_id
        WHERE d.fecha_ingreso IS NOT NULL
        """, nativeQuery = true)
    List<GraficoTiempoDTO> findTiempos();

    // 📊 4. Productividad por empleado
    @Query(value = """
        SELECT 
            e.nombre AS empleado,
            e.apellido,
            COUNT(*) AS total_analisis
        FROM (
            SELECT empleado_id FROM asignaciones_dosaje
            UNION ALL
            SELECT empleado_id FROM asignaciones_toxicologia
        ) a
        JOIN empleados e ON a.empleado_id = e.id
        GROUP BY e.id, e.nombre, e.apellido
        ORDER BY total_analisis DESC
        """, nativeQuery = true)
    List<GraficoEmpleadoDTO> findProductividadPorEmpleado();
    // 📊 5. Total de empleados registrados
    @Query(value = "SELECT COUNT(*) FROM empleados", nativeQuery = true)
    Long findTotalEmpleados();

    // 📊 6. Documentos por día (últimos 7 días)
    @Query(value = """
    SELECT DATE(fecha_ingreso) AS fecha, COUNT(*) AS total
    FROM documentos
    WHERE fecha_ingreso >= CURDATE() - INTERVAL 7 DAY
    GROUP BY DATE(fecha_ingreso)
    ORDER BY fecha ASC
    """, nativeQuery = true)
    List<Object[]> findDocumentosPorDia();
}