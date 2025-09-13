package com.example.common.util;

import com.example.common.Model.CropInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TranslateToHindi {

    private static final Map<String, CropInfo> cropDict = new HashMap<>();

    static {
        cropDict.put("rice", new CropInfo("चावल", "भारत में व्यापक रूप से खाया जाने वाला मुख्य अनाज।",
                "A staple grain widely consumed in India."));
        cropDict.put("maize", new CropInfo("मक्का", "अनाज और पशु चारे के रूप में उपयोग किया जाता है।",
                "Also known as corn, used as food and fodder."));
        cropDict.put("jute", new CropInfo("जूट", "मजबूत रेशा और बोरी बनाने के लिए उगाया जाने वाला पौधा।",
                "A plant used for making strong fiber and sacks."));
        cropDict.put("cotton", new CropInfo("कपास", "कपड़ा उद्योग में कपड़े बनाने के लिए प्रयुक्त होता है।",
                "Used in textile industry for making clothes."));
        cropDict.put("coconut", new CropInfo("नारियल", "पानी, दूध और तेल के लिए उपयोग किया जाने वाला फल।",
                "Used for water, milk, and oil in tropical regions."));
        cropDict.put("papaya",
                new CropInfo("पपीता", "पाचन में सहायक उष्णकटिबंधीय फल।", "A tropical fruit that aids digestion."));
        cropDict.put("orange",
                new CropInfo("संतरा", "विटामिन C से भरपूर खट्टा-मीठा फल।", "A citrus fruit rich in Vitamin C."));
        cropDict.put("apple", new CropInfo("सेब", "स्वादिष्ट और पोषक तत्वों से भरपूर फल।",
                "Nutritious fruit known for its sweetness."));
        cropDict.put("muskmelon", new CropInfo("खरबूजा", "गर्मियों में खाया जाने वाला मीठा और रसीला फल।",
                "A sweet, juicy summer fruit rich in water content."));
        cropDict.put("watermelon", new CropInfo("तरबूज", "गर्म जलवायु में मिलने वाला ठंडा और रसीला फल।",
                "A large, refreshing fruit ideal for hot climates."));
        cropDict.put("grapes", new CropInfo("अंगूर", "छोटे और मीठे फल, ताजे या वाइन बनाने में उपयोग होते हैं।",
                "Small, sweet fruits used fresh or for making wine."));
        cropDict.put("mango", new CropInfo("आम", "फलों का राजा, गर्मियों में सबसे अधिक पसंद किया जाने वाला फल।",
                "Known as the king of fruits, very popular in summer."));
        cropDict.put("banana",
                new CropInfo("केला", "पोटैशियम से भरपूर उष्णकटिबंधीय फल।", "A tropical fruit rich in potassium."));
        cropDict.put("pomegranate", new CropInfo("अनार", "लाल बीजों वाला फल जो एंटीऑक्सीडेंट्स से भरपूर होता है।",
                "A fruit with red seeds rich in antioxidants."));
        cropDict.put("lentil",
                new CropInfo("मसूर", "प्रोटीन से भरपूर एक प्रकार की दाल।", "A type of pulse rich in protein."));
        cropDict.put("blackgram", new CropInfo("उड़द", "दालों और खमीरयुक्त व्यंजन बनाने में प्रयुक्त।",
                "Used for making dals and fermented dishes."));
        cropDict.put("mungbean", new CropInfo("मूंग", "अंकुरित और दाल के रूप में उपयोग होने वाली हरी दाल।",
                "A green pulse used for sprouts and dals."));
        cropDict.put("mothbeans", new CropInfo("मटकी", "सूखे क्षेत्रों में उगाई जाने वाली दाल।",
                "A drought-resistant pulse grown in arid areas."));
        cropDict.put("pigeonpeas", new CropInfo("अरहर", "तूर दाल के रूप में जानी जाने वाली आम दाल।",
                "A common pulse also known as toor dal."));
        cropDict.put("kidneybeans", new CropInfo("राजमा", "भारतीय करी में प्रयुक्त एक प्रकार की फलियां।",
                "A type of legume commonly used in Indian curries."));
        cropDict.put("chickpea", new CropInfo("चना", "छोले और भुने हुए स्नैक्स के रूप में खाया जाता है।",
                "Used for chole and also eaten as roasted snacks."));
        cropDict.put("coffee",
                new CropInfo("कॉफी", "एक लोकप्रिय कैफीनयुक्त पेय फसल।", "Popular caffeinated beverage crop."));
        // ✅ Add more crops as needed...
    }

    public CropInfo getCropInfo(String cropName) {
        cropName = cropName.toLowerCase();

        if (cropDict.containsKey(cropName)) {
            return cropDict.get(cropName);
        }

        // If crop not in dictionary, call API to translate
        String translatedHindi = callTranslationAPI(cropName);
        return new CropInfo(translatedHindi, "इस फसल के लिए विवरण उपलब्ध नहीं है।",
                "No description available for this crop.");
    }

    private String callTranslationAPI(String text) {
        String encodedText = text.replace(" ", "%20");
        String url = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=en|hi";

        RestTemplate restTemplate = new RestTemplate();

        try {
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("responseData")) {
                Map responseData = (Map) response.get("responseData");
                if (responseData != null && responseData.containsKey("translatedText")) {
                    return responseData.get("translatedText").toString();
                }
            }
            return "अनुवाद उपलब्ध नहीं है";
        } catch (Exception e) {
            log.error("Translation API error: {}", e.getMessage());
            return "अनुवाद उपलब्ध नहीं है";
        }
    }
}