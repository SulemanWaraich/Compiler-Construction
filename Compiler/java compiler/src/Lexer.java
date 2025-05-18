import java.util.*;
import java.util.regex.*;

public class Lexer {

    private static final Pattern ALPHAN_VAR = Pattern.compile("Alphan\\s+(\\w+)\\s*=\\s*\"([^\"]*)\";");
    private static final Pattern NUMBER_VAR = Pattern.compile("number\\s+(\\w+)\\s*=\\s*(\\d+);");
    private static final Pattern EXPRESSION_VAR = Pattern.compile("number\\s+(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-*/])\\s*(\\w+);");
    private static final Pattern PRINT = Pattern.compile("publish\\((\\w+)\\);");

    private Map<String, String> strVars = new HashMap<>();
    private Map<String, Integer> numVars = new HashMap<>();

    public void analyze(String line, int lineNumber) {
        line = line.trim();

        // Skip empty or comment lines
        if (line.equals("") || line.equals("begin main {") || line.equals("}")) return;

        Matcher matcher;

        // Alphan (string) variable
        if (line.startsWith("Alphan")) {
            matcher = ALPHAN_VAR.matcher(line);
            if (matcher.matches()) {
                String varName = matcher.group(1);
                String value = matcher.group(2);
                strVars.put(varName, value);
                System.out.println("Line " + lineNumber + ": Variable '" + varName + "' assigned value \"" + value + "\"");
                return;
            } else {
                System.out.println("Syntax Error on line " + lineNumber + ": Invalid string variable assignment.");
                return;
            }
        }

        // Integer variable direct assignment
        if (line.startsWith("number") && !line.contains("+") && !line.contains("-") && !line.contains("*") && !line.contains("/")) {
            matcher = NUMBER_VAR.matcher(line);
            if (matcher.matches()) {
                String varName = matcher.group(1);
                int value = Integer.parseInt(matcher.group(2));
                numVars.put(varName, value);
                System.out.println("Line " + lineNumber + ": Number variable '" + varName + "' assigned value " + value);
                return;
            }
        }

        // Integer variable with arithmetic expression
        matcher = EXPRESSION_VAR.matcher(line);
        if (matcher.matches()) {
            String varName = matcher.group(1);
            String leftOperand = matcher.group(2);
            String operator = matcher.group(3);
            String rightOperand = matcher.group(4);

            if (!numVars.containsKey(leftOperand) || !numVars.containsKey(rightOperand)) {
                System.out.println("Error on line " + lineNumber + ": Undefined variable in expression.");
                return;
            }

            int left = numVars.get(leftOperand);
            int right = numVars.get(rightOperand);
            int result = 0;

            switch (operator) {
                case "+": result = left + right; break;
                case "-": result = left - right; break;
                case "*": result = left * right; break;
                case "/":
                    if (right == 0) {
                        System.out.println("Error on line " + lineNumber + ": Division by zero.");
                        return;
                    }
                    result = left / right;
                    break;
            }

            numVars.put(varName, result);
            System.out.println("Line " + lineNumber + ": Number variable '" + varName + "' assigned result " + result);
            return;
        }

        // Print or publish
        if (line.startsWith("publish")) {
            matcher = PRINT.matcher(line);
            if (matcher.matches()) {
                String varName = matcher.group(1);
                if (strVars.containsKey(varName)) {
                    System.out.println("Line " + lineNumber + ": Output → " + strVars.get(varName));
                } else if (numVars.containsKey(varName)) {
                    System.out.println("Line " + lineNumber + ": Output → " + numVars.get(varName));
                } else {
                    System.out.println("Error on line " + lineNumber + ": Variable '" + varName + "' not found.");
                }
                return;
            } else {
                System.out.println("Syntax Error on line " + lineNumber + ": Invalid publish statement.");
                return;
            }
        }

        // If nothing matches
        System.out.println("Syntax Error on line " + lineNumber + ": Unrecognized statement → " + line);
    }
}
