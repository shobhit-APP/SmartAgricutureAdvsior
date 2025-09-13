package com.example.agriconnect.Service;

import com.example.Authentication.repository.UserRepo;
import com.example.agriconnect.Repository.CropDiseaseRepo;
import com.example.agriconnect.Repository.CropRecommendationRepo;
import com.example.agriconnect.Repository.cropPriceRepo;
import com.example.common.Model.Crop;
import com.example.common.Model.CropDisease;
import com.example.common.Model.CropRecommendation;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;

@Service
public class ExportService {

    @Autowired
    private CropDiseaseRepo cropDiseaseRepository;

    @Autowired
    private CropRecommendationRepo cropRecommendationRepo;

    @Autowired
    private cropPriceRepo cropPriceRepo;

    @Autowired
    private UserRepo userRepo;

    public byte[] exportToPDFByUserId(String cropType, String cause, String lang,
                                      Long userId, String state, String crop_name, String page) throws Exception {

        List<CropDisease> diseases = null;
        List<CropRecommendation> cropRecommendations = null;
        List<Crop> cropList = null;

        boolean hasCropTypeAndCause = cropType != null && !cropType.isEmpty() && cause != null && !cause.isEmpty();
        boolean hasState = state != null && !state.isEmpty();
        boolean hasCropName = crop_name != null && !crop_name.isEmpty();

        // ----- Query selection -----
        if ("hi".equalsIgnoreCase(lang)) {
            if (hasCropTypeAndCause) {
                diseases = cropDiseaseRepository.findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserDetails1UserId(cropType, cause, userId);
            } else if (cropType != null && !cropType.isEmpty()) {
                diseases = cropDiseaseRepository.findByCropTypeHiAndUserDetails1UserId(cropType, userId);
            } else if (cause != null && !cause.isEmpty()) {
                diseases = cropDiseaseRepository.findByCauseHiContainingIgnoreCaseAndUserDetails1UserId(cause, userId);
            } else if (Objects.equals(page, "CropPrice_DashBoard")) {
                if (hasState) {
                    cropList = cropPriceRepo.findByStateAndUserDetails1UserId(state, userId);
                } else {
                    cropList = cropPriceRepo.findByUserDetails1UserId(userId);
                }
            } else if (Objects.equals(page, "CropRecommend_DashBoard")) {
                if (hasCropName) {
                    cropRecommendations = cropRecommendationRepo.findByUserIdAndPredictedCrop(userId, crop_name);
                } else {
                    cropRecommendations = cropRecommendationRepo.findByUserDetails1UserId(userId);
                }
            } else {
                diseases = cropDiseaseRepository.findByUserDetails1UserId(userId);
            }
        } else {
            if (hasCropTypeAndCause) {
                diseases = cropDiseaseRepository.findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserDetails1UserId(cropType, cause, userId);
            } else if (cropType != null && !cropType.isEmpty()) {
                diseases = cropDiseaseRepository.findByCropTypeEnAndUserDetails1UserId(cropType, userId);
            } else if (cause != null && !cause.isEmpty()) {
                diseases = cropDiseaseRepository.findByCauseEnContainingIgnoreCaseAndUserDetails1UserId(cause, userId);
            } else if (Objects.equals(page, "CropPrice_DashBoard")) {
                if (hasState) {
                    cropList = cropPriceRepo.findByStateAndUserDetails1UserId(state, userId);
                } else {
                    cropList = cropPriceRepo.findByUserDetails1UserId(userId);
                }
            } else if (Objects.equals(page, "CropRecommend_DashBoard")) {
                if (hasCropName) {
                    cropRecommendations = cropRecommendationRepo.findByUserIdAndPredictedCrop(userId, crop_name);
                } else {
                    cropRecommendations = cropRecommendationRepo.findByUserDetails1UserId(userId);
                }
            } else {
                diseases = cropDiseaseRepository.findByUserDetails1UserId(userId);
            }
        }

        // ----- PDF Building -----
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Add Title
        document.add(new Paragraph("AgriConnect Export Report", titleFont));
        document.add(new Paragraph("Language: " + lang.toUpperCase(), bodyFont));
        document.add(Chunk.NEWLINE);

        // ----------------- CropPrice_DashBoard -----------------
        if (Objects.equals(page, "CropPrice_DashBoard") && cropList != null) {
            PdfPTable table = new PdfPTable(7); // Increased to 8 columns to include SuggestedPrice separately
            table.setWidthPercentage(100);

            if ("hi".equalsIgnoreCase(lang)) {
                addTableHeader(table, new String[]{"राज्य", "ज़िला", "बाज़ार", "फ़सल का नाम", "न्यूनतम कीमत", "अधिकतम कीमत", "सुझाई गई कीमत"}, headerFont);
            } else {
                addTableHeader(table, new String[]{"State", "District", "Market", "Crop", "Min Price", "Max Price", "Suggested Price"}, headerFont);
            }

            for (Crop crop : cropList) {
                table.addCell(new PdfPCell(new Phrase(crop.getState(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(crop.getDistrict(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(crop.getMarket(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(crop.getCropName(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(crop.getMinPrice()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(crop.getMaxPrice()), bodyFont)));
//                table.addCell(new PdfPCell(new Phrase(String.valueOf(crop.getSuggestedPrice()), bodyFont))); // Corrected to SuggestedPrice
                table.addCell(new PdfPCell(new Phrase(String.valueOf(crop.getBestPrice()), bodyFont))); // Best Predicted Price
            }
            document.add(table);

            // ----------------- CropRecommend_DashBoard -----------------
        } else if (Objects.equals(page, "CropRecommend_DashBoard") && cropRecommendations != null) {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            if ("hi".equalsIgnoreCase(lang)) {
                addTableHeader(table, new String[]{"नाइट्रोजन (N)", "फॉस्फोरस (P)", "पोटैशियम (K)", "अनुमानित फसल", "विवरण_हिं"}, headerFont);
            } else {
                addTableHeader(table, new String[]{"Nitrogen (N)", "Phosphorus (P)", "Potassium (K)", "Predicted Crop", "English Description"}, headerFont);
            }

            for (CropRecommendation rec : cropRecommendations) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(rec.getN()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(rec.getP()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(rec.getK()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase("hi".equalsIgnoreCase(lang) ? rec.getPredictedCropHindi() : rec.getPredictedCrop(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase("hi".equalsIgnoreCase(lang) ? rec.getHindiDescription() : rec.getEnglishDescription(), bodyFont)));
            }
            document.add(table);

            // ----------------- CropDisease Default -----------------
        } else if (diseases != null) {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            if ("hi".equalsIgnoreCase(lang)) {
                addTableHeader(table, new String[]{"रोग का नाम", "फसल का प्रकार", "कारण", "लक्षण", "उपचार"}, headerFont);
            } else {
                addTableHeader(table, new String[]{"Disease Name", "Crop Type", "Cause", "Symptoms", "Treatment"}, headerFont);
            }

            for (CropDisease disease : diseases) {
                table.addCell(new PdfPCell(new Phrase("hi".equalsIgnoreCase(lang) ? disease.getNameHi() : disease.getNameEn(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase("hi".equalsIgnoreCase(lang) ? disease.getCropTypeHi() : disease.getCropTypeEn(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase("hi".equalsIgnoreCase(lang) ? disease.getCauseHi() : disease.getCauseEn(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase("hi".equalsIgnoreCase(lang) ? disease.getSymptomsHi() : disease.getSymptomsEn(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase("hi".equalsIgnoreCase(lang) ? disease.getSuggestionHi() : disease.getSuggestionEn(), bodyFont)));
            }
            document.add(table);
        }

        document.close();
        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
    }
}