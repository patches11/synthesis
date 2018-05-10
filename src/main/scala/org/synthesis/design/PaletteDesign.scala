package org.synthesis.design

import org.synthesis.palette.Palette

trait PaletteDesign extends Design {

  protected var internalPalette: Palette

  override def baseReceive: Receive = {
    case Render(channel) =>
      val t0 = System.nanoTime()
      iterationCount += 1
      render(channel)
      fpses += (1000000000 / (System.nanoTime() - t0))
      if (iterationCount % 120 == 0) {
        System.out.print(s"${this.getClass.getSimpleName} fps ${"%.1f".format(fpses.sum / fpses.size)}, palette: ${internalPalette.name} \r")
        fpses.clear()
      }
  }
}

