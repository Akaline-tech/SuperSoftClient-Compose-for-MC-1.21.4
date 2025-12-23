package com.xiamo.mixin;


import com.xiamo.event.MouseClickedEvent;
import com.xiamo.event.MouseReleasedEvent;
import com.xiamo.utils.bridge.MouseBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse{


    @Shadow private double x;

    @Shadow private double y;



    @Inject(method = "onMouseButton",at = @At("HEAD"))
    private void onClick(long window, int button, int action, int mods, CallbackInfo ci){
        switch (action){
            case GLFW.GLFW_PRESS -> {
                new MouseClickedEvent((int)x,(int)y, button).broadcast();
            }
            case GLFW.GLFW_RELEASE -> {
                new MouseReleasedEvent((int)x,(int)y, button).broadcast();
            }
        }
    }

}
