package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.GameViewport
import com.valerie.yurei.core.world.TimedChallenge
import org.junit.Assert.*
import org.junit.Test

class GameViewportTest {
    private val surfaces = listOf(Size(360f,640f), Size(720f,1280f), Size(1440f,2560f),
        Size(1080f,2200f), Size(1600f,900f))

    @Test fun arenaFitsWithoutStretchingOrCroppingAtEveryAspectRatio() {
        surfaces.forEach { surface ->
            val view = GameViewport.fit(surface)
            val end = view.toScreen(Offset(1080f,1920f))
            assertTrue(view.offset.x >= 0f && view.offset.y >= 0f)
            assertTrue(end.x <= surface.width + 0.001f && end.y <= surface.height + 0.001f)
            assertEquals(surface.width / 2, view.toScreen(Offset(540f,960f)).x, 0.001f)
            assertEquals(surface.height / 2, view.toScreen(Offset(540f,960f)).y, 0.001f)
            val horizontal = (view.toScreen(Offset(100f,0f)) - view.offset).getDistance()
            val vertical = (view.toScreen(Offset(0f,100f)) - view.offset).getDistance()
            assertEquals(horizontal, vertical, 0.001f)
        }
    }

    @Test fun identicalGesturesKeepMotionBodyAndCollisionEquivalentAcrossScreens() {
        val reference = Dragon(Offset(540f,960f), initialLength = 12)
        val referenceRun = TimedChallenge()
        val dragons = surfaces.map { Dragon(Offset(540f,960f), initialLength = 12) }
        val runs = surfaces.map { TimedChallenge() }
        repeat(600) { frame ->
            val direction = if (frame < 200) Offset(100f,0f) else if (frame < 400) Offset(0f,100f) else Offset(-100f,0f)
            referenceRun.steer(direction.x,direction.y)
            referenceRun.move(reference,GameViewport.worldSize,16)
            reference.update(16)
            surfaces.forEachIndexed { index, surface ->
                val view = GameViewport.fit(surface)
                val gesture = view.toScreen(direction) - view.toScreen(Offset.Zero)
                runs[index].steer(gesture.x,gesture.y)
                runs[index].move(dragons[index],GameViewport.worldSize,16)
                dragons[index].update(16)
                assertEquals(reference.getSegments(), dragons[index].getSegments())
                assertEquals(referenceRun.touchesTail(reference), runs[index].touchesTail(dragons[index]))
                assertEquals(reference.getHeadRadius() * view.scale,
                    (view.toScreen(Offset(reference.getHeadRadius(),0f)) - view.offset).getDistance(), 0.001f)
            }
        }
    }
}
