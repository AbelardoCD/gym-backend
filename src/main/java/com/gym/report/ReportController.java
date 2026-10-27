package com.gym.report;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAuthority('REPORTES')")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reports;
    private final ExcelExporter excel;

    /** Resumen del rango (por defecto, el mes en curso). */
    @GetMapping("/summary")
    public ReportService.Summary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return reports.summary(from == null ? today.withDayOfMonth(1) : from, to == null ? today : to);
    }

    /** Descarga un Excel: pagos, asistencia o socios. */
    @GetMapping("/export/{kind}")
    public ResponseEntity<byte[]> export(@PathVariable String kind,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ExcelExporter.Kind k = ExcelExporter.Kind.valueOf(kind.toUpperCase(Locale.ROOT));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate start = from == null ? today.withDayOfMonth(1) : from;
        LocalDate end = to == null ? today : to;
        String name = k == ExcelExporter.Kind.SOCIOS ? "socios-" + today + ".xlsx" : k.name().toLowerCase() + "-" + start + "-a-" + end + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel.export(k, start, end));
    }
}
