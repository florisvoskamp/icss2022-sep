package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MaxFunction;
import nl.han.ica.icss.ast.operations.MinFunction;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {
	
	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private IHANStack<ASTNode> currentContainer;

	private IHANStack<Expression> expressionStack;

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
		expressionStack = new HANStack<>();
	}

	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
		currentContainer.push(ast.root);
	}

	@Override
	public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
		currentContainer.pop();
	}

	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx) {
		Stylerule rule = new Stylerule();
		currentContainer.peek().addChild(rule);
		currentContainer.push(rule);
	}

	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx) {
		currentContainer.pop();
	}

	@Override
	public void enterIfClause(ICSSParser.IfClauseContext ctx) {
		IfClause clause = new IfClause();
		currentContainer.peek().addChild(clause);
		currentContainer.push(clause);
	}

	@Override
	public void exitIfClause(ICSSParser.IfClauseContext ctx) {
		currentContainer.pop();
	}

	@Override
	public void exitIfHead(ICSSParser.IfHeadContext ctx) {
		Expression conditional = expressionStack.pop();
		currentContainer.peek().addChild(conditional);
	}

	@Override
	public void enterElseClause(ICSSParser.ElseClauseContext ctx) {
		ElseClause clause = new ElseClause();
		currentContainer.peek().addChild(clause);
		currentContainer.push(clause);
	}

	@Override
	public void exitElseClause(ICSSParser.ElseClauseContext ctx) {
		currentContainer.pop();
	}

	@Override
	public void exitTagSelector(ICSSParser.TagSelectorContext ctx) {
		TagSelector selector = new TagSelector(ctx.getText());
		currentContainer.peek().addChild(selector);
	}

	@Override
	public void exitIdSelector(ICSSParser.IdSelectorContext ctx) {
		IdSelector selector = new IdSelector(ctx.getText());
		currentContainer.peek().addChild(selector);
	}

	@Override
	public void exitClassSelector(ICSSParser.ClassSelectorContext ctx) {
		ClassSelector selector = new ClassSelector(ctx.getText());
		currentContainer.peek().addChild(selector);
	}

	@Override
	public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
		Declaration declaration = new Declaration();
		currentContainer.peek().addChild(declaration);
		currentContainer.push(declaration);
	}

	@Override
	public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		VariableAssignment assignment = new VariableAssignment();
		currentContainer.peek().addChild(assignment);
		currentContainer.push(assignment);
	}

	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		String name = ctx.CAPITAL_IDENT().getText();
		currentContainer.peek().addChild(new VariableReference(name));
		Expression expression = expressionStack.pop();
		currentContainer.peek().addChild(expression);
		currentContainer.pop();
	}

	@Override
	public void exitVariableReference(ICSSParser.VariableReferenceContext ctx) {
		expressionStack.push(new VariableReference(ctx.getText()));
	}

	@Override
	public void exitPropertyName(ICSSParser.PropertyNameContext ctx) {
		PropertyName propertyName = new PropertyName(ctx.getText());
		currentContainer.peek().addChild(propertyName);
	}

	@Override
	public void exitBuiltinCall(ICSSParser.BuiltinCallContext ctx) {
		Expression right = expressionStack.pop();
		Expression left = expressionStack.pop();
		Operation op;
		if (ctx.MIN_KW() != null) {
			op = new MinFunction();
		} else {
			op = new MaxFunction();
		}
		op.addChild(left);
		op.addChild(right);
		expressionStack.push(op);
	}

	@Override
	public void exitLiteral(ICSSParser.LiteralContext ctx) {
		if (ctx.COLOR() != null) {
			expressionStack.push(new ColorLiteral(ctx.COLOR().getText()));
		} else if (ctx.PIXELSIZE() != null) {
			expressionStack.push(new PixelLiteral(ctx.PIXELSIZE().getText()));
		} else if (ctx.PERCENTAGE() != null) {
			expressionStack.push(new PercentageLiteral(ctx.PERCENTAGE().getText()));
		} else if (ctx.SCALAR() != null) {
			expressionStack.push(new ScalarLiteral(ctx.SCALAR().getText()));
		} else if (ctx.TRUE() != null) {
			expressionStack.push(new BoolLiteral(true));
		} else if (ctx.FALSE() != null) {
			expressionStack.push(new BoolLiteral(false));
		}
	}

	@Override
	public void exitMult(ICSSParser.MultContext ctx) {
		int count = ctx.primary().size();
		Expression[] operands = new Expression[count];
		for (int i = count - 1; i >= 0; i--) {
			operands[i] = expressionStack.pop();
		}
		Expression result = operands[0];
		for (int i = 1; i < count; i++) {
			MultiplyOperation operation = new MultiplyOperation();
			operation.addChild(result);
			operation.addChild(operands[i]);
			result = operation;
		}
		expressionStack.push(result);
	}

	@Override
	public void exitPlusminus(ICSSParser.PlusminusContext ctx) {
		int count = ctx.mult().size();
		Expression[] operands = new Expression[count];
		for (int i = count - 1; i >= 0; i--) {
			operands[i] = expressionStack.pop();
		}
		Expression result = operands[0];
		for (int i = 1; i < count; i++) {
			String operator = ctx.getChild(2 * i - 1).getText();
			if (operator.equals("+")) {
				AddOperation operation = new AddOperation();
				operation.addChild(result);
				operation.addChild(operands[i]);
				result = operation;
			} else {
				SubtractOperation operation = new SubtractOperation();
				operation.addChild(result);
				operation.addChild(operands[i]);
				result = operation;
			}
		}
		expressionStack.push(result);
	}

	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		Expression expression = expressionStack.pop();
		currentContainer.peek().addChild(expression);
		currentContainer.pop();
	}

	public AST getAST() {
		return ast;
	}

}