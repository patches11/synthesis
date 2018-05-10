package org.synthesis.design

import org.synthesis.{Mixer, MixerChannel, Wall}

/**
  * Created by Brown on 7/13/17.
  */
case class Test(wall: Wall) extends Design {
  override def render(channel: MixerChannel): Unit = {
    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      val panel = if (x < wall.pixelsX / 2) {
        // 1 or 3
        if (y < wall.pixelsY / 2) {
          //1
          channel.setPixel(x, y, Mixer.getRGB(255, y * 10, 0, 0))
        } else {
          //3
          channel.setPixel(x, y, Mixer.getRGB(255, 0, 0, (y - 25) * 10))
        }
      } else {
        // 2 or 4
        if (y < wall.pixelsY / 2) {
          //2
          channel.setPixel(x, y, Mixer.getRGB(255, 0, y * 10, 0))
        } else {
          //4
          channel.setPixel(x, y, Mixer.getRGB(255, (y - 25) * 10, (y - 25) * 10, 0))
        }
      }


    }
  }
}
