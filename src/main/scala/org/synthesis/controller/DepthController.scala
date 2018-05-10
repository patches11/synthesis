package org.synthesis.controller

/**
  * Created by Brown on 7/12/17.
  */
object DepthController extends Controller {
  val fadeRatio: Double = 1.0
  val stableMillis: Long = 30000
  val continue: Boolean = false
  override val bodyExt: Boolean = true
  override val maxMillis: Long = 120000
}
