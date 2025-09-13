package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDiseasesToPDF(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "crop_name", required = false) String crop_name,
            @RequestParam(value = "cropType", required = false) String cropType,
            @RequestParam(value = "cause", required = false) String cause) {
        try {
            Long userId = userPrinciples.getUserId();

            // Service should return PDF as byte[]
            byte[] pdfBytes = exportService.exportToPDFByUserId(
                    cropType, cause, lang, userId, crop_name, state, page
            );

            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"crop_report.pdf\"")
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("Error exporting PDF: " + e.getMessage()).getBytes());
        }
    }
}
