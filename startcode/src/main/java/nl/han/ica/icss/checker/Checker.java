package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.ElseClause;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.IfClause;
import nl.han.ica.icss.ast.Literal;
import nl.han.ica.icss.ast.Operation;
import nl.han.ica.icss.ast.Selector;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.Stylesheet;
import nl.han.ica.icss.ast.VariableAssignment;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;

public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();
        variableTypes.addFirst(new HashMap<>());
        checkStylesheet(ast.root);
    }

    private void pushScope() {
        variableTypes.addFirst(new HashMap<>());
    }

    private void popScope() {
        if (variableTypes.getSize() > 1) {
            variableTypes.removeFirst();
        }
    }

    private void checkStylesheet(Stylesheet stylesheet) {
        for (ASTNode node : stylesheet.body) {
            checkStylesheetChild(node);
        }
    }

    private void checkStylesheetChild(ASTNode node) {
        if (node instanceof VariableAssignment) {
            checkVariableAssignment((VariableAssignment) node);
        } else if (node instanceof Stylerule) {
            checkStylerule((Stylerule) node);
        }
    }

    private void checkStylerule(Stylerule stylerule) {
        pushScope();
        for (ASTNode child : stylerule.getChildren()) {
            if (child instanceof Selector) {
                continue;
            }
            checkStyleruleBodyChild(child);
        }
        popScope();
    }

    private void checkStyleruleBodyChild(ASTNode node) {
        if (node instanceof Declaration) {
            checkDeclaration((Declaration) node);
        } else if (node instanceof VariableAssignment) {
            checkVariableAssignment((VariableAssignment) node);
        } else if (node instanceof IfClause) {
            checkIfClause((IfClause) node);
        }
    }

    private void checkIfClause(IfClause ifClause) {
        ExpressionType condType = checkExpression(ifClause.conditionalExpression);
        if (condType != ExpressionType.BOOL) {
            ifClause.conditionalExpression.setError("if condition must be boolean");
        }
        pushScope();
        for (ASTNode node : ifClause.body) {
            checkStyleruleBodyChild(node);
        }
        popScope();
        if (ifClause.elseClause != null) {
            checkElseClause(ifClause.elseClause);
        }
    }

    private void checkElseClause(ElseClause elseClause) {
        pushScope();
        for (ASTNode node : elseClause.body) {
            checkStyleruleBodyChild(node);
        }
        popScope();
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType type = checkExpression(assignment.expression);
        variableTypes.getFirst().put(assignment.name.name, type);
    }

    private void checkDeclaration(Declaration declaration) {
        ExpressionType valueType = checkExpression(declaration.expression);
        String prop = declaration.property.name;
        if (prop.equals("color") || prop.equals("background-color")) {
            if (valueType != ExpressionType.COLOR) {
                declaration.setError("value type does not match property");
            }
        } else if (prop.equals("width") || prop.equals("height")) {
            if (valueType != ExpressionType.PIXEL && valueType != ExpressionType.PERCENTAGE) {
                declaration.setError("value type does not match property");
            }
        }
    }

    private ExpressionType checkExpression(Expression expression) {
        if (expression instanceof Literal) {
            return typeOfLiteral((Literal) expression);
        }
        if (expression instanceof VariableReference) {
            return checkVariableReference((VariableReference) expression);
        }
        if (expression instanceof MultiplyOperation) {
            return checkMultiply((MultiplyOperation) expression);
        }
        if (expression instanceof AddOperation) {
            return checkAddSubtract((AddOperation) expression);
        }
        if (expression instanceof SubtractOperation) {
            return checkAddSubtract((SubtractOperation) expression);
        }
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkVariableReference(VariableReference reference) {
        ExpressionType type = lookup(reference.name);
        if (type == null) {
            reference.setError("undefined variable");
            return ExpressionType.UNDEFINED;
        }
        return type;
    }

    private ExpressionType lookup(String name) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private ExpressionType typeOfLiteral(Literal literal) {
        if (literal instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        }
        if (literal instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        }
        if (literal instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        }
        if (literal instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        }
        if (literal instanceof BoolLiteral) {
            return ExpressionType.BOOL;
        }
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkMultiply(MultiplyOperation op) {
        ExpressionType left = checkExpression(op.lhs);
        ExpressionType right = checkExpression(op.rhs);
        if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
            op.setError("color not allowed in multiplication");
            return ExpressionType.UNDEFINED;
        }
        if (left == ExpressionType.BOOL || right == ExpressionType.BOOL) {
            op.setError("invalid operands for multiplication");
            return ExpressionType.UNDEFINED;
        }
        boolean leftScalar = left == ExpressionType.SCALAR;
        boolean rightScalar = right == ExpressionType.SCALAR;
        if (!leftScalar && !rightScalar) {
            op.setError("multiplication needs at least one scalar operand");
            return ExpressionType.UNDEFINED;
        }
        if (leftScalar && rightScalar) {
            return ExpressionType.SCALAR;
        }
        if (leftScalar) {
            return right;
        }
        return left;
    }

    private ExpressionType checkAddSubtract(Operation op) {
        ExpressionType left = checkExpression(op.lhs);
        ExpressionType right = checkExpression(op.rhs);
        if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
            op.setError("color not allowed in addition or subtraction");
            return ExpressionType.UNDEFINED;
        }
        if (left == ExpressionType.BOOL || right == ExpressionType.BOOL) {
            op.setError("invalid operands for addition or subtraction");
            return ExpressionType.UNDEFINED;
        }
        if (left != right) {
            op.setError("operand types must match for addition or subtraction");
            return ExpressionType.UNDEFINED;
        }
        return left;
    }
}
