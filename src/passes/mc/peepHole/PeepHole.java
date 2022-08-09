package passes.mc.peepHole;

import backend.ARMAssemble;
import backend.PrintInfo;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.MCBinary;
import backend.armCode.MCInstructions.MCMove;
import backend.armCode.MCInstructions.MCload;
import backend.armCode.MCInstructions.MCstore;
import backend.operand.Immediate;
import passes.mc.MCPass;

public class PeepHole implements MCPass {

    @Override
    public void runOnModule(ARMAssemble armAssemble) {
        for (var func : armAssemble) {
            for (var block : func) {
                MCInstruction pre;
                MCInstruction cur;
                MCInstruction nex;
                var instList = block.getInstructionList();
                for (int i=0; i<instList.size()-1; i++) {
                    if (i == 0) {
                        pre = null;
                        cur = instList.getFirst();
                        nex = instList.get(1);
                    }
                    else {
                        pre = instList.get(i-1);
                        cur = instList.get(i);
                        nex = instList.get(i+1);
                    }

                    switch (cur.getType()){
                        case ADD, SUB -> {
                            var bi = ((MCBinary) cur);

                            /*
                             *  ADD/SUB x, x, 0  <-  cur
                             *  =>
                             *  (Removed)
                             */
                            if (bi.getDestination() == bi.getOperand1()
                                    && bi.getOperand2().isImmediate()
                                    && ((Immediate) bi.getOperand2()).getIntValue() == 0) {
                                if (PrintInfo.printPeepHole)
                                    System.out.println("remove at " + block.emit() + ": " + bi.emit());
                                bi.removeSelf();
                                i--;
                            }
                        }
                        case MOV -> {
                            MCMove move = ((MCMove) cur);

                            /*
                             *  MOV x, x  <-  cur
                             *  =>
                             *  (Removed)
                             */
                            if (move.getDst() == move.getSrc() && move.getShift() == null) {
                                if (PrintInfo.printPeepHole)
                                    System.out.println("remove at " + block.emit() + ": " + move.emit());
                                cur.removeSelf();
                                i--;
                            }
                            else if (pre instanceof MCMove) {
                                var prev = ((MCMove) pre);
                                /*
                                 *  MOV x, y
                                 *  MOV x, z  <-  cur
                                 *  =>
                                 *  MOV x, z  <-  cur
                                 */
                                if (prev.getDst() == move.getDst()
                                        && prev.getSrc() != move.getSrc()
                                        && move.getSrc() != move.getDst()
                                        && move.getCond() == null) {
                                    if (PrintInfo.printPeepHole)
                                        System.out.println("remove at " + block.emit() + ": " + prev.emit());
                                    pre.removeSelf();
                                    i--;
                                }
                                /*
                                 *  MOV x, y
                                 *  MOV y, x  <-  cur
                                 *  =>
                                 *  MOV x, y
                                 */
                                else if (prev.getDst() == move.getSrc()
                                        && prev.getSrc() == move.getDst()
                                        && prev.getShift() == null
                                        && prev.getCond() == null
                                        && move.getShift() == null) {
                                    if (PrintInfo.printPeepHole)
                                        System.out.println("remove at " + block.emit() + ": " + move.emit());
                                    move.removeSelf();
                                    i--;
                                }
                            }
                        }
                        case LOAD -> {
                            var load = ((MCload) cur);
                            /*
                             *  STR x, [address, offset]
                             *  LDR y, [address, offset]  <-  cur
                             *  =>
                             *  STR x, [address, offset]
                             *  MOV y, x  <-  cur
                             */
                            if (pre instanceof MCstore) {
                                var store = ((MCstore) pre);
                                if (load.getAddr() == store.getAddr()
                                        && load.getOffset() == store.getOffset()
                                        && store.getCond() == null) {
                                    if (PrintInfo.printPeepHole)
                                        System.out.println("remove at " + block.emit() + ": " + load.emit());
                                    cur = new MCMove(load.getDst(), store.getSrc());
                                    load.insertAfter(cur);
                                    load.removeSelf();
                                    i--;
                                }
                            }
                            /*
                             *  MOV offset, #imm
                             *  LDR x, [address, offset]  <-  cur
                             *  =>
                             *  LDR x, [address, #imm]
                             */
                            else if (pre instanceof MCMove) {
                                var prev = ((MCMove) pre);
                                if (prev.getDst() == load.getOffset()
                                        && prev.getSrc().isImmediate()
                                        && ((Immediate) prev.getSrc()).getIntValue() < 4096
                                        && ((Immediate) prev.getSrc()).getIntValue() > -4095) {
                                    if (PrintInfo.printPeepHole)
                                        System.out.println("remove at " + block.emit() + ": " + prev.emit());
                                    load.setOffset(prev.getSrc());
                                    pre.removeSelf();
                                    i--;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}