package ir.types;

import ir.Type;

import java.util.ArrayList;


public class ArrayType extends Type {
    
    private int len;

    public int getLen() {
        return len;
    }

    
    private Type elemType;

    public Type getElemType() {
        return elemType;
    }

    /**
     * This method calculates the sizes of each dimension of this array.
     * @return A list containing each dimension size, starting from the first dimension
     */
    public ArrayList<Integer> getDimSize() {
        ArrayList<Integer> ret = new ArrayList<>();
        for (Type tmp=this; tmp.isArrayType(); tmp=((ArrayType) tmp).getElemType())
            ret.add(((ArrayType) tmp).getLen());
        return ret;
    }

    /**
     * This method calculates the sum of all elements of the array.
     * @return the size of array
     */
    public int getSize() {
        int size = 1;
        for (Type tmp=this; tmp.isArrayType(); tmp=((ArrayType) tmp).getElemType())
            size *= ((ArrayType) tmp).getLen();
        return size;
    }

    /**
     * Return the primitive type of this array.
     * @return the primitive type
     */
    public Type getPrimitiveType() {
        Type tmp = this;
        while (tmp.isArrayType())
            tmp = ((ArrayType) tmp).getElemType();
        return tmp;
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
