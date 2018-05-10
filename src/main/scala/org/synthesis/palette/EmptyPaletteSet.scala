package org.synthesis.palette

object EmptyPaletteSet extends PaletteSet {
  override def getAll: Set[Palette] = Set.empty

  override def getRandom: Option[Palette] = None

  override def getIterator: Iterator[Palette] = Iterator.empty
}
