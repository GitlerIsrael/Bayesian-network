import java.util.ArrayList;

public class Factor {
    private ArrayList<String> variables = new ArrayList<String>();
    private ArrayList<String> cpt = new ArrayList<String>();

    public Factor() {
    }

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

    public void setVariables(ArrayList<String> variables) {
        this.variables = variables;
    }

    public void addVariable(String variable) {
        this.variables.add(variable);
    }

    public ArrayList<String> getCpt() {
        return cpt;
    }

    public void setCpt(ArrayList<String> cpt) {
        this.cpt = cpt;
    }
}
