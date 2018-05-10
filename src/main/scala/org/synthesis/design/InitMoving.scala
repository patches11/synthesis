package org.synthesis.design

import org.synthesis.{Mixer, MixerChannel, Wall}

/**
  * Created by patrickbrown on 3/12/17.
  */
case class InitMoving(wall: Wall) extends Design {

  var iterations = 1

  override def render(channel: MixerChannel): Unit = {
    for (x <- 0 until wall.pixelsX) {
      for (y <- 0 until wall.pixelsY) {
        //buffer.setPixel(x, y, (x + 1) * (y + 2) * (System.currentTimeMillis() / 50 % 10000).toInt)
        channel.setPixel(x, y, (x + 1) * (y + 2) * iterations)
      }
    }
    iterations += 1
  }
}
