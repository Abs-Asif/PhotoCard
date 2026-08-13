package me.ash.reader.ui.page.home.feeds.photocard

import kotlinx.serialization.Serializable

@Serializable
data class PhotocardConfig(
    val source: SourceConfig = SourceConfig(),
    val design: DesignConfig = DesignConfig()
)

@Serializable
data class SourceConfig(
    val type: String = "rss", // "rss" or "sitemap"
    val url: String = ""
)

@Serializable
data class DesignConfig(
    val background: BackgroundConfig = BackgroundConfig(),
    val image: ImageConfig = ImageConfig(),
    val title: TextConfig = TextConfig(y = 600f, color = "#FFFFFF"),
    val date: DateConfig = DateConfig(y = 800f, color = "#CCCCCC"),
    val border: BorderConfig = BorderConfig()
)

@Serializable
data class BackgroundConfig(
    val width: Int = 1000,
    val height: Int = 1000
)

@Serializable
data class ImageConfig(
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Float = 800f,
    val height: Float = 450f,
    val cornerRadius: Float = 16f,
    val zIndex: String = "above_background" // "above_background" or "below_background"
)

@Serializable
data class TextConfig(
    val x: Float = 100f,
    val y: Float = 600f,
    val width: Float = 800f,
    val color: String = "#FFFFFF",
    val fontSize: Float = 36f,
    val fontFamily: String = "",
    val alignment: String = "left" // "left", "right", "center", "justify"
)

@Serializable
data class DateConfig(
    val x: Float = 100f,
    val y: Float = 800f,
    val color: String = "#CCCCCC",
    val fontSize: Float = 24f,
    val fontFamily: String = "",
    val format: String = "Sunday, 12 July 2026"
)

@Serializable
data class BorderConfig(
    val thickness: Float = 0f,
    val color: String = "#000000"
)
