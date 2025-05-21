import java.util.*;
import java.util.regex.*;

public class Parser {

   private static final Map<String, String> keywordMap = new HashMap<>();

static {
    keywordMap.put("print", "publish");
    keywordMap.put("start", "begin");
    keywordMap.put("end", "}");
    keywordMap.put("boolean", "truf");
    keywordMap.put("if", "suppose");
    keywordMap.put("new", "create");
    keywordMap.put("null", "empty");
    keywordMap.put("private", "internal");
    keywordMap.put("protected", "inheritable");
    keywordMap.put("public", "global");
    keywordMap.put("return", "giveback");
    keywordMap.put("short", "smallnum");
    keywordMap.put("static", "common");
    keywordMap.put("strictfp", "fixedfloat");
    keywordMap.put("super", "parent");
    keywordMap.put("switch", "decide");
    keywordMap.put("this", "self");
    keywordMap.put("throw", "raise");
    keywordMap.put("throws", "maygive");
    keywordMap.put("transient", "skipstore");
    keywordMap.put("try", "attemptblock");
    keywordMap.put("void", "nothing");
    keywordMap.put("volatile", "instable");
    keywordMap.put("while", "loopwhile");
    keywordMap.put("sealed", "lockedclass");
    keywordMap.put("permits", "allows");
}

    public static Map<String, String> getKeywordMap() {
        return keywordMap;
    }

    private final Map<String, List<String>> functionBodyMap = new HashMap<>();
    private final Map<String, List<String>> functionParams = new HashMap<>();
    private boolean insideFunction = false;
    private String currentFunction = "";
    private final List<String> currentFunctionBody = new ArrayList<>();
private Map<String, Boolean> globalBoolVars = new HashMap<>();

    private final Map<String, List<Double>> numberArrays = new HashMap<>();
    private final Map<String, List<String>> stringArrays = new HashMap<>();

    // Global variables
    private Map<String, Double> globalNumVars = new HashMap<>();
    private Map<String, String> globalStrVars = new HashMap<>();


    public void analyzeLine(String line) {
        analyzeLine(line, -1);
        
    }

    public void analyzeLine(String line, int lineNumber) {
        line = line.trim();
        if (lineNumber != -1 && !line.matches("/\\*.*\\*/")) {
            System.out.println("[Line " + lineNumber + "] " + line);
        }

        //  Skip comments
        if (line.startsWith("/\\") && line.endsWith("/\\")) return;

        //  Function support
        if (line.startsWith("func ")) {
            parseFunctionDefinition(line, lineNumber);
            insideFunction = true;
            return;
        }

        if (line.equals("}")) {
            if (insideFunction) {
                functionBodyMap.put(currentFunction, new ArrayList<>(currentFunctionBody));
                currentFunctionBody.clear();
                insideFunction = false;
                System.out.println("Function '" + currentFunction + "' body saved.");
                currentFunction = "";
            }
            return;
        }

        if (insideFunction) {
            currentFunctionBody.add(line);
            return;
        }

        if (line.startsWith("call ")) {
            callFunction(line, lineNumber);
            return;
        }

        // ✅ Parse number variable assignment
        Matcher m = Pattern.compile("number\\s+(\\w+)\\s*=\\s*(\\d+(\\.\\d+)?);").matcher(line);
        if (m.matches()) {
            String var = m.group(1);
            double val = Double.parseDouble(m.group(2));
            globalNumVars.put(var, val);
            System.out.println("Assigned: " + var + " = " + val);
            return;
        }

        // ✅ Parse arithmetic expression assignment
        m = Pattern.compile("number\\s+(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-*/])\\s*(\\w+);").matcher(line);
        if (m.matches()) {
            String var = m.group(1);
            Double left = getValue(m.group(2), globalNumVars);
            Double right = getValue(m.group(4), globalNumVars);
            String op = m.group(3);

            if (left == null || right == null) {
                System.out.println("Error: Undefined variable in expression.");
                return;
            }

            double result = switch (op) {
                case "+" -> left + right;
                case "-" -> left - right;
                case "*" -> left * right;
                case "/" -> right != 0 ? left / right : Double.NaN;
                default -> 0;
            };

            globalNumVars.put(var, result);
            System.out.println("Assigned: " + var + " = " + result);
            return;
        }

        // ✅ Parse number array assignment
        m = Pattern.compile("number\\[\\]\\s+(\\w+)\\s*=\\s*\\{([^}]*)\\};").matcher(line);
        if (m.matches()) {
            String name = m.group(1);
            String[] nums = m.group(2).split(",");
            List<Double> list = new ArrayList<>();
            for (String n : nums) list.add(Double.valueOf(n.trim()));
            numberArrays.put(name, list);
            System.out.println("Stored array '" + name + "' = " + list);
            return;
        }

        // Parse string array assignment
        m = Pattern.compile("Alphan\\[\\]\\s+(\\w+)\\s*=\\s*\\{\\s*\"([^\"]+)\"(?:\\s*,\\s*\"([^\"]+)\")*\\s*\\};").matcher(line);
        if (line.startsWith("Alphan[")) {
            String[] parts = line.split("=", 2);
            String name = parts[0].replace("Alphan[]", "").trim();
            String content = parts[1].replaceAll("[{};]", "").trim();
            String[] strings = content.split(",");
            List<String> list = new ArrayList<>();
            for (String s : strings) list.add(s.trim().replace("\"", ""));
            stringArrays.put(name, list);
            System.out.println("Stored string array '" + name + "' = " + list);
            return;
        }

        // ✅ Handle publish(variable)
        m = Pattern.compile("publish\\((\\w+)\\);").matcher(line);
        if (m.matches()) {
            String var = m.group(1);
            if (globalNumVars.containsKey(var)) {
                System.out.println("Output → " + globalNumVars.get(var));
            } else {
                System.out.println("Error: Variable '" + var + "' not found.");
            }
            return;
        }

        // Handle publish(array[index])
        m = Pattern.compile("publish\\((\\w+)\\[(\\d+)\\]\\);").matcher(line);
        if (m.matches()) {
            String arr = m.group(1);
            int index = Integer.parseInt(m.group(2));
            if (numberArrays.containsKey(arr)) {
                List<Double> list = numberArrays.get(arr);
                if (index < list.size()) {
                    System.out.println("Output : " + list.get(index));
                } else {
                    System.out.println("Error: Index out of bounds for array '" + arr + "'");
                }
            } else if (stringArrays.containsKey(arr)) {
                List<String> list = stringArrays.get(arr);
                if (index < list.size()) {
                    System.out.println("Output : " + list.get(index));
                } else {
                    System.out.println("Error: Index out of bounds for array '" + arr + "'");
                }
            } else {
                System.out.println("Error: Array '" + arr + "' not found.");
            }
        }

     //   System.out.println("Syntax Error or unrecognized line: " + line);
    }

    private void parseFunctionDefinition(String line, int lineNumber) {
        Pattern pattern = Pattern.compile("func\\s+(\\w+)\\s*\\((.*?)\\)\\s*\\{");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            currentFunction = matcher.group(1).trim();
            String paramsStr = matcher.group(2).trim();
            List<String> params = new ArrayList<>();
            if (!paramsStr.isEmpty()) {
                for (String p : paramsStr.split(",")) {
                    params.add(p.trim());
                }
            }
            functionParams.put(currentFunction, params);
            System.out.println("Function '" + currentFunction + "' defined with parameters " + params);
        } else {
            System.out.println("Syntax Error at line " + lineNumber + ": Invalid function declaration -> " + line);
        }
    }

    private void callFunction(String line, int lineNumber) {
        Pattern pattern = Pattern.compile("call\\s+(\\w+)\\s*\\((.*?)\\);");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String funcName = matcher.group(1);
            String argsStr = matcher.group(2);
            List<String> args = new ArrayList<>();
            if (!argsStr.isEmpty()) {
                for (String a : argsStr.split(",")) {
                    args.add(a.trim());
                }
            }
            executeFunction(funcName, args, lineNumber);
        } else {
            System.out.println("Syntax Error at line " + lineNumber + ": Invalid call statement -> " + line);
        }
    }

    private void executeFunction(String funcName, List<String> args, int lineNumber) {
        if (!functionBodyMap.containsKey(funcName)) {
            System.out.println("Error at line " + lineNumber + ": Function '" + funcName + "' not found.");
            return;
        }

        List<String> params = functionParams.get(funcName);
        if (params == null || args.size() != params.size()) {
            System.out.println("Error at line " + lineNumber + ": Parameter count mismatch for function '" + funcName + "'");
            return;
        }

        Map<String, Double> localNumVars = new HashMap<>();
        for (int i = 0; i < params.size(); i++) {
            String param = params.get(i).split(" ")[1];
            try {
                double val = Double.parseDouble(args.get(i));
                localNumVars.put(param, val);
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid number '" + args.get(i) + "'");
                return;
            }
        }

        for (String line : functionBodyMap.get(funcName)) {
            executeFunctionLine(line, localNumVars);
        }
    }

    private void executeFunctionLine(String line, Map<String, Double> localNumVars) {
        line = line.trim();

        Pattern assign = Pattern.compile("number\\s+(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-*/])\\s*(\\w+);");
        Matcher matcher = assign.matcher(line);
        if (matcher.find()) {
            String var = matcher.group(1);
            Double left = getValue(matcher.group(2), localNumVars);
            Double right = getValue(matcher.group(4), localNumVars);
            String op = matcher.group(3);

            if (left == null || right == null) {
                System.out.println("Error: Undefined variable in expression.");
                return;
            }

            double result = switch (op) {
                case "+" -> left + right;
                case "-" -> left - right;
                case "*" -> left * right;
                case "/" -> right != 0 ? left / right : Double.NaN;
                default -> 0;
            };

            localNumVars.put(var, result);
            System.out.println("Assigned: " + var + " = " + result);
            return;
        }

        Pattern pub = Pattern.compile("publish\\s+(\\w+);");
        matcher = pub.matcher(line);
        if (matcher.find()) {
            String var = matcher.group(1);
            Double val = localNumVars.get(var);
            if (val != null) {
                System.out.println("Output : " + val);
            } else {
                System.out.println("Error: Variable '" + var + "' not found.");
            }
            return;
        }

        System.out.println("Syntax Error: Unrecognized line in function: " + line);
    }

    private Double getValue(String var, Map<String, Double> numVars) {
        if (numVars.containsKey(var)) return numVars.get(var);
        return parseDoubleOrNull(var);
    }

    private Double parseDoubleOrNull(String str) {
        try {
            return Double.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        Parser parser = new Parser();
        String[] program = {
            "number x = 10;",
            "number y = 5;",
            "number sum = x + y;",
            "publish(sum);",
            "number[] scores = {98, 85.5, 74, 90};",
            "publish(scores[2]);",
            "Alphan[] names = { \"Ali\", \"Sara\", \"Zara\" };",
            "publish(names[1]);"
        };
        int lineNumber = 1;
        for (String line : program) {
            parser.analyzeLine(line, lineNumber++);
        }
    }
}
