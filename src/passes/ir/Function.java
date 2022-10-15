package passes.ir;

/**
 * Function wrapper, which will be inherited to store extra information.
 */
public class Function {
    protected final ir.values.Function rawFunction;

    public Function(ir.values.Function rawFunction){
        this.rawFunction = rawFunction;
    }

    public ir.values.Function getRawFunction() {
        return rawFunction;
    }


}
