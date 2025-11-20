package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.lang.Function;
import edu.westminsteru.cmpt355.level3.lang.LocalVariable;
import edu.westminsteru.cmpt355.level3.lang.Type;
import edu.westminsteru.cmpt355.level3.lang.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A local symbol table. At the local level, functions cannot be defined; when resolving, any symbols
 * not found in the local table are resolved in the global table.
 */
public class LocalSymbolTable implements SymbolTable {

    private final GlobalSymbolTable globalTable;
    private final LocalSymbolTable parent;
    private final LocalSymbolTable root;
    private final Map<String, LocalVariable> localTable;
    
    // Track the next available local variable index; only meaningful for the root scope
    private int nextIndex = 0;

    /** Creates a new local symbol table, descending from a given global symbol table */
    public LocalSymbolTable(GlobalSymbolTable globalTable) {
        this(globalTable, null);
    }

    private LocalSymbolTable(GlobalSymbolTable globalTable, LocalSymbolTable parent) {
        this.globalTable = globalTable;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        this.localTable = new HashMap<String, LocalVariable>();
    }

    /**
     * Returns the variable with the given name (checking in the local table first and then the global
     * if not found) or an empty Optional if none is defined.
     * @param name the name of the variable to resolve
     */
    @Override
    public Optional<? extends Variable> resolveVariable(String name) {
        if(localTable.containsKey(name)){
            return Optional.ofNullable(localTable.get(name));
        }
        if (parent != null) {
            return parent.resolveVariable(name);
        }
        return globalTable.resolveVariable(name);
    }

    /**
     * Returns the function with the given name or an empty Optional if none is defined. (Since functions
     * cannot be defined at the local level, this method just defers to the global table.)
     * @param name the name of the variable to resolve
     */
    @Override
    public Optional<Function> resolveFunction(String name) {
        if (parent != null) {
            return parent.resolveFunction(name);
        }
        return globalTable.resolveFunction(name); 
    }

    /**
     * Defines a new (local) variable having the given type and name.
     * @return the newly-defined variable
     * @throws DuplicateDefinitionException if there is a conflicting variable with this name
     */
    @Override
    public LocalVariable defineVariable(Type type, String name) throws DuplicateDefinitionException {
        if(localTable.containsKey(name)){
            throw new DuplicateDefinitionException("Variable " + name + " is already defined in this scope.");
        }
        LocalVariable newVar = new LocalVariable(type, name, allocateIndex(type));
        localTable.put(name, newVar);
        return newVar;
    }

    private int allocateIndex(Type type) {
        int index = root.nextIndex;
        root.nextIndex += type.size(); // Increment by the size of the type (1 for int/string/boolean, 2 for double)
        return index;
    }

    public LocalSymbolTable createChildScope() {
        return new LocalSymbolTable(globalTable, this);
    }
}
