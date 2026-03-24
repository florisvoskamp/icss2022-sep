package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.IfClause;
import nl.han.ica.icss.ast.Literal;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.Stylesheet;
import nl.han.ica.icss.ast.VariableAssignment;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

import java.util.ArrayList;
import java.util.HashMap;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
    }

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        variableValues.addFirst(new HashMap<>());
        evalStylesheet(ast.root);
    }

    private void pushScope() {
        variableValues.addFirst(new HashMap<>());
    }

    private void popScope() {
        if (variableValues.getSize() > 1) {
            variableValues.removeFirst();
        }
    }

    private void evalStylesheet(Stylesheet stylesheet) {
        for (ASTNode node : stylesheet.body) {
            if (node instanceof VariableAssignment) {
                evalVariableAssignment((VariableAssignment) node);
            } else if (node instanceof Stylerule) {
                evalStylerule((Stylerule) node);
            }
        }
    }

    private void evalStylerule(Stylerule stylerule) {
        pushScope();
        int i = 0;
        while (i < stylerule.body.size()) {
            ASTNode node = stylerule.body.get(i);
            if (node instanceof IfClause) {
                IfClause ifClause = (IfClause) node;
                Literal cond = evalExpression(ifClause.conditionalExpression);
                boolean value = ((BoolLiteral) cond).value;
                stylerule.body.remove(i);
                ArrayList<ASTNode> branch = value
                        ? ifClause.body
                        : (ifClause.elseClause != null ? ifClause.elseClause.body : new ArrayList<>());
                for (int j = 0; j < branch.size(); j++) {
                    stylerule.body.add(i + j, branch.get(j));
                }
            } else if (node instanceof Declaration) {
                Declaration declaration = (Declaration) node;
                declaration.expression = evalExpression(declaration.expression);
                i++;
            } else if (node instanceof VariableAssignment) {
                evalVariableAssignment((VariableAssignment) node);
                i++;
            } else {
                i++;
            }
        }
        popScope();
    }

    private void evalVariableAssignment(VariableAssignment assignment) {
        Literal literal = evalExpression(assignment.expression);
        assignment.expression = literal;
        variableValues.getFirst().put(assignment.name.name, literal);
    }

    private Literal evalExpression(Expression expression) {
        if (expression instanceof Literal) {
            return (Literal) expression;
        }
        if (expression instanceof VariableReference) {
            return lookup(((VariableReference) expression).name);
        }
        if (expression instanceof MultiplyOperation) {
            MultiplyOperation op = (MultiplyOperation) expression;
            Literal left = evalExpression(op.lhs);
            Literal right = evalExpression(op.rhs);
            return multiply(left, right);
        }
        if (expression instanceof AddOperation) {
            AddOperation op = (AddOperation) expression;
            Literal left = evalExpression(op.lhs);
            Literal right = evalExpression(op.rhs);
            return add(left, right);
        }
        if (expression instanceof SubtractOperation) {
            SubtractOperation op = (SubtractOperation) expression;
            Literal left = evalExpression(op.lhs);
            Literal right = evalExpression(op.rhs);
            return subtract(left, right);
        }
        return null;
    }

    private Literal lookup(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private Literal multiply(Literal left, Literal right) {
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);
        }
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            int v = ((ScalarLiteral) left).value * ((PixelLiteral) right).value;
            return new PixelLiteral(v + "px");
        }
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int v = ((PixelLiteral) left).value * ((ScalarLiteral) right).value;
            return new PixelLiteral(v + "px");
        }
        if (left instanceof ScalarLiteral && right instanceof PercentageLiteral) {
            int v = ((ScalarLiteral) left).value * ((PercentageLiteral) right).value;
            return new PercentageLiteral(v + "%");
        }
        if (left instanceof PercentageLiteral && right instanceof ScalarLiteral) {
            int v = ((PercentageLiteral) left).value * ((ScalarLiteral) right).value;
            return new PercentageLiteral(v + "%");
        }
        return null;
    }

    private Literal add(Literal left, Literal right) {
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            int v = ((PixelLiteral) left).value + ((PixelLiteral) right).value;
            return new PixelLiteral(v + "px");
        }
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
            int v = ((PercentageLiteral) left).value + ((PercentageLiteral) right).value;
            return new PercentageLiteral(v + "%");
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value + ((ScalarLiteral) right).value);
        }
        return null;
    }

    private Literal subtract(Literal left, Literal right) {
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            int v = ((PixelLiteral) left).value - ((PixelLiteral) right).value;
            return new PixelLiteral(v + "px");
        }
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
            int v = ((PercentageLiteral) left).value - ((PercentageLiteral) right).value;
            return new PercentageLiteral(v + "%");
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value - ((ScalarLiteral) right).value);
        }
        return null;
    }
}
