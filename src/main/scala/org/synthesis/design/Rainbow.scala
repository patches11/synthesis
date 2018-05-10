package org.synthesis.design

import java.awt.Color

import org.synthesis.design.support.SetPalette
import org.synthesis.palette.Palette
import org.synthesis.{Mixer, MixerChannel, Wall}

/**
  * Created by patrickbrown on 3/12/17.
  */
case class Rainbow(wall: Wall, defaultPalette: Palette) extends PaletteDesign {

  protected var internalPalette = defaultPalette

  override def support: Receive = {
    case SetPalette(p) => internalPalette = p
  }

  override def render(channel: MixerChannel): Unit = {
    val paletteShift = ((System.currentTimeMillis() % 10000)).toInt

    for(x <- 0 until wall.pixelsX) {
      for(y <- 0 until wall.pixelsY) {
        channel.setPixel(x, y, internalPalette.getColor(paletteIndex(x, y, paletteShift)))
      }
    }
  }

  def paletteIndex(x: Int, y: Int, paletteShift: Int): Int = {
    (((x * y) * 5 + paletteShift)).toInt
  }
}
