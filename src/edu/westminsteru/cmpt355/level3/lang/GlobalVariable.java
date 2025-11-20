package edu.westminsteru.cmpt355.level3.lang;

/**
 * Record representing a global variable in the language
 * <br>
 * N.B.: this is not an IR node and hence should not contain IR nodes:
 * instances of this interface should contain just the needed information to
 * access the relevant variable.
 */
public record GlobalVariable(/* add necessary fields here */) implements Variable {

    // Suggestion: fill in this method to return the field descriptor for
    // this variable based on the variable's type
    public String getDescriptor() {
        throw new RuntimeException("Not implemented");
    }
}
