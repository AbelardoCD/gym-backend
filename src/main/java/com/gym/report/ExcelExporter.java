package com.gym.report;

import com.gym.attendance.Attendance;
import com.gym.attendance.AttendanceRepository;
import com.gym.billing.BillingService;
import com.gym.billing.MembershipStatus;
import com.gym.billing.Payment;
import com.gym.routine.Discipline;
import com.gym.user.Role;
import com.gym.user.User;
import com.gym.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Exporta pagos, asistencia o socios a un archivo .xlsx. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelExporter {
    public enum Kind { PAGOS, ASISTENCIA, SOCIOS }

    private final BillingService billing;
    private final AttendanceRepository attendances;
    private final UserRepository users;

    public byte[] export(Kind kind, LocalDate from, LocalDate to) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles st = new Styles(wb);
            switch (kind) {
                case PAGOS -> payments(wb, st, from, to);
                case ASISTENCIA -> attendance(wb, st, from, to);
                case SOCIOS -> members(wb, st);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void payments(Workbook wb, Styles st, LocalDate from, LocalDate to) {
        Sheet sh = sheet(wb, st, "Pagos", "Fecha", "Socio", "Membresía", "Cantidad", "Cubre desde", "Cubre hasta", "Método", "Monto", "Notas", "Registró");
        int r = 1;
        ZoneId zone = ZoneId.systemDefault();
        for (Payment p : billing.search(null, from, to)) {
            Row row = sh.createRow(r++);
            date(row, 0, p.getPaidAt().atZone(zone).toLocalDateTime(), st.dateTime);
            row.createCell(1).setCellValue(p.getUser().getFullName());
            row.createCell(2).setCellValue(p.getPlanName());
            row.createCell(3).setCellValue(p.getQuantity());
            date(row, 4, p.getCoversFrom().atStartOfDay(), st.date);
            date(row, 5, p.getCoversTo().atStartOfDay(), st.date);
            row.createCell(6).setCellValue(label(p.getMethod().name()));
            Cell amount = row.createCell(7);
            amount.setCellValue(p.getAmount().doubleValue());
            amount.setCellStyle(st.money);
            row.createCell(8).setCellValue(p.getNotes() == null ? "" : p.getNotes());
            row.createCell(9).setCellValue(p.getRegisteredBy().getFullName());
        }
        if (r > 1) {
            Row total = sh.createRow(r);
            total.createCell(6).setCellValue("Total");
            Cell c = total.createCell(7);
            c.setCellFormula("SUM(H2:H" + r + ")");
            c.setCellStyle(st.moneyBold);
        }
        finish(sh, 10);
    }

    private void attendance(Workbook wb, Styles st, LocalDate from, LocalDate to) {
        Sheet sh = sheet(wb, st, "Asistencia", "Usuario", "Entrada", "Salida", "Minutos");
        ZoneId zone = ZoneId.systemDefault();
        int r = 1;
        for (Attendance a : attendances.search(null, from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant())) {
            Row row = sh.createRow(r++);
            row.createCell(0).setCellValue(a.getUser().getFullName());
            date(row, 1, a.getCheckInAt().atZone(zone).toLocalDateTime(), st.dateTime);
            if (a.getCheckOutAt() != null) {
                date(row, 2, a.getCheckOutAt().atZone(zone).toLocalDateTime(), st.dateTime);
                row.createCell(3).setCellValue(Duration.between(a.getCheckInAt(), a.getCheckOutAt()).toMinutes());
            }
        }
        finish(sh, 4);
    }

    private void members(Workbook wb, Styles st) {
        Sheet sh = sheet(wb, st, "Socios", "Nombre", "Correo", "Teléfono", "Subrol", "Inscripción", "Pagado hasta", "Estado", "Activo");
        List<User> socios = users.findDistinctByRoles_NameOrderByFullName(Role.SOCIO);
        Map<Long, MembershipStatus> status = billing.statuses(socios).stream()
                .collect(Collectors.toMap(MembershipStatus::userId, Function.identity()));
        int r = 1;
        for (User u : socios) {
            MembershipStatus s = status.get(u.getId());
            Row row = sh.createRow(r++);
            row.createCell(0).setCellValue(u.getFullName());
            row.createCell(1).setCellValue(u.getEmail());
            row.createCell(2).setCellValue(u.getPhone() == null ? "" : u.getPhone());
            row.createCell(3).setCellValue(u.getDisciplines().stream().sorted().map(Discipline::label).collect(Collectors.joining(" y ")));
            date(row, 4, u.getEnrolledOn().atStartOfDay(), st.date);
            if (s.paidUntil() != null) {
                date(row, 5, s.paidUntil().atStartOfDay(), st.date);
            }
            row.createCell(6).setCellValue(label(s.state().name()));
            row.createCell(7).setCellValue(u.isActive() ? "Sí" : "No");
        }
        finish(sh, 8);
    }

    private static Sheet sheet(Workbook wb, Styles st, String name, String... headers) {
        Sheet sh = wb.createSheet(name);
        Row h = sh.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(st.header);
        }
        sh.createFreezePane(0, 1);
        return sh;
    }

    private static void finish(Sheet sh, int columns) {
        sh.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(sh.getLastRowNum(), 0), 0, columns - 1));
        for (int i = 0; i < columns; i++) {
            sh.autoSizeColumn(i);
            sh.setColumnWidth(i, Math.min(sh.getColumnWidth(i) + 512, 60 * 256));
        }
    }

    private static void date(Row row, int col, LocalDateTime value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    /** EFECTIVO -> Efectivo, POR_VENCER -> Por vencer. */
    private static String label(String enumName) {
        String s = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle date;
        final CellStyle dateTime;
        final CellStyle money;
        final CellStyle moneyBold;

        Styles(Workbook wb) {
            DataFormat fmt = wb.createDataFormat();
            Font bold = wb.createFont();
            bold.setBold(true);
            header = wb.createCellStyle();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            date = wb.createCellStyle();
            date.setDataFormat(fmt.getFormat("dd/mm/yyyy"));
            dateTime = wb.createCellStyle();
            dateTime.setDataFormat(fmt.getFormat("dd/mm/yyyy hh:mm"));
            money = wb.createCellStyle();
            money.setDataFormat(fmt.getFormat("$#,##0.00"));
            moneyBold = wb.createCellStyle();
            moneyBold.cloneStyleFrom(money);
            moneyBold.setFont(bold);
        }
    }
}
