package ir.types;

import ir.Type;

import java.util.ArrayList;


public class ArrayType extends Type {

    /**
     * The length of the array.
     * <br>
     * e.g. Length of [3 x [2 x i32]] is 3.
     */
    private int len;

    public int getLen() {
        return len;
    }

    /**
     * The type of direct array elements (one step forward on the nesting chain).
     * <br>
     * e.g. ElemType of [3 x [2 x i32]] is [2 x i32]
     * <br>
     * If you want to retrieve the ultimate type of the array, use getAtomType().
     */
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
     * This method calculates the sum of all atom elements of the array.
     * @return the size of array
     */
    public int getSize() {
        // TODO: rename getAtomNum
        int size = 1;
        for (Type tmp=this; tmp.isArrayType(); tmp=((ArrayType) tmp).getElemType())
            size *= ((ArrayType) tmp).getLen();
        return size;
    }

    /**
     * Return the type of the atom element of the array.
     * In SysY no pointer is supported, thus ultimate elements in arrays can only
     * be in primitive type (in no case be in pointer type or others).
     * <br>
     * e.g. AtomType of [3 x [2 x i32]] is i32
     * @return The ultimate element type on the nesting chain.
     */
    public PrimitiveType getAtomType() {
        Type tmp = this;
        while (tmp.isArrayType())
            tmp = ((ArrayType) tmp).getElemType();
        return (PrimitiveType) tmp;
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
