package org.synthesis.design

import org.synthesis.design.support.SetPalette
import org.synthesis.palette.Palette
import org.synthesis.{MixerChannel, Wall}

/**
  * Created by Brown on 7/7/17.
  */
case class RainbowCircle(wall: Wall, defaultPalette: Palette) extends PaletteDesign {
  import org.synthesis.Utils.distance

  private val centerX = wall.pixelsX / 2
  private val centerY = wall.pixelsY / 2

  private val speedA = 100
  private val speedBAdjust = 2

  protected var internalPalette = defaultPalette

  override def support: Receive = {
    case SetPalette(p) => internalPalette = p
  }

  override def render(channel: MixerChannel): Unit = {
    val paletteShift = ((System.currentTimeMillis() % 100000) / 5.0).toInt

    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
        val index =paletteIndex(x, y, paletteShift)

        channel.setPixel(x, y,
          internalPalette.getColor(index)
        )
    }
  }

  def paletteIndex(x: Int, y: Int, paletteShift: Int): Int = {
    (
      distance(x, y, centerX, centerY) * speedA +
      paletteShift * speedB(paletteShift.toDouble / 1000) +
      10000
    ).toInt
  }

  def speedB(a: Double): Double = {
    Math.sin(a) / speedBAdjust
  }
}
