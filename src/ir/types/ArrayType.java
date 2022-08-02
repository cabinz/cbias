package ir.types;

import ir.Type;
import ir.values.Constant;
import ir.values.constants.ConstArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class ArrayType extends PrimitiveType {

    /**
     * The length of the array (in the immediately accessible layer).
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
    public ArrayList<Integer> getDimLens() {
        ArrayList<Integer> ret = new ArrayList<>();
        for (Type tmp = this; tmp.isArrayType(); tmp = ((ArrayType) tmp).getElemType())
            ret.add(((ArrayType) tmp).getLen());
        return ret;
    }

    /**
     * This method calculates the sum of all atom elements of the array.
     * @return the number of atom elements in the array
     */
    public int getAtomLen() {
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


    //<editor-fold desc="Singleton">
    private ArrayType(Type elemType, int len) {
        if (len <= 0) {
            throw new RuntimeException("Try to build a ArrayType with a non-positive length.\n");
        }

        this.elemType = elemType;
        this.len = len;
    }

    private static class DimLensKey {
        public final ArrayList<Integer> dimLens = new ArrayList<>();

        public DimLensKey(Type elemType, int len) {
            this.dimLens.add(len);
            if (elemType.isArrayType()) {
                this.dimLens.addAll(((ArrayType) elemType).getDimLens());
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimLens);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DimLensKey that = (DimLensKey) o;
            return Objects.equals(dimLens, that.dimLens);
        }
    }

    private final static HashMap<DimLensKey, ArrayType> pool = new HashMap<>();

    public static ArrayType getType(Type elemType, int len) {
        var key = new DimLensKey(elemType, len);
        if (pool.containsKey(key)) {
            return pool.get(key);
        }

        var newType = new ArrayType(elemType, len);
        pool.put(key, newType);
        return newType;
    }
    //</editor-fold>


    @Override
    public String toString() {
        return "[" + len + " x " + elemType.toString() + "]";
    }

    @Override
    public Constant getZero() {
        ArrayList<Constant> initList = new ArrayList<>();
        for (int i = 0; i < this.getLen(); i++) {
            initList.add(((PrimitiveType) this.getElemType()).getZero());
        }
        return ConstArray.get(this, initList);
    }
}
