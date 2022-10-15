# Backend

The backend receive the IR and translate into assemble file.

Pipeline: IR => MCBuilder => Optimize => MCEmitter => Assembly

![pipeline of backend](../../doc/image/pipeline_of_backend.jpg)

# References

## ARM Intruction

- [The ARM Instruction Set Architecture](https://github.com/jiweixing/build-a-compiler-within-30-days/blob/master/ARM/Arm_EE382N_4.pdf)
- [ARM Architecture Reference Manual ARMv7-A edition](https://developer.arm.com/documentation/ddi0406/latest/)
- [Procedure Call Standard for the ARM Architecture](https://web.eecs.umich.edu/~prabal/teaching/resources/eecs373/ARM-AAPCS-EABI-v2.08.pdf)
- [ARM Developer Suite Assembler Guide](https://developer.arm.com/documentation/dui0068/b/)
- [ARM and Thumb-2 Instruction Set Quick Reference Card](https://developer.arm.com/documentation/qrc0001/m)
- [Vector Floating Point Instruction Set Quick Reference Card](https://developer.arm.com/documentation/qrc0007/e/)

## GNU

- [GNU Arm Embedded Toolchain Downloads](https://developer.arm.com/downloads/-/gnu-rm)
- [GNU ARM Assembler Quick Reference](https://www.yumpu.com/en/document/view/34963142/gnu-arm-assembler-quick-reference-bel)
- [Using as](http://microelectronics.esa.int/erc32/doc/as.pdf)

## Tools

- [VisUAL](https://salmanarif.bitbucket.io/visual/index.html)
- [CPUlator online ARM assembler & emulator](https://cpulator.01xz.net/?sys=arm)