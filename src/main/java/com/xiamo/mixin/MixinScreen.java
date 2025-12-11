package com.xiamo.mixin;


import com.xiamo.event.NavigateEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.CopyOnWriteArrayList;


@Mixin(MinecraftClient.class)
public class MixinScreen {
    @Unique
    private static final CopyOnWriteArrayList<Class<? extends Screen>> hookScreenList =
            new CopyOnWriteArrayList<>();

    static {
        hookScreenList.add(TitleScreen.class);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen != null && hookScreenList.contains(screen.getClass())) {
            ci.cancel();
            new NavigateEvent(screen).broadcast();
        }
    }

    

}
