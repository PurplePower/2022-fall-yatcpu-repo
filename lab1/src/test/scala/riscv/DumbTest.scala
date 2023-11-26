package riscv



import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


class DumpTest extends  AnyFlatSpec with ChiselScalatestTester {

    class DumbChip extends Module {
        val io = IO(new Bundle {
            val useless_input = Input(UInt(32.W))
            val output = Output(UInt(32.W))
        })

        val pc = RegInit(0x8008L.U(32.W))  // with initial value 8008

        pc := pc + 4.U


        io.output := pc     // NOTE: this statement goes AFTER pc + 4

    }


    behavior of "DummyChip"
    it should "output different values for pc and output" in {
        test(new DumbChip).withAnnotations(TestAnnotations.annos) { c =>
            for (i <- 1 to 100) {
                c.clock.step()
                c.io.useless_input.poke(1.U)
            }
        
        }
    }


}


