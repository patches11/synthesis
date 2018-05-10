package org.synthesis.design

import java.awt.Color

import org.synthesis.{Mixer, MixerChannel, Wall}

/**
  * Created by patrickbrown on 3/12/17.
  */
case class Plasma(wall: Wall) extends Design {
  private val paletteB = new Array[Int](4096)
  private val localBuffer = Array.ofDim[Int](wall.pixelsX,wall.pixelsY)

  for(x <- 0 until 4096) {
    val red =  128.0 + (17.0*Math.sin(Math.PI * x / 139.0))
    val green = 128.0 + (113.0*Math.sin(Math.PI * x / 311.0))
    val blue = 128.0 + (97.0*Math.sin(Math.PI * x / 631.0))
    paletteB(x) = new Color(		red.toInt,
      green.toInt,
      blue.toInt).getRGB
  }

  for(x <- 0 until wall.pixelsX) {
    for(y <- 0 until wall.pixelsY) {
      localBuffer(x)(y) = ((
        128.0 + (128.0 * Math.sin(x / 8.0))
          + 128.0 + (128.0 * Math.sin(y / 3.0))
          + 128.0 + (128.0 * Math.sin((x + y) / 8.0))
          + 128.0 + (128.0 * Math.sin(Math.sqrt(x * x + y * y) / 3.0))
        ) / 2).toInt
    }
  }

  override def render(channel: MixerChannel): Unit = {
    val paletteShift = ((System.currentTimeMillis() % 100000) / 5.0).toInt

    for(x <- 0 until wall.pixelsX) {
      for(y <- 0 until wall.pixelsY) {
        channel.setPixel(x, y, paletteB((localBuffer(x)(y) + paletteShift) % 4096))
      }
    }
  }
}
