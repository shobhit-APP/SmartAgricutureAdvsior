package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Export API", description = "Endpoint for exporting crop disease data to PDF")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/export")
    @Operation(
            summary = "Export crop disease data to PDF",
            description = "Generates a PDF report of crop disease data for the authenticated user, filtered by language, crop type, cause, crop name, state, and page."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF report generated successfully",
                    content = @Content(
                            mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "204",
                    description = "No content available to export",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error generating PDF report",
                    content = @Content(
                            mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<byte[]> exportDiseasesToPDF(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @Parameter(description = "Language for the report (e.g., 'en' for English, 'hi' for Hindi)", example = "en")
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @Parameter(description = "Page number or identifier for filtering (optional)", example = "1")
            @RequestParam(value = "page", required = false) String page,
            @Parameter(description = "State to filter disease data (optional)", example = "Maharashtra")
            @RequestParam(value = "state", required = false) String state,
            @Parameter(description = "Crop name to filter disease data (optional)", example = "Wheat")
            @RequestParam(value = "crop_name", required = false) String crop_name,
            @Parameter(description = "Crop type to filter disease data (optional)", example = "Cereal")
            @RequestParam(value = "cropType", required = false) String cropType,
            @Parameter(description = "Cause of disease to filter data (optional)", example = "Fungus")
            @RequestParam(value = "cause", required = false) String cause) {
        try {
            // Retrieve user ID from authenticated principal
            Long userId = userPrinciples.getUserId();

            // Generate PDF report using export service
            byte[] pdfBytes = exportService.exportToPDFByUserId(
                    cropType, cause, lang, userId, crop_name, state, page
            );

            // Check if PDF content is empty
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            // Return PDF as binary response with appropriate headers
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"crop_report.pdf\"")
                    .body(pdfBytes);

        } catch (Exception e) {
            // Return error message as byte array if PDF generation fails
            return ResponseEntity.badRequest()
                    .body(("Error exporting PDF: " + e.getMessage()).getBytes());
        }
    }
}