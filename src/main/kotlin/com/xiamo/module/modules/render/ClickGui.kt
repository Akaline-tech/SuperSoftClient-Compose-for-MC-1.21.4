package com.xiamo.module.modules.render

import androidx.compose.ui.graphics.Color
import com.xiamo.gui.ComposeScreen
import com.xiamo.gui.clickGui.ClickGuiColors
import com.xiamo.gui.clickGui.ClickGuiScreen
import com.xiamo.module.Category
import com.xiamo.module.Module
import com.xiamo.setting.AbstractSetting
import com.xiamo.setting.ColorSetting
import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW

object ClickGui : Module("ClickGui", "ClickGui", Category.Render) {

    var instance: ComposeScreen? = null

    private val bgColorSetting = colorSetting("Background", "窗口背景色", colorToArgb(26, 26, 26))
    private val titleBgSetting = colorSetting("TitleBg", "标题背景色", colorToArgb(0, 0, 0))
    private val moduleHoverSetting = colorSetting("ModuleHover", "模块悬停色", colorToArgb(105, 180, 255))
    private val moduleEnabledSetting = colorSetting("ModuleEnabled", "模块启用色", colorToArgb(108, 53, 222))
    private val settingBgSetting = colorSetting("SettingBg", "设置背景色", colorToArgb(40, 40, 40))
    private val settingHoverSetting = colorSetting("SettingHover", "设置悬停色", colorToArgb(60, 60, 60))
    private val accentSetting = colorSetting("Accent", "强调色", colorToArgb(108, 53, 222))
    private val textSetting = colorSetting("Text", "文字颜色", colorToArgb(255, 255, 255))
    private val dropdownBgSetting = colorSetting("DropdownBg", "下拉菜单背景", colorToArgb(30, 30, 30))
    private val sliderTrackSetting = colorSetting("SliderTrack", "滑块轨道色", colorToArgb(128, 128, 128, 77))
    private val textFieldBgSetting = colorSetting("TextFieldBg", "输入框背景", colorToArgb(50, 20, 40))

    init {
        this.key = GLFW.GLFW_KEY_RIGHT_SHIFT
        this.isComposeScreen = true
        applyColors()
    }

    private fun colorToArgb(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    private fun argbToColor(setting: ColorSetting): Color {
        return Color(
            red = setting.red / 255f,
            green = setting.green / 255f,
            blue = setting.blue / 255f,
            alpha = setting.alpha / 255f
        )
    }

    private fun applyColors() {
        ClickGuiColors.backgroundColor = argbToColor(bgColorSetting)
        ClickGuiColors.titleBgColor = argbToColor(titleBgSetting)
        ClickGuiColors.moduleHoverBgColor = argbToColor(moduleHoverSetting)
        ClickGuiColors.moduleEnabledBgColor = argbToColor(moduleEnabledSetting)
        ClickGuiColors.settingBgColor = argbToColor(settingBgSetting)
        ClickGuiColors.settingHoverColor = argbToColor(settingHoverSetting)
        ClickGuiColors.accentColor = argbToColor(accentSetting)
        ClickGuiColors.textColor = argbToColor(textSetting)
        ClickGuiColors.textSecondaryColor = argbToColor(textSetting).copy(alpha = 0.9f)
        ClickGuiColors.dropdownBgColor = argbToColor(dropdownBgSetting)
        ClickGuiColors.sliderTrackColor = argbToColor(sliderTrackSetting)
        ClickGuiColors.textFieldBgColor = argbToColor(textFieldBgSetting)
    }

    override fun onSettingChanged(setting: AbstractSetting<*>) {
        super.onSettingChanged(setting)
        applyColors()
    }

    override fun disable() {
        instance?.isVisible = false
        super.disable()
    }

    override fun enable() {
        val currentScreen = MinecraftClient.getInstance().currentScreen
        if (currentScreen is ComposeScreen) {
            currentScreen.isVisible = false
        }
        instance = ClickGuiScreen(currentScreen)
        MinecraftClient.getInstance().setScreen(instance)
        super.enable()
    }
}
