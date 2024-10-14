package noammaddons.events

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.model.ModelBase
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.network.Packet
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event


open class GuiContainerEvent(val container: Container, val gui: GuiContainer) : Event() {
    @Cancelable
    class DrawSlotEvent(container: Container, gui: GuiContainer, var slot: Slot) :
        GuiContainerEvent(container, gui)

    @Cancelable
    class SlotClickEvent(container: Container, gui: GuiContainer, var slot: Slot?, var slotId: Int) :
        GuiContainerEvent(container, gui)

    class CloseEvent(container: Container, gui: GuiContainer) : GuiContainerEvent(container, gui)

    @Cancelable
    class GuiMouseClickEvent(val mouseX: Int, val mouseY: Int, val button: Int, val guiScreen: GuiScreen) : Event()
}

open class ClickEvent : Event() {
	@Cancelable
	class LeftClickEvent : ClickEvent()
	
	@Cancelable
	class RightClickEvent : ClickEvent()
}


open class PacketEvent : Event() {
    @Cancelable
    class Received(val packet: Packet<*>) : PacketEvent()

    @Cancelable
    class Sent(val packet: Packet<*>) : PacketEvent()
}


@Cancelable
open class RenderLivingEntityEvent(
	var entity: EntityLivingBase,
	var p_77036_2_: Float,
	var p_77036_3_: Float,
	var p_77036_4_: Float,
	var p_77036_5_: Float,
	var p_77036_6_: Float,
	var scaleFactor: Float,
	var modelBase: ModelBase
) : Event() {
	class Post(
		entity: EntityLivingBase,
		p_77036_2_: Float,
		p_77036_3_: Float,
		p_77036_4_: Float,
		p_77036_5_: Float,
		p_77036_6_: Float,
		scaleFactor: Float,
		modelBase: ModelBase
	) : RenderLivingEntityEvent(entity, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor, modelBase)
}



@Cancelable
open class RenderTitleEvent : Event()


@Cancelable
class MessageSentEvent(var message: String) : Event()


@Cancelable
class RenderScoreBoardEvent(val objective: ScoreObjective, val scaledRes: ScaledResolution) : Event()

/*
@Cancelable
class BlockChangeEvent(val pos: BlockPos, val BlockState: IBlockState, val state: IBlockState, val worldObj: World) : Event()
*/
/*
open class EntityWorldEvent : Event() {
	@Cancelable
	class Join(val entityID: Int, val entityToSpawn: Entity) : EntityWorldEvent()
	
	
	@Cancelable
    class Leave(val entityID: Int) : EntityWorldEvent()
}
*/

class RenderOverlay: Event()


@Cancelable
class Chat(var component: IChatComponent): Event()


@Cancelable
class Actionbar(val component: IChatComponent): Event()


class InventoryFullyOpenedEvent(val inventory: RegisterEvents.Inventory): Event()

class ServerTick: Event()

class Tick: Event()

/**
 * This event is mixed in to fire before the vanilla key binds are evaluated.
 * The forge event fires after those.
 */
class PreMouseInputEvent(val button: Int): Event()

/**
 * This event is mixed in to fire before the vanilla key binds are evaluated.
 * The forge event fires after those.
 */
class PreKeyInputEvent(val key: Int, val character: Char) : Event()

@Cancelable
class renderPlayerlist(val width: Int, val scoreObjectiveIn: ScoreObjective?) : Event()