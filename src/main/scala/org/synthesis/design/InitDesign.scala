package org.synthesis.design

import org.synthesis.{MixerChannel, Wall}

/**
  * Created by Patrick Brown on 3/11/17
  */

case class InitDesign(wall: Wall) extends Design {
  override def render(channel: MixerChannel): Unit = {
    for (x <- 0 until wall.pixelsX) {
      for (y <- 0 until wall.pixelsY) {
        channel.setPixel(x, y, (x + 21) * (y + 31) * 3511 + 100)
      }
    }
  }
}
