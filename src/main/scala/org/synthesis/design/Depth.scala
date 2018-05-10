package org.synthesis.design

import akka.actor.{Actor, Props}
import org.synthesis.design.support.SetPalette
import org.synthesis.kinect.DepthFrame
import org.synthesis.{MixerChannel, Wall}
import org.synthesis.palette.Palette

case class Depth(wall: Wall, initialPalette: Palette) extends PaletteDesign {
  override protected var internalPalette: Palette = initialPalette

  private val canvas = Array.ofDim[Int](wall.pixelsX, wall.pixelsY)

  context.system.actorOf(Props(new DepthListener()))

  for (x <- 0 until wall.pixelsX;
       y <- 0 until wall.pixelsY) yield {
    canvas(x)(y) = 0
  }

  override def support: Receive = {
    case SetPalette(p) => internalPalette = p
  }

  private class DepthListener extends Actor {
    context.system.eventStream.subscribe(self, classOf[DepthFrame])

    def receive: Receive = {
      case DepthFrame(frame, width, height) =>
        val min = Math.min(width, height) - 40
        val gapX = (width - min) / 2
        val gapY = (height - min) / 2
        val ratio = min / Math.min(wall.pixelsX, wall.pixelsY)

        for(x <- 0 until wall.pixelsX;
            y <- 0 until wall.pixelsX) yield {

          val depth = frame((x * ratio + gapX) + (y * ratio + gapY) * width)

          if (depth <= 0) {
            canvas(wall.pixelsX - 1 - x)(y) = 0
          } else {
            val depthMod = if (depth > 10000) 1.0 else depth.toDouble / 10000

            canvas(wall.pixelsX - 1 - x)(y) = internalPalette.getColor(((1 - depthMod) * internalPalette.length).toInt)
          }
        }
    }
  }

  override def render(channel: MixerChannel): Unit = {
    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      channel.setPixel(x, y, canvas(x)(y))
    }
  }


}
