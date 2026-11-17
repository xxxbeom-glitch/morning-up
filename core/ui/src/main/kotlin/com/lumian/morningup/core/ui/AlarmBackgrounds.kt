package com.lumian.morningup.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** S8 알람 화면 배경 묶음(탭 순서). 내 사진은 묶음 대신 헤더 버튼으로 더해요. */
enum class BackgroundGroup(@StringRes val title: Int) {
    Sky(R.string.ui_bg_group_sky),
    Nature(R.string.ui_bg_group_nature),
    Solid(R.string.ui_bg_group_solid),
    Photo(R.string.ui_bg_group_photo),
}

/**
 * 고를 수 있는 배경 하나. [id]가 null이면 기본 '시간대 하늘'(울림 화면의 Ring Plain)이에요.
 * 색은 사용자가 고르는 내용 색이라 토큰 없이 원색으로 둬요(design-system S8). [darkText]는 미리보기 글자 색이에요.
 */
data class AlarmBackground(
    val id: String?,
    val group: BackgroundGroup,
    @StringRes val name: Int,
    val top: Color,
    val bottom: Color,
    val darkText: Boolean,
)

private fun bg(id: String?, group: BackgroundGroup, @StringRes name: Int, top: Long, bottom: Long, darkText: Boolean) =
    AlarmBackground(id, group, name, Color(top), Color(bottom), darkText)

object AlarmBackgrounds {
    const val PHOTO_PREFIX = "photo:"
    private const val PHOTO_DIR = "backgrounds"

    val all: List<AlarmBackground> = listOf(
        bg(null, BackgroundGroup.Sky, R.string.ui_bg_time_sky, 0xFF8EC5EA, 0xFFF3D9C0, true),
        bg("sunrise", BackgroundGroup.Sky, R.string.ui_bg_sunrise, 0xFFF4B58C, 0xFFFFF1E4, true),
        bg("dawn", BackgroundGroup.Sky, R.string.ui_bg_dawn, 0xFF1F2B4D, 0xFF3A4F82, false),
        bg("clear", BackgroundGroup.Sky, R.string.ui_bg_clear, 0xFF6BB6E6, 0xFFCDE7F8, true),
        bg("night", BackgroundGroup.Sky, R.string.ui_bg_night, 0xFF0F1626, 0xFF26355E, false),
        bg("mint", BackgroundGroup.Nature, R.string.ui_bg_mint, 0xFF7FD8BE, 0xFFE3F7F0, true),
        bg("forest", BackgroundGroup.Nature, R.string.ui_bg_forest, 0xFF3F7A57, 0xFFA9D4B3, false),
        bg("lime", BackgroundGroup.Nature, R.string.ui_bg_lime, 0xFFC8E27A, 0xFFF4F8D8, true),
        bg("peach", BackgroundGroup.Solid, R.string.ui_bg_peach, 0xFFF9D3C0, 0xFFF9D3C0, true),
        bg("gray", BackgroundGroup.Solid, R.string.ui_bg_gray, 0xFFE6E8EC, 0xFFE6E8EC, true),
        bg("sand", BackgroundGroup.Solid, R.string.ui_bg_sand, 0xFFE8D8BC, 0xFFE8D8BC, true),
        bg("ink", BackgroundGroup.Solid, R.string.ui_bg_ink, 0xFF2B2D33, 0xFF2B2D33, false),
    )

    /** 이름이 있는 배경. 모르는 id나 '시간대 하늘'(null), 끈 상태는 null을 돌려줘요. */
    fun find(id: String?): AlarmBackground? = id?.let { all.firstOrNull { b -> b.id == it } }

    fun isPhoto(id: String?): Boolean = id?.startsWith(PHOTO_PREFIX) == true

    /** 내 사진 배경 파일(앱 안 저장소). 사진 배경이 아니면 null. */
    fun photoFile(context: Context, id: String?): File? =
        id?.takeIf(::isPhoto)?.removePrefix(PHOTO_PREFIX)?.let { File(photoDir(context), it) }

    fun photoDir(context: Context): File = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }

    /** 앱 안에 저장해 둔 내 사진 배경 id들(새것 먼저). */
    fun savedPhotos(context: Context): List<String> =
        photoDir(context).listFiles().orEmpty()
            .filter { it.isFile && it.extension == PHOTO_EXT }
            .sortedByDescending { it.lastModified() }
            .map { PHOTO_PREFIX + it.name }

    /**
     * 고른 사진을 긴 변 [MAX_PHOTO_SIDE] 안으로 줄여 앱 안에 jpg로 복사하고 배경 id를 돌려줘요.
     * 원본을 계속 읽을 권한이 없어도 울림 때 쓸 수 있게 복사해 둬요. 못 읽으면 null이에요.
     */
    fun importPhoto(context: Context, uri: Uri): String? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@runCatching null
        val file = File(photoDir(context), "bg_${System.currentTimeMillis()}.$PHOTO_EXT")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        PHOTO_PREFIX + file.name
    }.getOrNull()

    private const val PHOTO_EXT = "jpg"
    private const val MAX_PHOTO_SIDE = 1600
    private const val JPEG_QUALITY = 88

    /** 잠금을 풀기 전이나 파일이 지워졌으면 null이라 기본 하늘로 대신해요. */
    fun loadPhoto(context: Context, id: String?): Bitmap? = runCatching {
        photoFile(context, id)?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
    }.getOrNull()
}

/** 내 사진 배경을 뒤에서 읽어 와요. 못 읽으면 null이에요. */
@Composable
fun rememberBackgroundPhoto(id: String?): ImageBitmap? {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, id) {
        value = withContext(Dispatchers.IO) { AlarmBackgrounds.loadPhoto(context, id)?.asImageBitmap() }
    }
    return photo
}
