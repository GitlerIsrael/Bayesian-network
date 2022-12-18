import java.util.ArrayList;

/**
 * factor class.
 */
public class Factor {
    private final ArrayList<String> variables;
    private final ArrayList<String> cpt;

    public Factor(ArrayList<String> variables, ArrayList<String> cpt){
        this.variables=variables;
        this.cpt=cpt;
    }


    public String toString() {
        // Returning attributes of Variable
        return "Factor Variables: " + this.variables.toString() + "\nCPT:  " + this.cpt.toString();
    }

    public ArrayList<String> getVariables() {
        return variables;
    }

    public ArrayList<String> getCpt() {
        return cpt;
    }

}
