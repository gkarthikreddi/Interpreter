package craftinginterpreter.lox;

class RPN implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return expr.left.accept(this) + expr.right.accept(this) + expr.operator.lexeme ;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        // Apparently in RPN we don't need explicit grouping
        return expr.expression.accept(this);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return "(-" + expr.right.accept(this) + ")";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitConditionalExpr(Expr.Conditional expr) {
        return expr.base.accept(this) + " " + expr.left.accept(this) + " " + expr.right.accept(this);
    }

    public static void main(String[] args) {
        Expr exp = new Expr.Binary(
                new Expr.Binary(
                    new Expr.Literal(1), new Token(TokenType.PLUS, "+", null, 1),
                    new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(
                    new Expr.Literal(4), new Token(TokenType.MINUS, "-", null, 1),
                    new Expr.Literal(3)));
        System.out.println(new RPN().print(exp));
    }
}
