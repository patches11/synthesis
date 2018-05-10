package org.synthesis.palette

import java.awt.Color

class PaletteB extends Palette {
  override def name: String = "PaletteB"

  for(x <- 0 until 4096) {
    val red = 128.0 + (17.0 * Math.sin(Math.PI * x / 139.0))
    val green = 128.0 + (113.0 * Math.sin(Math.PI * x / 311.0))
    val blue = 128.0 + (97.0 * Math.sin(Math.PI * x / 631.0))
    paletteB(x) = new Color(red.toInt,
      green.toInt,
      blue.toInt).getRGB
  }

  override def length: Int = paletteB.length

  private val paletteB = new Array[Int](4096)

  override def getColor(index: Int): Int = paletteB(index % length)
}
