package ir.types;

import ir.Type;
import ir.values.Constant;

public abstract class PrimitiveType extends Type{

    public abstract Constant getZero();

}
