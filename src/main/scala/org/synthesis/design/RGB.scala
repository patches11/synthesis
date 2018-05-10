package org.synthesis.design

import java.awt.Color

import org.synthesis.{MixerChannel, Wall}

/**
  * Created by Brown on 7/5/17.
  */
case class RGB(wall: Wall) extends Design {
  var index: Long = 0


  override def render(channel: MixerChannel): Unit = {
    index += 1
    index = index % 30
    for(x <- 0 until wall.pixelsX) {
      for (y <- 0 until wall.pixelsY) {
        if (index / 10 == 0) {
          channel.setPixel(x, y, new Color(255, 0, 0).getRGB)
        } else if (index / 10 == 1) {
          channel.setPixel(x, y, new Color(0, 255, 0).getRGB)
        } else {
          channel.setPixel(x, y, new Color(0, 0, 255).getRGB)
        }
      }
    }
  }
}
