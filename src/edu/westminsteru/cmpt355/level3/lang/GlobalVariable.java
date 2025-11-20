package edu.westminsteru.cmpt355.level3.lang;

/**
 * Record representing a global variable in the language
 * <br>
 * N.B.: this is not an IR node and hence should not contain IR nodes:
 * instances of this interface should contain just the needed information to
 * access the relevant variable.
 */
public record GlobalVariable(Type type, String name) implements Variable {

    public String getDescriptor() {
        return type.getDescriptor();
    }
}
