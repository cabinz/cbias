package ir.values;

import ir.Type;
import ir.Value;

/**
 * 'undef' values are things that do not have specified contents.
 * These are used for a variety of purposes, including global variable initializers and operands to instructions.
 * Unlike in LLVM, our UndefValue directly inherit from Value (other than Constant), making it a more general
 * placeholder for processes of more types like Instructions, BasicBlock.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/14.x/llvm/include/llvm/IR/Constants.h#L1377">
 *     LLVM Source: UndefValue</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#undefined-values">
 *     LLVM Ref: Undefined Values</a>
 */
public class UndefValue extends Value {

    public UndefValue(Type type) {
        super(type);
    }
}
