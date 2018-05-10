package org.synthesis.controller

/**
  * Created by Brown on 7/12/17.
  */
object PaintingController extends Controller {
  val fadeRatio: Double = 0.00005
  val stableMillis: Long = 20000
  val continue: Boolean = false
  override val bodyExt: Boolean = true
  override val maxMillis: Long = 120000
}
