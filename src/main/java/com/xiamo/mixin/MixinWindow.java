package com.xiamo.mixin;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinWindow {
    @Inject(method = "resize",at = @At("HEAD"))
    private void resize(MinecraftClient client, int width, int height, CallbackInfo ci){
        System.out.println("resize");
    }
}
