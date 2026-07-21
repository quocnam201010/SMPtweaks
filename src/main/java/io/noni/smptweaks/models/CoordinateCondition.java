package io.noni.smptweaks.models;

import org.bukkit.Location;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoordinateCondition {
    private static final Pattern PATTERN = Pattern.compile("^(x|y|z)\\s*(>=|<=|>|<|==|!=)\\s*([+-]?\\d+(?:\\.\\d+)?)$", Pattern.CASE_INSENSITIVE);
    
    private final String variable;
    private final String operator;
    private final double value;
    
    public CoordinateCondition(String variable, String operator, double value) {
        this.variable = variable.toLowerCase();
        this.operator = operator;
        this.value = value;
    }
    
    public static CoordinateCondition parse(String str) {
        if (str == null) return null;
        Matcher matcher = PATTERN.matcher(str.trim());
        if (matcher.matches()) {
            String variable = matcher.group(1);
            String operator = matcher.group(2);
            double value = Double.parseDouble(matcher.group(3));
            return new CoordinateCondition(variable, operator, value);
        }
        return null;
    }
    
    public boolean evaluate(Location loc) {
        if (loc == null) return false;
        double valToCheck;
        switch (variable) {
            case "x":
                valToCheck = loc.getX();
                break;
            case "y":
                valToCheck = loc.getY();
                break;
            case "z":
                valToCheck = loc.getZ();
                break;
            default:
                return false;
        }
        
        switch (operator) {
            case ">":
                return valToCheck > value;
            case ">=":
                return valToCheck >= value;
            case "<":
                return valToCheck < value;
            case "<=":
                return valToCheck <= value;
            case "==":
                return Math.abs(valToCheck - value) < 0.001;
            case "!=":
                return Math.abs(valToCheck - value) >= 0.001;
            default:
                return false;
        }
    }

    public String variable() {
        return variable;
    }

    public String operator() {
        return operator;
    }

    public double value() {
        return value;
    }
}
