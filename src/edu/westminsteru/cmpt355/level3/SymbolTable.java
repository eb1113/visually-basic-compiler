package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.lang.Function;
import edu.westminsteru.cmpt355.level3.lang.Type;
import edu.westminsteru.cmpt355.level3.lang.Variable;

import java.util.Optional;

/**
 * An interface representing an abstract symbol table, with the ability to
 * lookup (resolve) variables and functions by name
 */
public interface SymbolTable {

    /**
     * Returns the variable with the given name or an empty Optional if none is defined.
     * @param name the name of the variable to resolve
     */
    Optional<? extends Variable> resolveVariable(String name);
    
    /**
     * Returns the function with the given name or an empty Optional if none is defined.
     * @param name the name of the function to resolve
     */
    Optional<Function> resolveFunction(String name);
    
    /**
     * Defines a new variable having the given type and name.
     * @return the newly-defined variable
     * @throws DuplicateDefinitionException if there is a conflicting variable with this name
     */
    Variable defineVariable(Type type, String name) throws DuplicateDefinitionException;
}
