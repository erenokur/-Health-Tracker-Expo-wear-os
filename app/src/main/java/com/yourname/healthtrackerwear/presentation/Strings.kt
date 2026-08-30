package com.yourname.healthtrackerwear.presentation

object Strings {
    private val tr = mapOf(
        "appTitle" to "Sağlık Monitörü",
        "addBp" to "Tansiyon Ekle",
        "medTaken" to "İlaç İçildi",
        "sys" to "Büyük (Sys)",
        "dia" to "Küçük (Dia)",
        "pulse" to "Nabız",
        "sending" to "Gönderiliyor...",
        "saved" to "Kaydedildi ✓",
        "errorRetry" to "Hata — Tekrar Dene",
        "save" to "KAYDET",
        "medNamePrompt" to "İlaç Adı Söyle/Yaz",
        "hungry" to "Aç",
        "full" to "Tok",
        "saveAsTaken" to "İÇİLDİ OLARAK KAYDET",
        "medNameLabel" to "İlaç adı",
        "otherMed" to "Diğer (Ses/Yaz)",
        "languageToggle" to "TR",
        "about" to "Hakkında",
        "aboutTitle" to "Sağlık Monitörü",
        "version" to "Versiyon",
        "phoneConnected" to "📶 Telefon bağlı",
        "phoneDisconnected" to "✗ Telefon bağlı değil",
        "checkingConnection" to "Bağlantı kontrol ediliyor...",
        "refresh" to "Yenile",
        "language" to "Dil",
        "medListEmpty" to "İlaç listesi yok",
        "medListHint" to "Telefon uygulamasında ilaç ekleyin",
        "refreshList" to "Listemi Güncelle",
    )

    private val en = mapOf(
        "appTitle" to "Health Monitor",
        "addBp" to "Add Blood Pressure",
        "medTaken" to "Medication Taken",
        "sys" to "Systolic (Sys)",
        "dia" to "Diastolic (Dia)",
        "pulse" to "Pulse",
        "sending" to "Sending...",
        "saved" to "Saved ✓",
        "errorRetry" to "Error — Retry",
        "save" to "SAVE",
        "medNamePrompt" to "Say/Type Medication Name",
        "hungry" to "Empty Stomach",
        "full" to "Full Stomach",
        "saveAsTaken" to "SAVE AS TAKEN",
        "medNameLabel" to "Medication name",
        "otherMed" to "Other (Voice/Type)",
        "languageToggle" to "EN",
        "about" to "About",
        "aboutTitle" to "Health Monitor",
        "version" to "Version",
        "phoneConnected" to "📶 Phone connected",
        "phoneDisconnected" to "✗ Phone not connected",
        "checkingConnection" to "Checking connection...",
        "refresh" to "Refresh",
        "language" to "Language",
        "medListEmpty" to "No medication list",
        "medListHint" to "Add medications in the phone app",
        "refreshList" to "Refresh My List",
    )

    fun get(key: String, lang: String): String {
        val map = if (lang == "en") en else tr
        return map[key] ?: key
    }
}
