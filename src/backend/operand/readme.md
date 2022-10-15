# Description about classes design

This folder collects the operands of instructions.<br/>
All class extend from MCOperand for unified interface. 
- `Label`: the label in the code
- `Register`: the interface of core registers in arm
  - `VirtualRegister`: virtual one used for regAlloc
  - `RealRegister`: physical registers, r0-r15
- `ExtensionRegister`: the interface of extension registers in arm
  - `VirtualExtRegister`: virtual one used for regAlloc
  - `RealExtRegister`: physical registers, s0-s31
- `Immediate`: integer immediate
- `FPImmediate`: float value can be encode in VMOV

# Flaw

The design now looks wonderful: split all operands into atomic basic class
and use the unified interface.<br/>

In fact, some operands looks same but their usages and limits are totally different.

> In case of Immediate, when used in binary calculate, it has **4 bits as rotate and 8 bit as immediate**,
while used as offset in load or store, it has **12 bits as immediate**, ranging from 0 to 4095.

The class design now can NOT perfectly indicate the difference between the operands with the same name.

A better way is to specify this difference and then use the constructors of MCInstructions to restrict the operands.