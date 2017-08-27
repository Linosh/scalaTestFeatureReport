package com.dy.feature.report

import org.scalatest.FunSpecLike

class FunSpecTest extends FunSpecLike {
  describe("Fun Spec Test") {
    describe("Dive into more level") {
      it("should be successful") {
        //nothing
      }

      it("should fail") {
        throw new Exception("NotThatSimpleTest failed :(")
      }
    }
  }
}
