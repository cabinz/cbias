package passes.ir.gv_localize;

import passes.ir.RelationAnalysis;

import java.util.HashMap;
import java.util.Map;

public class Function extends passes.ir.Function {

    Map<ir.values.BasicBlock, BasicBlock> basicBlockMap = new HashMap<>();

    public Function(ir.values.Function function){
        super(function);
        function.forEach(basicBlock -> basicBlockMap.put(basicBlock, new BasicBlock(basicBlock)));
        RelationAnalysis.analysisBasicBlocks(basicBlockMap);
    }

}
