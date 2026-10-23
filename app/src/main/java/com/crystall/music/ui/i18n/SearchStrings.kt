package com.crystall.music.ui.i18n

data class SearchStrings(
    val error: String,
    val retry: String,
    val lessArtist: String,
    val recommendations: String,
    val basedOnTaste: String,
    val basedOnMood: String,
    val trending: String,
    val restoreArtists: String,
    val artistTracks: String,
    val back: String
)

fun searchStrings(language: AppLanguage): SearchStrings = when (language) {
    AppLanguage.RU -> SearchStrings(
        "Не удалось выполнить поиск. Проверьте подключение.", "Повторить",
        "Меньше этого артиста", "Почему эти рекомендации?", "На основе выбранных артистов",
        "На основе выбранного настроения", "Популярные треки: выберите артистов для персонализации",
        "Сбросить скрытых артистов", "Найти больше треков", "Назад"
    )
    AppLanguage.AZ -> SearchStrings(
        "Axtarış alınmadı. Bağlantını yoxlayın.", "Yenidən cəhd et",
        "Bu ifaçıdan daha az", "Niyə bu tövsiyələr?", "Seçilmiş ifaçılara əsasən",
        "Seçilmiş əhvala əsasən", "Populyar mahnılar: fərdiləşdirmək üçün ifaçı seçin",
        "Gizlədilmiş ifaçıları sıfırla", "Daha çox mahnı tap", "Geri"
    )
    AppLanguage.TR -> SearchStrings(
        "Arama başarısız. Bağlantınızı kontrol edin.", "Tekrar dene",
        "Bu sanatçıdan daha az", "Neden bu öneriler?", "Seçilen sanatçılara göre",
        "Seçilen ruh haline göre", "Popüler parçalar: kişiselleştirmek için sanatçı seçin",
        "Gizlenen sanatçıları sıfırla", "Daha fazla parça bul", "Geri"
    )
    AppLanguage.EN -> SearchStrings(
        "Search failed. Check your connection.", "Retry", "Less of this artist",
        "Why these recommendations?", "Based on your selected artists", "Based on your selected mood",
        "Trending tracks: select artists to personalize", "Reset hidden artists", "Find more tracks", "Back"
    )
}
