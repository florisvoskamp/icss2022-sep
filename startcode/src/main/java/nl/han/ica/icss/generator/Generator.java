package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.Selector;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;

public class Generator {

    public String generate(AST ast) {
        StringBuilder builder = new StringBuilder();
        for (ASTNode node : ast.root.body) {
            if (node instanceof Stylerule) {
                appendStylerule(builder, (Stylerule) node, 0);
            }
        }
        return builder.toString();
    }

    private void appendStylerule(StringBuilder builder, Stylerule stylerule, int indentLevel) {
        String outer = " ".repeat(indentLevel * 2);
        String inner = " ".repeat((indentLevel + 1) * 2);
        for (Selector selector : stylerule.selectors) {
            builder.append(outer).append(selector.toString()).append(" {\n");
        }
        for (ASTNode node : stylerule.body) {
            if (node instanceof Declaration) {
                Declaration declaration = (Declaration) node;
                builder.append(inner)
                        .append(declaration.property.name)
                        .append(": ")
                        .append(expressionToCss(declaration.expression))
                        .append(";\n");
            }
        }
        builder.append(outer).append("}\n");
    }

    private String expressionToCss(Expression expression) {
        if (expression instanceof ColorLiteral) {
            return ((ColorLiteral) expression).value;
        }
        if (expression instanceof PixelLiteral) {
            PixelLiteral pixel = (PixelLiteral) expression;
            return pixel.value + "px";
        }
        if (expression instanceof PercentageLiteral) {
            PercentageLiteral percentage = (PercentageLiteral) expression;
            return percentage.value + "%";
        }
        return expression.toString();
    }
}
