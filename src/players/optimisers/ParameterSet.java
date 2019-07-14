package players.optimisers;

import utils.Pair;

import java.text.DecimalFormat;
import java.util.*;

@SuppressWarnings("unchecked,Duplicates")
public interface ParameterSet {

    void setParameterValue(String param, Object value);
    Object getParameterValue(String root);
    ArrayList<String> getParameters();
    Map<String, Object[]> getParameterValues();
    Pair<String, ArrayList<Object>> getParameterParent(String parameter);
    Map<Object, ArrayList<String>> getParameterChildren(String root);
    Map<String, String[]> constantNames();

    default void translate(int[] values, boolean topLevel) {
        ArrayList<String> paramList = getParameters();
        Map<String, Object[]> params = getParameterValues();
        int k = 0;
        for (String param : paramList) {
            if (!topLevel || getParameterParent(param) == null) {   // Sets only top-level params if topLevel=false
                Object value = params.get(param)[values[k]];
                setParameterValue(param, value);
                k++;
            }
        }
    }

    /**
     * Interpret parameters with integer values from constants
     * @param parameter - parameter name
     * @param value - parameter value
     * @return - name of constant
     */
    default String interpret(String parameter, int value) {
        Map<String, String[]> constants = constantNames();
        if (constants.containsKey(parameter)) {
            return constants.get(parameter)[value];
        }
        return null;
    }

    default void printParameterSearchSpace() {
        Map<String, Object[]> params = getParameterValues();
        double searchSpaceSize = 1;
        for (String p : params.keySet()) {
            if (getParameterParent(p) == null) {
                searchSpaceSize *= printParameterValues(p, params, 0);
            }
        }
        DecimalFormat df = new DecimalFormat("0.000E0");
        System.out.println("Parameter space size: " + df.format(searchSpaceSize));
    }

    default int printParameterValues(String root, Map<String, Object[]> params, int depth) {
        Object[] valueArray = params.get(root);
        int searchSpaceSize = valueArray.length;
        StringBuilder values = new StringBuilder("[");
        if (!(valueArray[0] instanceof Integer) || interpret(root, (int)valueArray[0]) == null) {
            values = new StringBuilder(Arrays.toString(valueArray));
        } else {
            for (int i = 0; i < valueArray.length; i++) {
                values.append(interpret(root, (int) valueArray[i]));
                if (i < valueArray.length - 1) {
                    values.append(", ");
                }
            }
            values.append("]");
        }

        StringBuilder stringToPrint = new StringBuilder();
        for (int i = 0; i < depth * 4; i++) {
            stringToPrint.append(" ");
        }
        stringToPrint.append("- ").append(root).append(": ").append(values);
        System.out.println(stringToPrint);

        // Recursively print children
        Map<Object,ArrayList<String>> children = getParameterChildren(root);
        HashSet<String> childSet = new HashSet<>();
        for (Map.Entry e : children.entrySet()) {
            childSet.addAll((ArrayList<String>) e.getValue());
        }
        for (String c : childSet) {
            searchSpaceSize *= printParameterValues(c, params, depth + 1);
        }

        return searchSpaceSize;
    }

    default void printParameters() {
        Map<String, Object[]> params = getParameterValues();
        for (String p : params.keySet()) {
            if (getParameterParent(p) == null) {
                printParameter(p, 0);
            }
        }
    }

    default void printParameter(String root, int depth) {

        StringBuilder stringToPrint = new StringBuilder();
        for (int i = 0; i < depth * 4; i++) {
            stringToPrint.append(" ");
        }
        Object value = getParameterValue(root);
        if (value instanceof Integer) {
            String interpreted = interpret(root, (int)value);
            if (interpreted != null) value = interpreted;
        }
        stringToPrint.append("- ").append(root).append(": ").append(value);
        System.out.println(stringToPrint);

        // Recursively print children
        Map<Object,ArrayList<String>> children = getParameterChildren(root);
        HashSet<String> childSet = new HashSet<>();
        for (Map.Entry e : children.entrySet()) {
            childSet.addAll((ArrayList<String>) e.getValue());
        }
        for (String c : childSet) {
            printParameter(c, depth + 1);
        }
    }
}
