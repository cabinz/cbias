# Detail about the instructions

This folder collects all representations of MachineIR. So the beginning "MC" is wrong ...<br/>
Some of them directly correspond to an ARM assemble,
while the other are aggregations of a bunch of related instructions, with the first character capital (exclude "MC").

More description:
- MCMove&nbsp;&nbsp; - aggregation of data transfer instructions
- MCBinary&nbsp; - aggregation of ADD, SUB, RSB, MUL, SDIV
- MCFma&nbsp;&nbsp;&nbsp;&nbsp; - aggregation of MLA, MLS, SMMLA, SMMLS
- MCSmull&nbsp;&nbsp; - SMULL
- MCcmp&nbsp;&nbsp;&nbsp;&nbsp; - CMP
- MCbranch - aggregation of B, BL
- MCload&nbsp;&nbsp;&nbsp;&nbsp; - LDR
- MCstore&nbsp;&nbsp;&nbsp; - STR
- MCpop&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - POP
- MCpush&nbsp;&nbsp;&nbsp; - PUSH
- MCReturn - aggregation of instructions at the end of a function


- MCFPmove &nbsp;&nbsp;&nbsp;&nbsp; - aggregation of VMOV, VMRS
- MCFPBinary &nbsp;&nbsp;&nbsp; - aggregation of VADD, VSUB, VMUL, VDIV
- MCFPneg &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VNEG
- MCFPconvert &nbsp; - VCVT
- MCFPcompare - VCMP
- MCFPload &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VLDR
- MCFPstore &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VSTR
- MCFPpop &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VPOP
- MCFPpush &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - VPUSH