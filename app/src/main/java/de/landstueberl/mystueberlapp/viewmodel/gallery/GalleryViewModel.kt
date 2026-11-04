package de.landstueberl.mystueberlapp.viewmodel.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.GallerySection
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.repository.GalleryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.File

class GalleryViewModel(
    repo: GalleryRepository,
    private val imageStorage: ProductImageStorage
) : ViewModel() {

    /**
     * Null until the first emission, so the screen can tell "loading" from
     * "no photos yet".
     */
    val sections: StateFlow<List<GallerySection>?> = repo
        .getGallerySectionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Resolves a stored file name to a file Coil can load. */
    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            repo: GalleryRepository,
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<GalleryViewModel> { GalleryViewModel(repo, imageStorage) }
        }
    }
}