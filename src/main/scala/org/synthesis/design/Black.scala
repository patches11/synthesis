package org.synthesis.design

import org.synthesis.{MixerChannel, Wall}

case class Black(wall: Wall) extends Design {
  override def render(channel: MixerChannel): Unit = {
    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      channel.setPixel(x, y, 0)
    }
  }
}
