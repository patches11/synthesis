package org.synthesis.palette

import java.io.File

import scala.collection.mutable
import scala.util.{Random, Try}

object AllPalettes extends PaletteSet {
  private val pathA = getClass.getResource("/empty").getPath
  private val path = pathA.dropRight(5) + "palettes"
  private val folder = new File(path)
  private val filePalettes = folder.listFiles().map(FilePalette)
  private val random = new Random(System.nanoTime())
  private val palettes: Set[Palette] = Set(RainbowPalette) ++ filePalettes.toSet

  private val getList = palettes.toList

  private val randomQueue = mutable.Queue[Palette]()

  override def getAll: Set[Palette] = palettes

  override def getRandom: Option[Palette] = Try {
    if (randomQueue.isEmpty) {
      randomQueue.enqueue(Random.shuffle(getList):_*)
    }
    randomQueue.dequeue()
  }.toOption

  override def getIterator: Iterator[Palette] = palettes.toIterator
}
