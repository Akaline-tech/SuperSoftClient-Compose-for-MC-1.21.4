package com.xiamo.module.modules.player

import com.xiamo.module.Category
import com.xiamo.module.Module
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.delay
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.SlotActionType
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

object ChestStealer : Module("ChestStealer","", Category.Player) {

    val isSilence  = booleanSetting("isSilence","isSilence",true)
    var isStealing = false

    override fun onTick() {
        if (!isStealing){
            if (MinecraftClient.getInstance().currentScreen is HandledScreen<*>) {
                val screen = MinecraftClient.getInstance().currentScreen as HandledScreen<*>
                if (MinecraftClient.getInstance().currentScreen !is InventoryScreen && screen !is CreativeInventoryScreen) {
                    if (screen.screenHandler.type == ScreenHandlerType.GENERIC_9X6 || screen.screenHandler.type == ScreenHandlerType.GENERIC_9X3){
                        stealer()
                    }


                }
            }

        }



        super.onTick()
    }



    fun stealer(){
        isStealing = true
        if (MinecraftClient.getInstance().currentScreen is HandledScreen<*>) {
            val screen = MinecraftClient.getInstance().currentScreen as HandledScreen<*>
            if (MinecraftClient.getInstance().currentScreen !is InventoryScreen && screen !is CreativeInventoryScreen) {
                if (screen.screenHandler.type == ScreenHandlerType.GENERIC_9X6 || screen.screenHandler.type == ScreenHandlerType.GENERIC_9X3){
                    val slot = screen.screenHandler.slots
                    val player = MinecraftClient.getInstance().player
                    thread {
                        slot.filter {
                        it.inventory != player?.inventory
                    }.forEach { s ->
                        if (s.stack != ItemStack.EMPTY) {
                            clickSlot(screen.screenHandler.syncId,s.index,0,SlotActionType.QUICK_MOVE)
                            Thread.sleep(100L)
                            //screen.screenHandler.onSlotClick(s.index,0, SlotActionType.PICKUP,player)

                        }
                            isStealing = false

                    }
                    }.start()
                }


            }
        }

    }


    fun clickSlot(screenId : Int,slotId : Int,button : Int,action : SlotActionType){
        val screenHandler = MinecraftClient.getInstance().currentScreen as HandledScreen<*>
        if (screenId != screenHandler.screenHandler.syncId) return
        val defaultedList = screenHandler.screenHandler.slots
        val defaultedSize= defaultedList.size
        val list = CopyOnWriteArrayList<ItemStack>()
        defaultedList.forEach {
            list.add(it.stack)
        }
        val int2ObjectMap: Int2ObjectMap<ItemStack?> = Int2ObjectOpenHashMap<ItemStack?>()

        for (j in 0..<defaultedSize) {
            val itemStack = list[j]
            val itemStack2 = defaultedList[j].stack
            if (!ItemStack.areEqual(itemStack, itemStack2)) {
                int2ObjectMap.put(j, itemStack2)
            }
        }

        val networkHandler = MinecraftClient.getInstance().player!!.networkHandler
        networkHandler.sendPacket(
            ClickSlotC2SPacket(
                screenId,screenHandler.screenHandler.revision,slotId,button,action, screenHandler.screenHandler.getSlot(slotId).stack,int2ObjectMap
            )
        )







    }







}