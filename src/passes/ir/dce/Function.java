package passes.ir.dce;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Function with extra information.
 */
class Function extends passes.ir.Function {

    private boolean sideEffect;

    private final Set<Function> caller = new HashSet<>();

    public Function(ir.values.Function rawFunction) {
        super(rawFunction);
        sideEffect = rawFunction.isExternal();
    }

    public boolean hasSideEffect(){
        return sideEffect;
    }

    public void setSideEffect(boolean hasSideEffect){
        sideEffect = hasSideEffect;
    }

    public void addCaller(Function function){
        caller.add(function);
    }

    public Collection<Function> getCallers(){
        return caller;
    }

}
