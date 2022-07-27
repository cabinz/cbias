package passes.mc.registerAllocation;

/**
 * Unchangeable pair
 */
public class Pair<A, B> {

    /**
     * Key of this <code>Pair</code>.
     */
    private A a;

    /**
     * Gets the key for this pair.
     * @return key for this pair
     */
    public A getA() { return a; }

    /**
     * Value of this this <code>Pair</code>.
     */
    private B b;

    /**
     * Gets the value for this pair.
     * @return value for this pair
     */
    public B getB() { return b; }

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }

    /**
     * Creates a new pair
     * @param key The key for this pair
     * @param value The value to use for this pair
     */
    public Pair(A key, B value) {
        this.a = key;
        this.b = value;
    }

    /**
     * <p><code>String</code> representation of this
     * <code>Pair</code>.</p>
     *
     * <p>The default name/value delimiter '=' is always used.</p>
     *
     *  @return <code>String</code> representation of this <code>Pair</code>
     */
    @Override
    public String toString() {
        return "(" + a + ", " + b + ")";
    }

    /**
     * <p>Generate a hash code for this <code>Pair</code>.</p>
     *
     * <p>The hash code is calculated using both the name and
     * the value of the <code>Pair</code>.</p>
     *
     * @return hash code for this <code>Pair</code>
     */
    @Override
    public int hashCode() {
        // name's hashCode is multiplied by an arbitrary prime number (13)
        // in order to make sure there is a difference in the hashCode between
        // these two parameters:
        //  name: a  value: aa
        //  name: aa value: a
        return a.hashCode() * 13 + (b == null ? 0 : b.hashCode());
    }

    /**
     * <p>Test this <code>Pair</code> for equality with another
     * <code>Object</code>.</p>
     *
     * <p>If the <code>Object</code> to be tested is not a
     * <code>Pair</code> or is <code>null</code>, then this method
     * returns <code>false</code>.</p>
     *
     * <p>Two <code>Pair</code>s are considered equal if and only if
     * both the names and values are equal.</p>
     *
     * @param o the <code>Object</code> to test for
     * equality with this <code>Pair</code>
     * @return <code>true</code> if the given <code>Object</code> is
     * equal to this <code>Pair</code> else <code>false</code>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Pair) {
            Pair pair = (Pair) o;
            if (a != null ? !a.equals(pair.a) : pair.a != null) return false;
            if (b != null ? !b.equals(pair.b) : pair.b != null) return false;
            return true;
        }
        return false;
    }
}