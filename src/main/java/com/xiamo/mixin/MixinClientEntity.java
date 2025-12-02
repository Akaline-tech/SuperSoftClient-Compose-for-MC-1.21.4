package com.xiamo.mixin;


import com.xiamo.utils.rotation.Rotation;
import com.xiamo.utils.rotation.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientEntity {

    @Shadow
    private float lastYaw;

    @Shadow
    private float lastPitch;



    @Shadow
    @Final
    protected MinecraftClient client;



    @Inject(method = "tick",at = @At("HEAD"))
    private void onTick(CallbackInfo ci){
//        if (RotationManager.INSTANCE.isActive().getValue() && RotationManager.INSTANCE.getTargetRotation() != null) {
//            ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
//
//            float spoofYaw = RotationManager.INSTANCE.getTargetRotation().getYaw();
//            float spoofPitch = RotationManager.INSTANCE.getTargetRotation().getPitch();
//
//            // 让 Minecraft 认为你每 tick 都转头了
//            player.prevYaw = spoofYaw - 0.0001f;
//            player.prevPitch = spoofPitch - 0.0001f;
//        }

    }

//    @ModifyVariable(method = "sendMovementPackets",at = @At("STORE"),ordinal = 3)
//    public double hookYaw(double value){
//        if (RotationManager.INSTANCE.getTargetRotation() != null){
//            return RotationManager.INSTANCE.getTargetRotation().getYaw();
//        }else {
//            return value;
//        }
//
//    }
//
//    @ModifyVariable(method = "sendMovementPackets",at = @At("STORE"),ordinal = 4)
//    public double hookPitch(double value){
//        if (RotationManager.INSTANCE.getTargetRotation() != null){
//            return RotationManager.INSTANCE.getTargetRotation().getPitch();
//        }else {
//            return value;
//        }
//
//    }

    @Redirect(method = "sendMovementPackets",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    private void sendMovementPackets(ClientPlayNetworkHandler instance, Packet<?> packet){
        if (packet instanceof PlayerMoveC2SPacket && RotationManager.INSTANCE.getTargetRotation() != null && RotationManager.INSTANCE.isActive().getValue()){
            Rotation targetRotation = RotationManager.INSTANCE.getPacketRotation();
            PlayerMoveC2SPacket movePacket = (PlayerMoveC2SPacket) packet;
            Packet<?> newPacket;
            float yaw = RotationManager.INSTANCE.getTargetRotation().getYaw();
            float pitch = RotationManager.INSTANCE.getTargetRotation().getYaw();
            if (targetRotation != null){
                yaw = targetRotation.getYaw();
                pitch = targetRotation.getPitch();
            }
            double x = movePacket.getX(this.client.player.getX());
            double y = movePacket.getY(this.client.player.getY());
            double z = movePacket.getZ(this.client.player.getZ());
            if (movePacket.changesLook()){
                newPacket = new PlayerMoveC2SPacket.LookAndOnGround(yaw,pitch, movePacket.isOnGround(),true);
            }else if (movePacket.changesLook() && movePacket.changesLook()){
                newPacket = new PlayerMoveC2SPacket.Full(x,y,z,yaw,pitch,movePacket.isOnGround(),movePacket.isOnGround());
            } else newPacket = new PlayerMoveC2SPacket.Full(x,y,z,yaw,pitch,movePacket.isOnGround(),movePacket.isOnGround());
            instance.sendPacket(newPacket);
        }else instance.sendPacket(packet);

    }
}
