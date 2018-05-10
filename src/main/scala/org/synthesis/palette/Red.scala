package org.synthesis.palette

import org.synthesis.Mixer

object Red extends Palette {
  override def name: String = "Red"

  override def length: Int = 255

  private val palette = (0 until length).map(i => Mixer.getRGB(255, i, 0, 0))

  override def getColor(index: Int): Int = palette(index % length)
}
