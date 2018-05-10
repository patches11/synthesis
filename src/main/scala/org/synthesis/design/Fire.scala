package org.synthesis.design

import java.awt.Color

import org.synthesis.{MixerChannel, Wall}

import scala.util.Random

/**
  * Created by patrickbrown on 3/13/17.
  */
// TODO: Fix for size increases, works on 25x25 for now but probably won't work well at 50x50
case class Fire(wall: Wall) extends Design {
  private val fire = Array.ofDim[Int](wall.pixelsX, wall.pixelsY)
  private val sparks = Array.ofDim[Boolean](wall.pixelsX)
  private val palette = Array.ofDim[Int](256)
  private val random = new Random(System.nanoTime())

  for(x <- 0 until wall.pixelsX) {
    for (y <- 0 until wall.pixelsY) {
      fire(x)(y) = 0
    }
  }

  for(x <- 0 until wall.pixelsX) {
    sparks(x) = false
  }

  palette.indices.foreach { i =>
    palette(i) =  Color.HSBtoRGB((((i - 40) / 4) % 256) / 255f, 1f, Math.min(1f, i * 2 / 255f))
  }

  override def render(channel: MixerChannel): Unit = {
    0 until wall.pixelsX foreach { x =>
      if (sparks(x)) {
        fire(x)(wall.pixelsY - 1) = Math.min(fire(x)(wall.pixelsY - 1) + random.nextInt(12), 255)
        if (fire(x)(wall.pixelsY - 1) >= 255) {
          sparks(x) = false
        } else {
          sparks(x) = random.nextDouble < 0.95
        }
      } else {
        fire(x)(wall.pixelsY - 1) = Math.max(fire(x)(wall.pixelsY - 1)  - random.nextInt(12), 0)
        sparks(x) = random.nextDouble > 0.95
      }
    }

    for(x <- 0 until wall.pixelsX) {
      for (y <- 0 until wall.pixelsY - 1) {
        fire(x)(y) = ((fire((x - 1 + wall.pixelsX) % wall.pixelsX)((y + 1) % wall.pixelsY)
        + fire(x % wall.pixelsX)((y + 1) % wall.pixelsY)
        + fire((x + 1) % wall.pixelsX)((y + 1) % wall.pixelsY)
        + fire(x % wall.pixelsX)((y + 2) % wall.pixelsY))
        * 32) / 149
      }
    }

    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
          channel.setPixel(x, y, palette(fire(x)(y)))
    }
  }
}
