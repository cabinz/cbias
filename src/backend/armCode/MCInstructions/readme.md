# Detail about the instructions

This folder collects all representations of MachineIR. So the beginning "MC" is wrong ...<br/>
Some of them directly correspond to an ARM assemble,
while the other are aggregations of a bunch of related instructions, with the first character capital (exclude "MC").

More description:
- `MCMove` - aggregation of data transfer instructions
- `MCBinary` - aggregation of ADD, SUB, RSB, MUL, SDIV
- `MCFma` - aggregation of MLA, MLS, SMMLA, SMMLS
- `MCSmull` - SMULL
- `MCcmp` - CMP
- `MCbranch` - aggregation of B, BL
- `MCload` - LDR
- `MCstore` - STR
- `MCpop` - POP
- `MCpush` - PUSH
- `MCReturn` - aggregation of instructions at the end of a function


- `MCFPmove` - aggregation of VMOV, VMRS
- `MCFPBinary` - aggregation of VADD, VSUB, VMUL, VDIV
- `MCFPneg` - VNEG
- `MCFPconvert` - VCVT
- `MCFPcompare` - VCMP
- `MCFPload` - VLDR
- `MCFPstore` - VSTR
- `MCFPpop` - VPOP
- `MCFPpush` - VPUSH