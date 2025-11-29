package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.lang.Method;
import edu.westminsteru.cmpt355.level3.lang.ReturnType;
import edu.westminsteru.cmpt355.level3.lang.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A symbol table for record-specific symbols (fields and methods).
 * Similar to GlobalSymbolTable but scoped to a single record definition.
 */
public class RecordSymbolTable {

    private final Map<String, Type> fieldTable = new HashMap<String, Type>();
    private final Map<String, Method> methodTable = new HashMap<String, Method>();

    /**
     * Returns the field type with the given name or an empty Optional if none is defined.
     * @param name the name of the field to resolve
     */
    public Optional<Type> resolveField(String name) {
        if (fieldTable.containsKey(name)) {
            return Optional.ofNullable(fieldTable.get(name));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the method with the given name or an empty Optional if none is defined.
     * @param name the name of the method to resolve
     */
    public Optional<Method> resolveMethod(String name) {
        if (methodTable.containsKey(name)) {
            return Optional.ofNullable(methodTable.get(name));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Defines a new field having the given type and name.
     * @param type the type of the field
     * @param name the name of the field
     * @return the type of the newly-defined field
     * @throws DuplicateDefinitionException if there is a conflicting field with this name
     */
    public Type defineField(Type type, String name) throws DuplicateDefinitionException {
        if (fieldTable.containsKey(name)) {
            throw new DuplicateDefinitionException("Field " + name + " is already defined in this record.");
        } else {
            fieldTable.put(name, type);
            return type;
        }
    }

    /**
     * Defines a new method having the given return type, name, and parameter types.
     * @param returnType the return type of the method
     * @param name the name of the method
     * @param parameterTypes the parameter types of the method
     * @return the newly-defined method
     * @throws DuplicateDefinitionException if there is a conflicting method with this name
     */
    public Method defineMethod(ReturnType returnType, String name, List<Type> parameterTypes) throws DuplicateDefinitionException {
        if (methodTable.containsKey(name)) {
            throw new DuplicateDefinitionException("Method " + name + " is already defined in this record.");
        } else {
            Method newMethod = new Method(name, returnType, parameterTypes);
            methodTable.put(name, newMethod);
            return newMethod;
        }
    }

    /**
     * Returns a copy of all fields defined in this record symbol table
     */
    public Map<String, Type> getFields() {
        return Map.copyOf(fieldTable);
    }

    /**
     * Returns a copy of all methods defined in this record symbol table
     */
    public Map<String, Method> getMethods() {
        return Map.copyOf(methodTable);
    }
}

