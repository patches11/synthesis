package org.synthesis.palette

trait PaletteSet {
  def getAll: Set[Palette]
  def getRandom: Option[Palette]
  def getIterator: Iterator[Palette]
}
