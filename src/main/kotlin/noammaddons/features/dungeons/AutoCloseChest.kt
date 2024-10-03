package noammaddons.features.dungeons

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.PacketEvent
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.LocationUtils.inDungeons

object AutoCloseChest {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet !is S2DPacketOpenWindow || !inDungeons) return
        if (!config.autoCloseSecretChests) return
	    if (event.packet.windowTitle.unformattedText.equalsOneOf("Chest", "Large Chest")) {
			event.isCanceled = true
		    mc.netHandler.networkManager.sendPacket(C0DPacketCloseWindow((event.packet.windowId)))
		}
    }
}
