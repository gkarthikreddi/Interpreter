package craftinginterpreter.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                    "Assign     : Token name, Expr value",
                    "Binary     : Expr left, Token operator, Expr right",
                    "Call       : Expr callee, Token paren, List<Expr> arguments",
                    "Get        : Expr object, Token name",
                    "Grouping   : Expr expression",
                    "Literal    : Object value",
                    "Logical    : Expr left, Token operator, Expr right",
                    "Set        : Expr object, Token name, Expr value",
                    "Super      : Token keyword, Token method",
                    "This       : Token keyword",
                    "Unary      : Token operator, Expr right",
                    "Variable   : Token name",

                    // implementing ternary
                    "Conditional: Expr base, Expr left, Expr right"
                    ));

        defineAst(outputDir, "Stmt", Arrays.asList(
                    "Block      : List<Stmt> statements",
                    "Class      : Token name, Expr.Variable superclass, List<Stmt.Function> methods",
                    "Expression : Expr expression",
                    "Function   : Token name, List<Token> params, List<Stmt> body",
                    "If         : Expr condition, Stmt thenBranch, Stmt elsebranch",
                    "Break      : Token keyword",
                    "Print      : Expr expression",
                    "Return     : Token keyword, Expr value",
                    "While      : Expr condition, Stmt body",
                    "Var        : Token name, Expr initializer"
                    ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types)
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package craftinginterpreter.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);
        
        // The AST calsses
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // The base accept() method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineType
        (PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("    static class " + className + " extends " + baseName + " {");
        
        // Store parameter in the fields.
        String[] fields = fieldList.split(", ");

        // Fields.
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }
        writer.println();

        // Constructor.
        writer.println("        " + className + "(" + fieldList + ") {");

        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }

        writer.println("        }");

        // Visitor pattern.
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" +
                className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
    }

    private static void defineVisitor
            (PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " +
                    baseName.toLowerCase() + ");");
        }

        writer.println("    }");
    }
}
