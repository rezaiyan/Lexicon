package presentation.ui.components.imports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.from_file
import lexicon.resources.generated.resources.from_image
import lexicon.resources.generated.resources.type_text

@Stable
sealed class ImportTabV2(open val title: StringResource, open val icon: ImageVector) {
    @Stable
    data class Text(
        override val title: StringResource = Res.string.type_text,
        override val icon: ImageVector = Icons.Filled.Edit,
    ) : ImportTabV2(title, icon)

    @Stable
    data class File(
        override val title: StringResource = Res.string.from_file,
        override val icon: ImageVector = Icons.Filled.AttachFile,
    ) : ImportTabV2(title, icon)

    @Stable
    data class Image(
        override val title: StringResource = Res.string.from_image,
        override val icon: ImageVector = Icons.Filled.CameraAlt,
        val selectedImage: ByteArray? = null,
    ) : ImportTabV2(title, icon) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Image

            if (title != other.title) return false
            if (icon != other.icon) return false
            if (selectedImage != null && other.selectedImage != null) {
                if (!selectedImage.contentEquals(other.selectedImage)) return false
            } else if (!selectedImage.contentEquals(other.selectedImage)) {
                return false
            }

            return true
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + icon.hashCode()
            result = 31 * result + (selectedImage?.contentHashCode() ?: 0)
            return result
        }
    }
}