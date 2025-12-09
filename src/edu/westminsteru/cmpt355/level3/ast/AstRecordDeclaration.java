package edu.westminsteru.cmpt355.level3.ast;

import java.util.List;
import java.util.Map;
import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Record declaration: record Point(double x, double y) { methods }
 * @param name The name of the record.
 * @param fields The fields of the record.
 * @param methods The methods of the record.
 */
public record AstRecordDeclaration(String name, Map<String, Type> fields, List<AstFunctionDeclaration> methods)
        implements AstStatement {
    @Override
    public Iterable<? extends AstNode> children() {
        return methods;
    }
}

