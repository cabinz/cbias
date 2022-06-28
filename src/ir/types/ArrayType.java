package ir.types;

import ir.Type;


public class ArrayType extends Type {
    
    private int len;

    public int getLen() {
        return len;
    }

    
    private Type elemType;

    public Type getElemType() {
        return elemType;
    }


    private ArrayType(Type elemType, int len) {
        if (len <= 0) {
            throw new RuntimeException("Try to build a ArrayType with a non-positive length.\n");
        }

        this.elemType = elemType;
        this.len = len;
    }

    
    public static ArrayType getType(Type elemType, int len) {
        return new ArrayType(elemType, len);
    }

    @Override
    public String toString() {
        return "[" + len + " x " + elemType.toString() + "]";
    }
}
