package com.xiamo.setting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

abstract class AbstractSetting<T : Any>(
    open val name: String,
    open val description: String,
    protected val defaultValue: T
) {
    abstract var value: T
    var visible: Boolean = true
    var dependency: (() -> Boolean)? = null

    fun isVisible(): Boolean {
        return visible && (dependency?.invoke() ?: true)
    }

    open fun reset() {
        value = defaultValue
    }

    abstract fun toJson(): JsonElement
    abstract fun fromJson(element: JsonElement)
}

class BooleanSetting(
    override val name: String,
    override val description: String,
    defaultValue: Boolean
) : AbstractSetting<Boolean>(name, description, defaultValue) {

    override var value by mutableStateOf(defaultValue)

    override fun toJson(): JsonElement = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) {
            value = element.asBoolean
        }
    }
}

class NumberSetting(
    override val name: String,
    override val description: String,
    defaultValue: Double,
    val min: Double,
    val max: Double,
    val step: Double = 0.1
) : AbstractSetting<Double>(name, description, defaultValue) {

    override var value by mutableStateOf(defaultValue.coerceIn(min, max))

    fun setValueClamped(newValue: Double) {
        value = newValue.coerceIn(min, max)
    }

    val intValue: Int get() = value.toInt()
    val floatValue: Float get() = value.toFloat()

    override fun toJson(): JsonElement = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            value = element.asDouble.coerceIn(min, max)
        }
    }
}

class ModeSetting(
    override val name: String,
    override val description: String,
    defaultValue: String,
    val modes: List<String>
) : AbstractSetting<String>(name, description, defaultValue) {

    override var value by mutableStateOf(
        if (modes.contains(defaultValue)) defaultValue else modes.firstOrNull() ?: ""
    )

    val index: Int get() = modes.indexOf(value).coerceAtLeast(0)

    fun cycle() {
        val nextIndex = (index + 1) % modes.size
        value = modes[nextIndex]
    }

    fun cycleBack() {
        val prevIndex = if (index == 0) modes.size - 1 else index - 1
        value = modes[prevIndex]
    }

    override fun toJson(): JsonElement = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            val newValue = element.asString
            if (modes.contains(newValue)) {
                value = newValue
            }
        }
    }
}

class StringSetting(
    override val name: String,
    override val description: String,
    defaultValue: String
) : AbstractSetting<String>(name, description, defaultValue) {

    override var value by mutableStateOf(defaultValue)

    override fun toJson(): JsonElement = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            value = element.asString
        }
    }
}

class ColorSetting(
    override val name: String,
    override val description: String,
    defaultValue: Int
) : AbstractSetting<Int>(name, description, defaultValue) {

    override var value by mutableStateOf(defaultValue)

    val alpha: Int get() = (value shr 24) and 0xFF
    val red: Int get() = (value shr 16) and 0xFF
    val green: Int get() = (value shr 8) and 0xFF
    val blue: Int get() = value and 0xFF

    fun setARGB(a: Int, r: Int, g: Int, b: Int) {
        value = ((a and 0xFF) shl 24) or
                ((r and 0xFF) shl 16) or
                ((g and 0xFF) shl 8) or
                (b and 0xFF)
    }

    override fun toJson(): JsonElement = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            value = element.asInt
        }
    }
}

class KeyBindSetting(
    override val name: String,
    override val description: String,
    defaultValue: Int
) : AbstractSetting<Int>(name, description, defaultValue) {

    override var value by mutableStateOf(defaultValue)
    var isListening by mutableStateOf(false)

    override fun toJson(): JsonElement = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            value = element.asInt
        }
    }
}
