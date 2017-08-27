package com.dy.feature.report

import org.scalatest.FlatSpec

class FlatSpecTest extends FlatSpec {
  behavior of "Flat Spec Test"

  it should "be successful" in {
    //nothing
  }

  it should "fail" in {
    throw new Exception("flat spec test failed :(")
  }
}
