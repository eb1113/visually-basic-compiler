package edu.westminsteru.cmpt355.level3.lang;

public record ArrayType(Type elementType) implements Type{

    @Override
    public String getDescriptor() {
        // JVM arrays use "[<type>"
        return "[" + elementType.getDescriptor();
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
