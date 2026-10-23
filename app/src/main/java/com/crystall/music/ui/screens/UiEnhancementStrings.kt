package com.crystall.music.ui.screens

import com.crystall.music.ui.i18n.AppLanguage

internal data class UiEnhancementStrings(
    val searchLibrary: String,
    val clearSearch: String,
    val sortBy: String,
    val sortTitle: String,
    val sortArtist: String,
    val noMatches: String,
    val offlineCenter: String,
    val storageUsed: String,
    val storageFree: String,
    val storageUnavailable: String,
    val wifiOnly: String,
    val downloading: String,
    val failed: String,
    val cancelDownload: String,
    val retryDownload: String,
    val renamePlaylist: String,
    val playlistName: String,
    val removeTrack: String,
    val save: String,
    val cancel: String,
    val recommendationExplanation: String,
    val recommendationExplanationBody: String,
    val lessThisArtist: String,
    val lessThisArtistDescription: String,
    val currentArtist: String,
    val selectedArtistsCount: String,
    val downloadedTracksCount: String
)

internal fun uiEnhancementStrings(language: AppLanguage): UiEnhancementStrings = when (language) {
    AppLanguage.EN -> UiEnhancementStrings(
        searchLibrary = "Search this collection", clearSearch = "Clear search", sortBy = "Sort by", sortTitle = "Title", sortArtist = "Artist",
        noMatches = "No matches found", offlineCenter = "Offline downloads", storageUsed = "Used", storageFree = "Free",
        storageUnavailable = "Storage information unavailable", wifiOnly = "Download over Wi-Fi only",
        downloading = "Downloading", failed = "Download failed", cancelDownload = "Cancel", retryDownload = "Retry",
        renamePlaylist = "Rename playlist", playlistName = "Playlist name", removeTrack = "Remove from playlist",
        save = "Save", cancel = "Cancel", recommendationExplanation = "How recommendations work",
        recommendationExplanationBody = "Recommendations use your selected artists and mood; otherwise trending tracks are shown. Hidden artists and disliked tracks are excluded.",
        lessThisArtist = "Hear less from an artist", lessThisArtistDescription = "Choose an artist to reduce similar recommendations.",
        currentArtist = "Currently playing: %s",
        selectedArtistsCount = "%d artists selected", downloadedTracksCount = "%d tracks stored on this device"
    )
    AppLanguage.RU -> UiEnhancementStrings(
        searchLibrary = "Поиск в этой коллекции", clearSearch = "Очистить поиск", sortBy = "Сортировка", sortTitle = "Название", sortArtist = "Исполнитель",
        noMatches = "Ничего не найдено", offlineCenter = "Офлайн-загрузки", storageUsed = "Занято", storageFree = "Свободно",
        storageUnavailable = "Сведения о хранилище недоступны", wifiOnly = "Загружать только по Wi-Fi",
        downloading = "Загрузка", failed = "Ошибка загрузки", cancelDownload = "Отменить", retryDownload = "Повторить",
        renamePlaylist = "Переименовать плейлист", playlistName = "Название плейлиста", removeTrack = "Убрать из плейлиста",
        save = "Сохранить", cancel = "Отмена", recommendationExplanation = "Как работают рекомендации",
        recommendationExplanationBody = "Рекомендации основаны на выбранных артистах и настроении; без предпочтений показываются популярные треки. Скрытые артисты и треки с дизлайком исключаются.",
        lessThisArtist = "Реже включать исполнителя", lessThisArtistDescription = "Выберите исполнителя, чтобы сократить похожие рекомендации.",
        currentArtist = "Сейчас играет: %s",
        selectedArtistsCount = "Выбрано исполнителей: %d", downloadedTracksCount = "Треков на устройстве: %d"
    )
    AppLanguage.AZ -> UiEnhancementStrings(
        searchLibrary = "Bu kolleksiyada axtar", clearSearch = "Axtarışı təmizlə", sortBy = "Sırala", sortTitle = "Ad", sortArtist = "İfaçı",
        noMatches = "Nəticə tapılmadı", offlineCenter = "Oflayn yükləmələr", storageUsed = "İstifadə olunur", storageFree = "Boş",
        storageUnavailable = "Yaddaş məlumatı əlçatan deyil", wifiOnly = "Yalnız Wi-Fi ilə yüklə",
        downloading = "Yüklənir", failed = "Yükləmə alınmadı", cancelDownload = "Ləğv et", retryDownload = "Yenidən yoxla",
        renamePlaylist = "Pleylistin adını dəyiş", playlistName = "Pleylistin adı", removeTrack = "Pleylistdən sil",
        save = "Yadda saxla", cancel = "Ləğv et", recommendationExplanation = "Tövsiyələr necə işləyir",
        recommendationExplanationBody = "Tövsiyələr seçilmiş ifaçılara və əhvala əsaslanır; seçim yoxdursa populyar mahnılar göstərilir. Gizlədilmiş ifaçılar və bəyənilməyən mahnılar çıxarılır.",
        lessThisArtist = "Bu ifaçını daha az səsləndir", lessThisArtistDescription = "Oxşar tövsiyələri azaltmaq üçün ifaçı seçin.",
        currentArtist = "Hazırda səslənir: %s",
        selectedArtistsCount = "Seçilmiş ifaçı sayı: %d", downloadedTracksCount = "Cihazda saxlanılan trek: %d"
    )
    AppLanguage.TR -> UiEnhancementStrings(
        searchLibrary = "Bu koleksiyonda ara", clearSearch = "Aramayı temizle", sortBy = "Sırala", sortTitle = "Başlık", sortArtist = "Sanatçı",
        noMatches = "Sonuç bulunamadı", offlineCenter = "Çevrimdışı indirmeler", storageUsed = "Kullanılan", storageFree = "Boş",
        storageUnavailable = "Depolama bilgisi kullanılamıyor", wifiOnly = "Yalnızca Wi-Fi ile indir",
        downloading = "İndiriliyor", failed = "İndirme başarısız", cancelDownload = "İptal", retryDownload = "Yeniden dene",
        renamePlaylist = "Oynatma listesini yeniden adlandır", playlistName = "Oynatma listesi adı", removeTrack = "Listeden kaldır",
        save = "Kaydet", cancel = "İptal", recommendationExplanation = "Öneriler nasıl çalışır",
        recommendationExplanationBody = "Öneriler seçtiğiniz sanatçılara ve ruh haline dayanır; seçim yoksa popüler parçalar gösterilir. Gizlenen sanatçılar ve beğenilmeyen parçalar hariç tutulur.",
        lessThisArtist = "Bu sanatçıyı daha az dinle", lessThisArtistDescription = "Benzer önerileri azaltmak için bir sanatçı seçin.",
        currentArtist = "Şu anda çalıyor: %s",
        selectedArtistsCount = "%d sanatçı seçildi", downloadedTracksCount = "Cihazda saklanan parça: %d"
    )
}
