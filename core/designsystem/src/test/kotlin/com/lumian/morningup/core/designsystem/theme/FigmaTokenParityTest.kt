package com.lumian.morningup.core.designsystem.theme

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/** 코드의 토큰 표가 피그마에서 읽어 저장한 figma-tokens.json과 똑같은지 확인해요. */
class FigmaTokenParityTest {
    private val figma: JsonObject = Json.parseToJsonElement(
        checkNotNull(javaClass.classLoader?.getResource("figma-tokens.json")).readText(),
    ).jsonObject

    private fun hex(value: Long) = "#%06X".format(value and 0xFFFFFF)

    @Test
    fun colors_matchFigma_inBothModes() {
        val json = figma.getValue("color").jsonObject
        assertThat(LightColorTokens.keys).containsExactlyElementsIn(json.keys)
        assertThat(DarkColorTokens.keys).containsExactlyElementsIn(json.keys)
        json.forEach { (name, modes) ->
            val (light, dark) = modes.jsonArray.map { it.jsonPrimitive.content }
            assertWithMessage("$name light").that(hex(LightColorTokens.getValue(name))).isEqualTo(light)
            assertWithMessage("$name dark").that(hex(DarkColorTokens.getValue(name))).isEqualTo(dark)
        }
    }

    @Test
    fun colors_areOpaque() {
        (LightColorTokens.values + DarkColorTokens.values).forEach {
            assertThat(it ushr 24).isEqualTo(0xFFL)
        }
    }

    @Test
    fun dimensions_matchFigma() {
        val json = figma.getValue("dimension").jsonObject
        assertThat(DimensionTokens).isEqualTo(json.mapValues { it.value.jsonPrimitive.int })
    }

    @Test
    fun textStyles_matchFigma() {
        val json = figma.getValue("text").jsonObject
        assertThat(TextStyleTokens.keys).containsExactlyElementsIn(json.keys)
        json.forEach { (name, v) ->
            val a = v.jsonArray
            val expected = TextStyleToken(
                weight = FontWeightToken.valueOf(a[0].jsonPrimitive.content),
                size = a[1].jsonPrimitive.int,
                lineHeight = a[2].jsonPrimitive.int,
                letterSpacingPercent = a[3].jsonPrimitive.int,
            )
            assertWithMessage(name).that(TextStyleTokens.getValue(name)).isEqualTo(expected)
        }
    }

    @Test
    fun shadows_matchFigma() {
        val json = figma.getValue("shadow").jsonObject
        assertThat(ShadowTokens.keys).containsExactlyElementsIn(json.keys)
        json.forEach { (name, v) ->
            val a = v.jsonArray
            val token = ShadowTokens.getValue(name)
            assertThat(hex(token.color)).isEqualTo(a[0].jsonPrimitive.content)
            assertWithMessage(name)
                .that(listOf(token.alphaPercent, token.offsetX, token.offsetY, token.blur, token.spread))
                .isEqualTo(a.drop(1).map { it.jsonPrimitive.int })
        }
    }
}
