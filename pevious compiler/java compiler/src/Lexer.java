import java.util.*;
import java.util.regex.*;

public class Lexer {

    private static final Pattern ALPHAN_VAR = Pattern.compile("Alphan\\s+(\\w+)\\s*=\\s*\"([^\"]*)\";");
    private static final Pattern NUMBER_VAR = Pattern.compile("number\\s+(\\w+)\\s*=\\s*(\\d+);");
    private static final Pattern EXPRESSION_VAR = Pattern.compile("number\\s+(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-*/])\\s*(\\w+);");
    private static final Pattern PRINT = Pattern.compile("publish\\((\\w+)\\);");
private boolean insideMain = false;

    // Updated patterns for arrays
    private static final Pattern STRING_ARRAY = Pattern.compile("Alphan\\[\\]\\s+(\\w+)\\s*=\\s*\\{\\s*((\"[^\"]*\"\\s*,\\s*)*\"[^\"]*\")\\s*};?");
    private static final Pattern NUMBER_ARRAY = Pattern.compile("number\\[\\]\\s+(\\w+)\\s*=\\s*\\{\\s*((\\d+(\\.\\d+)?\\s*,\\s*)*\\d+(\\.\\d+)?)\\s*};?");
   private static final Pattern ARRAY_ACCESS = Pattern.compile("publish\\((\\w+)\\s*\\[\\s*(\\d+)\\s*]\\);");

    private static final Pattern FUNC_DEF = Pattern.compile("func\\s+(\\w+)\\(([^)]*)\\)\\s*\\{");
    private static final Pattern FUNC_END = Pattern.compile("}");
    private static final Pattern FUNC_CALL = Pattern.compile("call\\s+(\\w+)\\(([^)]*)\\);");

    private final Map<String, String> strVars = new HashMap<>();
    private final Map<String, Integer> numVars = new HashMap<>();
    private final Map<String, List<String>> stringArrays = new HashMap<>();
    private final Map<String, List<Double>> numberArrays = new HashMap<>();
private final Map<String, List<String>> functionParams = new HashMap<>();
private final Map<String, List<String>> functionBody = new HashMap<>();
private boolean insideFunction = false;
private String currentFunction = null;
private List<String> currentFunctionLines = new ArrayList<>();

   public void analyze(String line, int lineNumber) {
    long start = System.nanoTime();  // Start timing

    line = line.trim();
    if (line.equals("") || line.equals("begin main {") || line.equals("}")) {
        long end = System.nanoTime();
        printLineTime(lineNumber, start, end);
        return;
    }

    // 🔴 Universal semicolon enforcement block
    if (!line.endsWith(";")) {
        // Exempt certain lines from semicolon requirement
        boolean isExempt = line.equals("begin main {") ||
                           line.equals("}") ||
                           line.startsWith("func ") ||
                           insideFunction ||
                           line.startsWith("call ") ||
                           line.isEmpty() ||
                           line.startsWith("//") ||
                           FUNC_END.matcher(line).matches();

        if (!isExempt && (
                line.startsWith("number ") ||
                line.startsWith("Alphan ") ||
                line.startsWith("Alphan[") ||
                line.startsWith("number[") ||
                line.startsWith("publish("))) {

            System.err.println(" Syntax Error on line " + lineNumber + ": Missing semicolon at end of statement.");
            printLineTime(lineNumber, start, System.nanoTime());
            return;
        }
    }

    // 🟢 The rest of your analyze() code continues below...
    Matcher matcher;


        // Alphan string variable
        if (line.startsWith("Alphan")) {
            matcher = ALPHAN_VAR.matcher(line);
            if (matcher.matches()) {
                String varName = matcher.group(1);
                String value = matcher.group(2);
                strVars.put(varName, value);
                System.out.println("Line " + lineNumber + ": Variable '" + varName + "' assigned value \"" + value + "\"");
                printLineTime(lineNumber, start, System.nanoTime());
                return;
            }
        }

        // Number variable without arithmetic
        if (line.startsWith("number") && !line.contains("+") && !line.contains("-") && !line.contains("*") && !line.contains("/")) {
            matcher = NUMBER_VAR.matcher(line);
            if (matcher.matches()) {
                String varName = matcher.group(1);
                int value = Integer.parseInt(matcher.group(2));
                numVars.put(varName, value);
                System.out.println("Line " + lineNumber + ": Number variable '" + varName + "' assigned value " + value);
                printLineTime(lineNumber, start, System.nanoTime());
                return;
            }
        }

        matcher = STRING_ARRAY.matcher(line);
        if (matcher.matches()) {
            String name = matcher.group(1);
            String content = matcher.group(2);
            String[] items = content.split("\\s*,\\s*");
            List<String> values = new ArrayList<>();
            for (String item : items) {
                values.add(item.replace("\"", ""));
            }
            stringArrays.put(name, values);
            System.out.println("Line " + lineNumber + ": String array '" + name + "' declared with " + values.size() + " elements.");
            printLineTime(lineNumber, start, System.nanoTime());
            return;
        }

        matcher = NUMBER_ARRAY.matcher(line);
        if (matcher.matches()) {
            String name = matcher.group(1);
            String content = matcher.group(2);
            String[] items = content.split("\\s*,\\s*");
            List<Double> values = new ArrayList<>();
            try {
                for (String item : items) {
                    values.add(Double.parseDouble(item));
                }
                numberArrays.put(name, values);
                System.out.println("Line " + lineNumber + ": Number array '" + name + "' declared with " + values.size() + " elements.");
            } catch (NumberFormatException e) {
                System.out.println("Syntax Error on line " + lineNumber + ": Invalid number format.");
            }
            printLineTime(lineNumber, start, System.nanoTime());
            return;
        }
matcher = FUNC_DEF.matcher(line);
if (matcher.matches()) {
    insideFunction = true;
    currentFunction = matcher.group(1);
    String params = matcher.group(2).trim();
    List<String> paramList = params.isEmpty() ? new ArrayList<>() : Arrays.asList(params.split("\\s*,\\s*"));
    functionParams.put(currentFunction, paramList);
    currentFunctionLines = new ArrayList<>();
    System.out.println("Line " + lineNumber + ": Function '" + currentFunction + "' defined with parameters " + paramList);
    return;
}
if (insideFunction) {
    if (FUNC_END.matcher(line).matches()) {
        functionBody.put(currentFunction, currentFunctionLines);
        System.out.println("Line " + lineNumber + ": End of function '" + currentFunction + "'");
        insideFunction = false;
        currentFunction = null;
    } else {
        currentFunctionLines.add(line);
    }
    return;
}
matcher = FUNC_CALL.matcher(line);
if (matcher.matches()) {
    String funcName = matcher.group(1);
    String argsStr = matcher.group(2);
    List<String> args = Arrays.asList(argsStr.split("\\s*,\\s*"));

    if (!functionParams.containsKey(funcName)) {
        System.out.println("Error on line " + lineNumber + ": Function '" + funcName + "' not defined.");
        return;
    }

    List<String> paramList = functionParams.get(funcName);
    if (args.size() != paramList.size()) {
        System.out.println("Error on line " + lineNumber + ": Argument count mismatch for function '" + funcName + "'");
        return;
    }

    // Temporary environment for function execution
    Map<String, Integer> tempNumVars = new HashMap<>(numVars);
    Map<String, String> tempStrVars = new HashMap<>(strVars);

    // Assign parameters
    for (int i = 0; i < args.size(); i++) {
        String param = paramList.get(i).trim();
        String[] parts = param.split("\\s+");
        if (parts.length != 2) continue;
        String type = parts[0], name = parts[1];
        String arg = args.get(i);
        if (type.equals("number")) {
            int val = arg.matches("\\d+") ? Integer.parseInt(arg) : numVars.getOrDefault(arg, 0);
            numVars.put(name, val);
        } else if (type.equals("Alphan")) {
            String val = arg.replace("\"", "");
            strVars.put(name, val);
        }
    }

    System.out.println("Line " + lineNumber + ": Calling function '" + funcName + "' with arguments " + args);
    List<String> body = functionBody.get(funcName);
    for (String funcLine : body) {
        analyze(funcLine, lineNumber); // reuse same method
    }

    // Restore original variables after call
    numVars.clear();
    numVars.putAll(tempNumVars);
    strVars.clear();
    strVars.putAll(tempStrVars);

    return;
}

        // Access array element
        matcher = ARRAY_ACCESS.matcher(line);
        if (matcher.matches()) {
            String arrayName = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            if (stringArrays.containsKey(arrayName)) {
                List<String> values = stringArrays.get(arrayName);
                if (index < values.size()) {
                    System.out.println("Line " + lineNumber + ": Output → " + values.get(index));
                } else {
                    System.out.println("Error on line " + lineNumber + ": Index out of bounds for string array '" + arrayName + "'.");
                }
            } else if (numberArrays.containsKey(arrayName)) {
                List<Double> values = numberArrays.get(arrayName);
                if (index < values.size()) {
                    System.out.println("Line " + lineNumber + ": Output → " + values.get(index));
                } else {
                    System.out.println("Error on line " + lineNumber + ": Index out of bounds for number array '" + arrayName + "'.");
                }
            } else {
                System.out.println("Error on line " + lineNumber + ": Array '" + arrayName + "' not found.");
            }
            printLineTime(lineNumber, start, System.nanoTime());
            return;
        }

        // Arithmetic expression
        matcher = EXPRESSION_VAR.matcher(line);
        if (matcher.matches()) {
            String varName = matcher.group(1);
            String leftOperand = matcher.group(2);
            String operator = matcher.group(3);
            String rightOperand = matcher.group(4);

            if (!numVars.containsKey(leftOperand) || !numVars.containsKey(rightOperand)) {
                System.out.println("Error on line " + lineNumber + ": Undefined variable in expression.");
                printLineTime(lineNumber, start, System.nanoTime());
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
                        printLineTime(lineNumber, start, System.nanoTime());
                        return;
                    }
                    result = left / right;
                    break;
            }
            numVars.put(varName, result);
            System.out.println("Line " + lineNumber + ": Number variable '" + varName + "' assigned result " + result);
            printLineTime(lineNumber, start, System.nanoTime());
            return;
        }

        // Publish
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
                printLineTime(lineNumber, start, System.nanoTime());
                return;
            } else {
                System.out.println("Syntax Error on line " + lineNumber + ": Invalid publish statement.");
                printLineTime(lineNumber, start, System.nanoTime());
                return;
            }
        }

        // Default case
        System.out.println("Syntax Error on line " + lineNumber + ": Unrecognized statement → " + line);
        printLineTime(lineNumber, start, System.nanoTime());
    }

    private void printLineTime(int lineNumber, long start, long end) {
        double elapsedMs = (end - start) / 1_000_000.0;
        System.out.printf("Line %d processed in %.3f ms%n", lineNumber, elapsedMs);
    }

  
}
