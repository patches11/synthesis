package org.synthesis.controller

/**
  * Created by Brown on 7/12/17.
  */
trait Controller {
  def fadeRatio: Double
  def stableMillis: Long
  def continue: Boolean
  def bodyExt: Boolean = false
  def maxMillis: Long = stableMillis
}


