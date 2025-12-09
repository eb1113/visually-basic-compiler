package edu.westminsteru.cmpt355.level3.lang;

public record ArrayType(ScalarType baseType, int dimensions) implements Type {
    // int [][][]: baseType is int, dimensions = 3, componentType is int [][]
    
    public ArrayType {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be >= 1");
        }
    }
    
    /**
     * Given a component type T, return T[].
     * If T is already an ArrayType, increase its dimensions.
     * If T is a ScalarType, make a 1D array of it.
     */
    public static ArrayType of(Type componentType) {
        if (componentType instanceof ArrayType at) {
            return new ArrayType(at.baseType, at.dimensions + 1);
        } else if (componentType instanceof ScalarType st) {
            return new ArrayType(st, 1);
        } else {
            throw new IllegalArgumentException("Cannot make an array of type: " + componentType);
        }
    }

    /**
     * Type you get after taking one [] off.
     * int[][] -> componentType() is int[]
     * int[]   -> componentType() is int
     */
    public Type componentType() {
        if (dimensions == 1) {
            return baseType;
        } else {
            return new ArrayType(baseType, dimensions - 1);
        }
    }

    @Override
    public String getDescriptor() {
        // JVM arrays use "[<type>" for each dimension
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dimensions; i++) {
            sb.append('[');
        }
        sb.append(baseType.getDescriptor());
        return sb.toString();
    }

    @Override
    public int size() {
        // Arrays are reference types
        return 1;
    }

    @Override
    public boolean isReference() {
        return true;
    }
}
