package edu.westminsteru.cmpt355.level3.ir;
import edu.westminsteru.cmpt355.level3.lang.ReturnType;

import java.util.ArrayList;
import java.util.List;

/**
 * Function declaration: void functionName(params) { statements }
 * @param type The return type of the function (void).
 * @param name The name of the function (functionName).
 * @param parameters The parameters of the function (params).
 * @param body The body of the function (statements).
 */
public record IrFunctionDeclaration(
        ReturnType type,
        String name,
        List<IrParameter> parameters,
        IrBlock body
) implements IrStatement {
    @Override
    public Iterable<? extends IrNode> children() {
        List<IrNode> children = new ArrayList<>(parameters);
        children.add(body);
        return children;
    }
}
