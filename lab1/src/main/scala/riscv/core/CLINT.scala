// Copyright 2021 Howard Lau
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package riscv.core

import chisel3._
import chisel3.util.MuxLookup
import riscv.Parameters
import chisel3.util.Cat

/*
  Used to index external interrupt, at most 2^8 devices can be connected and handled.
*/
object InterruptStatus {
  val None = 0x0.U(8.W)
  val Timer0 = 0x1.U(8.W)
  val Ret = 0xFF.U(8.W)
}

object InterruptEntry {
  val Timer0 = 0x4.U(8.W)
}

object InterruptState {
  val Idle = 0x0.U
  val SyncAssert = 0x1.U
  val AsyncAssert = 0x2.U
  val MRET = 0x3.U
}

object CSRState {
  val Idle = 0x0.U
  val Traping = 0x1.U
  val Mret = 0x2.U
}

class CSRDirectAccessBundle extends Bundle {
  val mstatus = Input(UInt(Parameters.DataWidth))
  val mepc = Input(UInt(Parameters.DataWidth))
  val mcause = Input(UInt(Parameters.DataWidth))
  val mtvec = Input(UInt(Parameters.DataWidth))

  val mstatus_write_data= Output(UInt(Parameters.DataWidth))
  val mepc_write_data= Output(UInt(Parameters.DataWidth))
  val mcause_write_data= Output(UInt(Parameters.DataWidth))

  val direct_write_enable = Output(Bool())
}

// Core Local Interrupt Controller
class CLINT extends Module {
  val io = IO(new Bundle {
    // Interrupt signals from peripherals
    val interrupt_flag = Input(UInt(Parameters.InterruptFlagWidth)) // among InterruptStatus

    val instruction = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))

    val jump_flag = Input(Bool())
    val jump_address = Input(UInt(Parameters.AddrWidth))

    val interrupt_handler_address = Output(UInt(Parameters.AddrWidth))
    val interrupt_assert = Output(Bool())

    val csr_bundle = new CSRDirectAccessBundle
  })
  val interrupt_enable = io.csr_bundle.mstatus(3)
  val instruction_address = Mux(
    io.jump_flag,
    io.jump_address,
    io.instruction_address + 4.U,
  )
  //lab2(CLINTCSR)

  val mie_enabled_mstatus = Cat(io.csr_bundle.mstatus(31, 4), 1.U(1.W), io.csr_bundle.mstatus(2, 0))
  val mie_disabled_mstatus = Cat(io.csr_bundle.mstatus(31, 4), 0.U(1.W), io.csr_bundle.mstatus(2, 0))

  when(io.interrupt_flag =/= InterruptStatus.None && interrupt_enable) {
    // when any interruption occurs and int. enabled

    io.csr_bundle.mstatus_write_data := mie_disabled_mstatus  // do not respond to int. during int.
    io.csr_bundle.mepc_write_data := instruction_address

    io.csr_bundle.mcause_write_data := 1.U(1.W) ## MuxLookup(io.interrupt_flag, 11.U(31.W), // default to external
      IndexedSeq(
        InterruptStatus.Timer0 -> 7.U(31.W),  // machine timer int.
        InterruptStatus.Ret -> 3.U(31.W), // machine software int.
    ))
    io.csr_bundle.direct_write_enable := true.B
    io.interrupt_assert := true.B
    io.interrupt_handler_address := io.csr_bundle.mtvec // jump to interrupt handler
  }.elsewhen(io.instruction === InstructionsRet.mret) {

    io.csr_bundle.mstatus_write_data := mie_enabled_mstatus // enable int.
    io.csr_bundle.mepc_write_data := io.csr_bundle.mepc // unchanged
    io.csr_bundle.mcause_write_data := io.csr_bundle.mcause // unchanged
    io.csr_bundle.direct_write_enable := true.B
    io.interrupt_assert := true.B // return from trap
    io.interrupt_handler_address := io.csr_bundle.mtvec
  }.otherwise {
    // int. disabled (during int.) or none int. 

    io.csr_bundle.mstatus_write_data := io.csr_bundle.mstatus
    io.csr_bundle.mepc_write_data := io.csr_bundle.mepc
    io.csr_bundle.mcause_write_data := io.csr_bundle.mcause
    io.csr_bundle.direct_write_enable := false.B
    io.interrupt_assert := false.B
    io.interrupt_handler_address := instruction_address
  }

}
