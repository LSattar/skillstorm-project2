package com.skillstorm.reserveone.controllers;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.services.PaymentTransactionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @GetMapping("/payment-transactions")
    public ResponseEntity<byte[]> generatePaymentTransactionReport(
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        List<PaymentTransaction> transactions = paymentTransactionService.findByDateRange(fromDate, toDate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);
        // CSV Header
        writer.println(
                "PaymentID,ReservationID,UserID,Amount,Currency,Status,Provider,CreatedAt,UpdatedAt");
        for (PaymentTransaction tx : transactions) {
            writer.printf("%s,%s,%s,%.2f,%s,%s,%s,%s,%s\n",
                    tx.getPaymentId(),
                    tx.getReservationId(),
                    tx.getUserId(),
                    tx.getAmount(),
                    tx.getCurrency(),
                    tx.getStatus(),
                    tx.getProvider(),
                    tx.getCreatedAt(),
                    tx.getUpdatedAt());
        }
        writer.flush();
        byte[] csvBytes = out.toByteArray();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payment-transactions-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}
