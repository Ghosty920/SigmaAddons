package noammaddons.features.gui.Menus.impl

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiContainerEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.features.gui.Menus.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.ItemUtils.getHeadSkinTexture
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils.drawPlayerHead
import noammaddons.utils.RenderUtils.drawRainbowRoundedBorder
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils.renderItem
import noammaddons.utils.SoundUtils
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.isNull
import org.lwjgl.input.Keyboard
import kotlin.math.floor


object CustomWardrobeMenu: Feature() {
    private const val EDIT_SLOT = 50
    private val wardrobeMenuRegex = Regex("^Wardrobe \\(\\d/\\d\\)$")
    var inWardrobeMenu = false
    private val allowedSlots = mapOf(
        36 to listOf(36, 27, 18, 9, 0),
        37 to listOf(37, 28, 19, 10, 1),
        38 to listOf(38, 29, 20, 11, 2),
        39 to listOf(39, 30, 21, 12, 3),
        40 to listOf(40, 31, 22, 13, 4),
        41 to listOf(41, 32, 23, 14, 5),
        42 to listOf(42, 33, 24, 15, 6),
        43 to listOf(43, 34, 25, 16, 7),
        44 to listOf(44, 35, 26, 17, 8),
    )

    @SubscribeEvent
    fun onOpen(event: PacketEvent.Received) {
        if (config.CustomSBMenus && event.packet is S2DPacketOpenWindow) {
            inWardrobeMenu = event.packet.windowTitle.unformattedText.matches(wardrobeMenuRegex)
        }

        if (inWardrobeMenu && event.packet is S2EPacketCloseWindow) {
            inWardrobeMenu = false
        }
    }

    @SubscribeEvent
    fun onClose(event: PacketEvent.Sent) {
        if (inWardrobeMenu && event.packet is C0DPacketCloseWindow) {
            inWardrobeMenu = false
        }
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (! inWardrobeMenu) return
        if (! event.button.equalsOneOf(0, 1, 2)) return
        val container = Player?.openContainer?.inventorySlots ?: return
        event.isCanceled = true

        val scale = calculateScale()
        val (x, y) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        var (offsetX, offsetY, _, _) = calculateOffsets(screenSize, windowSize)
        // adding 1 slot gap so the title won’t overlap with the items
        offsetY += 9
        val slotPosition = calculateSlotPosition(x, y, offsetX, offsetY)

        if (! isValidSlot(slotPosition.first, slotPosition.second)) return
        val slot = getSlotIndex(slotPosition.first, slotPosition.second)

        if (slot >= windowSize) return
        container[slot].run {
            if (stack.isNull()) return
            if (stack.getItemId() == 160 && stack.metadata == 15) return
        }

        if (slot == EDIT_SLOT) {
            SoundUtils.click.start()
            inWardrobeMenu = false
        }
        else this.handleSlotClick(event.button, slot)
    }


    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inWardrobeMenu) return
        event.isCanceled = true
        val container = Player?.openContainer?.inventorySlots ?: return
        injectEditButton(container[EDIT_SLOT])

        val scale = calculateScale()
        val (mx, my) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        var (offsetX, offsetY, width, height) = calculateOffsets(screenSize, windowSize)
        // adding 1 slot gap so the title won’t overlap with the items
        height += 18
        offsetY -= 9
        val slotPosition = calculateSlotPosition(mx, my, offsetX, offsetY)

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)

        renderBackground(offsetX, offsetY, width, height, backgroundColor)
        drawText("&6&l&n[&b&l&nN&d&l&nA&6&l&n]&r &b&lWardrobe Menu", offsetX, offsetY)

        for (slot in container) {
            val i = slot !!.slotNumber
            if (i >= windowSize) continue
            if (slot.stack.isNull()) continue

            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = (floor(i / 9.0).toInt() + 1) * 18 + offsetY

            if (slot.stack.metadata == 10 && i in allowedSlots) {
                drawRainbowRoundedBorder(
                    currentOffsetX, currentOffsetY,
                    16f, 16f, 3f, 3f,
                )
            }

            if (slot.stack.item is ItemSkull) drawPlayerHead(
                getHeadSkinTexture(slot.stack) ?: continue,
                currentOffsetX + 2.2f,
                currentOffsetY + 2.2f,
                11.6f, 11.6f, 1f
            )
        }

        container.forEach { slot ->
            val i = slot !!.slotNumber
            val stack = slot.stack

            if (i >= windowSize) return@forEach
            if (stack.isNull()) return@forEach
            if (slot.stack?.item is ItemSkull) return@forEach
            if (stack?.getItemId() == 160 && stack.metadata == 15) return@forEach
            stack.tagCompound.removeTag("ench")

            renderItem(
                slot.stack,
                i % 9 * 18 + offsetX,
                (floor(i / 9.0).toInt() + 1) * 18 + offsetY
            )

        }

        GlStateManager.popMatrix()

        if (! isValidSlot(slotPosition.first, slotPosition.second - 1)) return
        val slotIndex = getSlotIndex(slotPosition.first, slotPosition.second - 1)
        if (slotIndex >= windowSize) return

        val item = container[slotIndex]?.stack ?: return
        if (item.getItemId() == 160 && item.metadata == 15) return

        updateLastSlot(slotIndex)
        drawLore(item.displayName, item.lore, mx, my, scale, screenSize)
    }

    private fun injectEditButton(slot: Slot) {
        val itemStack = ItemStack(Blocks.anvil)
        itemStack.setStackDisplayName("&l&bEdit".addColor())

        if (slot.stack == itemStack) return

        slot.putStack(itemStack)
    }

    private fun handleSlotClick(button: Int, slotIndex: Int) {
        val mainSlot = allowedSlots.values.firstOrNull { slotIndex in it }?.get(0) ?: slotIndex
        sendWindowClickPacket(
            mainSlot, button,
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) 1 else 0
        )
        SoundUtils.click.start()
    }
}