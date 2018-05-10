package org.synthesis.palette

import java.awt.Color

object RainbowPalette extends Palette {
  override def name: String = "Rainbow"

  override def length: Int = 10000

  private val palette = (0 until length).map(i => Color.HSBtoRGB(i.toFloat / length, 1f, 1f))

  override def getColor(index: Int): Int = palette(index % length)
}