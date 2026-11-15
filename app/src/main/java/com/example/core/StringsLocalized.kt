package com.example.core

enum class AppLanguage(val code: String, val label: String) {
    EN("en", "English"),
    HI("hi", "हिन्दी"),
    KN("kn", "ಕನ್ನಡ");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.firstOrNull { it.code == code } ?: EN
    }
}

data class LocalizedStrings(
    val appTitle: String,
    val tagline: String,
    val signInTitle: String,
    val signInSubtitle: String,
    val signInWithGoogle: String,
    val navHome: String,
    val navMap: String,
    val navHub: String,
    val navTrack: String,
    val navProfile: String,
    val roleDonor: String,
    val roleReceiver: String,
    val roleVolunteer: String,
    val postDonation: String,
    val browseFood: String,
    val volunteerPickups: String,
    val emergencyHelp: String,
    val mealsSaved: String,
    val wasteReduced: String,
    val activeListings: String,
    val valueSaved: String,
    val fssaiBanner: String,
    val requestFood: String,
    val claimTask: String,
    val trackDelivery: String,
    val safetyConfirmText: String,
    val privacyProtectedNote: String
)

object AppStrings {
    fun get(lang: AppLanguage): LocalizedStrings = when (lang) {
        AppLanguage.EN -> LocalizedStrings(
            appTitle = "FoodBridge India",
            tagline = "Share Food, Spread Hope. No Food Wasted, No One Hungry.",
            signInTitle = "Bridge Surplus to Smiles",
            signInSubtitle = "Connect restaurants, events, NGOs, shelters, and volunteers across India with real-time food safety tracking.",
            signInWithGoogle = "Continue with Google",
            navHome = "Home",
            navMap = "Map",
            navHub = "Hub",
            navTrack = "Track",
            navProfile = "Profile",
            roleDonor = "Food Donor",
            roleReceiver = "Food Receiver / NGO",
            roleVolunteer = "Volunteer",
            postDonation = "Post Surplus Food",
            browseFood = "Find Food Nearby",
            volunteerPickups = "Volunteer Pickups",
            emergencyHelp = "Emergency Request",
            mealsSaved = "Meals Saved",
            wasteReduced = "Waste Reduced",
            activeListings = "Active Listings",
            valueSaved = "Est. Value Saved",
            fssaiBanner = "FSSAI Surplus Food Hygiene Verified",
            requestFood = "Request Food",
            claimTask = "Accept Pickup",
            trackDelivery = "Track Delivery",
            safetyConfirmText = "I confirm this food is hygienically stored, unexpired, and safe for human consumption per FSSAI guidelines.",
            privacyProtectedNote = "Exact pickup address & +91 mobile are hidden publicly and shared only after match consent."
        )

        AppLanguage.HI -> LocalizedStrings(
            appTitle = "फूडब्रिज इंडिया",
            tagline = "भोजन बांटें, आशा फैलाएं। न अन्न की बर्बादी, न कोई भूखा रहे।",
            signInTitle = "अधिशेष भोजन से मुस्कान जोड़ें",
            signInSubtitle = "सुरक्षित और रीयल-टाइम ट्रैकिंग के साथ पूरे भारत में रेस्तरां, एनजीओ, आश्रय और स्वयंसेवकों को जोड़ें।",
            signInWithGoogle = "Google से जारी रखें",
            navHome = "होम",
            navMap = "मानचित्र",
            navHub = "केंद्र",
            navTrack = "ट्रैक",
            navProfile = "प्रोफ़ाइल",
            roleDonor = "भोजन दाता",
            roleReceiver = "प्राप्तकर्ता / एनजीओ",
            roleVolunteer = "स्वयंसेवक",
            postDonation = "अधिशेष भोजन दान करें",
            browseFood = "आस-पास भोजन खोजें",
            volunteerPickups = "स्वयंसेवक पिकअप",
            emergencyHelp = "आपातकालीन अनुरोध",
            mealsSaved = "भोजन बचाया",
            wasteReduced = "बर्बादी कम हुई",
            activeListings = "सक्रिय दान",
            valueSaved = "बचाया गया मूल्य",
            fssaiBanner = "FSSAI खाद्य सुरक्षा मानकों के अनुरूप",
            requestFood = "भोजन अनुरोध करें",
            claimTask = "पिकअप स्वीकार करें",
            trackDelivery = "डिलीवरी ट्रैक करें",
            safetyConfirmText = "मैं पुष्टि करता/करती हूं कि यह भोजन स्वच्छ, ताज़ा और FSSAI मानकों के अनुसार खाने के लिए सुरक्षित है।",
            privacyProtectedNote = "सटीक पता और +91 नंबर सार्वजनिक नहीं दिखते; केवल स्वीकृत मिलान पर साझा होते हैं।"
        )

        AppLanguage.KN -> LocalizedStrings(
            appTitle = "ಫುಡ್‌ಬ್ರಿಡ್ಜ್ ಇಂಡಿಯಾ",
            tagline = "ಆಹಾರ ಹಂಚಿ, ಭರವಸೆ ಹರಡಿ. ಆಹಾರ ವ್ಯರ್ಥ ಬೇಡ, ಯಾರೂ ಹಸಿವಿನಿಂದ ಇರಬಾರದು.",
            signInTitle = "ಹೆಚ್ಚುವರಿ ಆಹಾರವನ್ನು ಹಸಿದವರಿಗೆ ತಲುಪಿಸಿ",
            signInSubtitle = "ಭಾರತದಾದ್ಯಂತ ರೆಸ್ಟೋರೆಂಟ್‌ಗಳು, ಎನ್‌ಜಿಒಗಳು, ಆಶ್ರಯ ತಾಣಗಳು ಮತ್ತು ಸ್ವಯಂಸೇವಕರನ್ನು ನೇರವಾಗಿ ಸಂಪರ್ಕಿಸಿ.",
            signInWithGoogle = "Google ಮೂಲಕ ಮುಂದುವರಿಸಿ",
            navHome = "ಮುಖಪುಟ",
            navMap = "ನಕ್ಷೆ",
            navHub = "ಕೇಂದ್ರ",
            navTrack = "ಟ್ರ್ಯಾಕ್",
            navProfile = "ಪ್ರೊಫೈಲ್",
            roleDonor = "ಆಹಾರ ದಾನಿ",
            roleReceiver = "ಸ್ವೀಕರಿಸುವವರು / NGO",
            roleVolunteer = "ಸ್ವಯಂಸೇವಕ",
            postDonation = "ಆಹಾರ ದಾನ ಮಾಡಿ",
            browseFood = "ಹತ್ತಿರದ ಆಹಾರ ಹುಡುಕಿ",
            volunteerPickups = "ಸ್ವಯಂಸೇವಕ ಪಿಕಪ್",
            emergencyHelp = "ತುರ್ತು ಆಹಾರ ವಿನಂತಿ",
            mealsSaved = "ಉಳಿಸಿದ ಊಟ",
            wasteReduced = "ತಡೆದ ವ್ಯರ್ಥ",
            activeListings = "ಸಕ್ರಿಯ ದಾನಗಳು",
            valueSaved = "ಉಳಿಸಿದ ಮೌಲ್ಯ",
            fssaiBanner = "FSSAI ಆಹಾರ ಸುರಕ್ಷತೆ ಪರಿಶೀಲಿಸಲಾಗಿದೆ",
            requestFood = "ಆಹಾರ ವಿನಂತಿಸಿ",
            claimTask = "ಪಿಕಪ್ ಸ್ವೀಕರಿಸಿ",
            trackDelivery = "ವಿತರಣೆ ಟ್ರ್ಯಾಕ್ ಮಾಡಿ",
            safetyConfirmText = "ಈ ಆಹಾರವು ಶುಚಿಯಾಗಿದೆ, ಅವಧಿ ಮೀರಿಲ್ಲ ಮತ್ತು FSSAI ಮಾರ್ಗಸೂಚಿಗಳ ಪ್ರಕಾರ ಸುರಕ್ಷಿತವಾಗಿದೆ ಎಂದು ನಾನು ದೃಢೀಕರಿಸುತ್ತೇನೆ.",
            privacyProtectedNote = "ನಿಖರ ವಿಳಾಸ ಮತ್ತು +91 ಸಂಖ್ಯೆಯನ್ನು ಸಾರ್ವಜನಿಕವಾಗಿ ಮರೆಮಾಡಲಾಗುತ್ತದೆ."
        )
    }
}
