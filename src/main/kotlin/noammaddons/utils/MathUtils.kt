package noammaddons.utils

import net.minecraft.util.*
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.PlayerUtils.getEyePos
import java.awt.Color
import kotlin.math.*

object MathUtils {
    data class Rotation(val yaw: Float, val pitch: Float)

    /**
     * Checks if a given coordinate is inside a specified 3D box.
     * @param coord The coordinate to check.
     * @param corner1 The coordinates of one corner of the box.
     * @param corner2 The coordinates of the opposite corner of the box.
     * @return True if the coordinate is inside the box, false otherwise.
     */
    fun isCoordinateInsideBox(coord: Vec3, corner1: Vec3, corner2: Vec3): Boolean {
        val min = Vec3(
            corner1.xCoord.coerceAtMost(corner2.xCoord),
            corner1.yCoord.coerceAtMost(corner2.yCoord),
            corner1.zCoord.coerceAtMost(corner2.zCoord)
        )

        val max = Vec3(
            corner1.xCoord.coerceAtLeast(corner2.xCoord),
            corner1.yCoord.coerceAtLeast(corner2.yCoord),
            corner1.zCoord.coerceAtLeast(corner2.zCoord)
        )

        return coord.xCoord >= min.xCoord && coord.xCoord <= max.xCoord &&
                coord.yCoord >= min.yCoord && coord.yCoord <= max.yCoord &&
                coord.zCoord >= min.zCoord && coord.zCoord <= max.zCoord
    }


    /**
     * Calculates the distance between two points in a 3D space using Vec3.
     * @param vec1 The first point as a Vec3.
     * @param vec2 The second point as a Vec3.
     * @return The distance between the two points.
     */
    fun distance3D(vec1: Vec3, vec2: Vec3) = vec1.distanceTo(vec2)
    fun distance3D(pos1: BlockPos, pos2: BlockPos): Double {
        val delta = pos1.subtract(pos2).toVec()
        return sqrt(delta.xCoord.pow(2) + delta.yCoord.pow(2) + delta.zCoord.pow(2))
    }


    /**
     * Calculates the distance between two points in a 2D space (ignoring the Y coordinate) using Vec3.
     * @param vec1 The first point as a Vec3.
     * @param vec2 The second point as a Vec3.
     * @return The distance between the two points in 2D space.
     */
    fun distance2D(vec1: Vec3, vec2: Vec3): Double {
        val deltaX = vec1.xCoord - vec2.xCoord
        val deltaZ = vec1.zCoord - vec2.zCoord
        return sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }

    fun distance2D(pos1: BlockPos, pos2: BlockPos): Double {
        val deltaX = pos1.x - pos2.x
        val deltaZ = pos1.z - pos2.z
        return sqrt((deltaX * deltaX + deltaZ * deltaZ).toDouble())
    }

    fun normalizeYaw(yaw: Float): Float {
        var result = yaw
        while (result >= 180) result -= 360
        while (result < - 180) result += 360
        return result
    }

    fun normalizePitch(pitch: Float): Float {
        var result = pitch
        while (result >= 90) result -= 180
        while (result < - 90) result += 180
        return result
    }

    fun calcYawPitch(blockPos: Vec3, playerPos: Vec3 = getEyePos()): Rotation {
        val delta = blockPos.subtract(playerPos)
        val yaw = - atan2(delta.xCoord, delta.zCoord) * (180 / PI)
        val pitch = - atan2(delta.yCoord, sqrt(delta.xCoord * delta.xCoord + delta.zCoord * delta.zCoord)) * (180 / PI)
        return Rotation(yaw.toFloat(), pitch.toFloat())
    }


    @JvmStatic
    fun interpolate(prev: Number, newPos: Number, partialTicks: Number): Double {
        return prev.toDouble() + (newPos.toDouble() - prev.toDouble()) * partialTicks.toDouble()
    }

    fun interpolateColor(color1: Color, color2: Color, value: Float): Color {
        return Color(
            interpolate(color1.red, color2.red, value).toInt(),
            interpolate(color1.green, color2.green, value).toInt(),
            interpolate(color1.blue, color2.blue, value).toInt()
        )
    }

    fun interpolateYaw(startYaw: Float, targetYaw: Float, progress: Float): Float {
        var delta = (targetYaw - startYaw) % 360

        if (delta > 180) delta -= 360
        if (delta < - 180) delta += 360

        return (startYaw + delta * progress)
    }

    fun interpolatePitch(startPitch: Float, targetPitch: Float, progress: Float): Float {
        var delta = (targetPitch - startPitch)

        // Clamp the delta within the valid pitch range (-90 to 90)
        if (delta > 90) delta = 90f
        if (delta < - 90) delta = - 90f

        return (startPitch + delta * progress)
    }


    fun Vec3.floor() = Vec3(floor(xCoord), floor(yCoord), floor(zCoord))
    fun Vec3.add(x: Number = 0.0, y: Number = 0.0, z: Number = 0.0) = add(Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
    fun Vec3i.destructured() = listOf(x, y, z)
    fun Vec3.destructured() = listOf(xCoord, yCoord, zCoord)

}