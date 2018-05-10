package org.synthesis.design

import java.awt.Color

import org.synthesis.{Mixer, MixerChannel, Wall}

/**
  * Created by patrickbrown on 3/12/17.
  */
case class WipeSideways(wall: Wall) extends Design {

  private val paletteB = new Array[Int](4096)

  for(x <- 0 until 4096) {
    val red =  128.0 + (17.0*Math.sin(Math.PI * x / 139.0))
    val green = 128.0 + (113.0*Math.sin(Math.PI * x / 311.0))
    val blue = 128.0 + (97.0*Math.sin(Math.PI * x / 631.0))
    paletteB(x) = new Color(		red.toInt,
      green.toInt,
      blue.toInt).getRGB
  }

  override def render(channel: MixerChannel): Unit = {
    val paletteShift = ((System.currentTimeMillis() % 100000) / 5.0).toInt

    for(x <- 0 until wall.pixelsX) {
      for(y <- 0 until wall.pixelsY) {
        channel.setPixel(x, y, paletteB(((x * y) + paletteShift) % 4096))
      }
    }
  }
}
