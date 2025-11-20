package edu.westminsteru.cmpt355.level3.lang;

/**
 * Record representing a function in the language.
 * <br>
 * N.B.: this is not an IR node and hence should not contain IR nodes: this
 * record should contain just the needed information to access (call) the
 * function, but not the function's code.
 */
public record Function(/* add necessary fields here */) {

    // Suggestion: fill in this method to return a descriptor for this function/
    // method (e.g. "(I)I")
    public String getDescriptor() {
        throw new RuntimeException("Not implemented");
    }
}
