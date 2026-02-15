package com.app.support.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    /**
     * Generates a PDF report using JasperReports.
     */
    public byte[] generatePdfReport(String templatePath, Map<String, Object> parameters, List<?> data) {
        try {
            InputStream templateStream = getClass().getResourceAsStream(templatePath);
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
            
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            log.error("Error generating Jasper report", e);
            throw new RuntimeException("Report Generation Failed");
        }
    }
}
