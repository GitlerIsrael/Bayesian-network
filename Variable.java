import java.util.ArrayList;

public class Variable {
    private String var_name = null;
    private ArrayList<String> outcomes = new ArrayList<String>();
    private ArrayList<String> parents = new ArrayList<String>();
    private ArrayList<String> cpt = new ArrayList<String>();

    public Variable() {
    }

    public Variable(String name, ArrayList<String> outcomes, ArrayList<String> parents, ArrayList<String> cpt){
        this.var_name=name;
        this.outcomes=outcomes;
        this.parents=parents;
        this.cpt=cpt;
    }


    public String toString() {
        // Returning attributes of Variable
        return "Variable: " + this.var_name + "\nValues: " + this.outcomes.toString() + "\nParents: "
                + this.parents.toString() + "\nCPT:  " + this.cpt.toString();

    }

    public String getVar_name() {
        return var_name;
    }

    public void setVar_name(String var_name) {
        this.var_name = var_name;
    }

    public ArrayList<String> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(ArrayList<String> outcomes) {
        this.outcomes=outcomes;
    }

    public void addOutcome(String value) {
        this.outcomes.add(value);
    }

    public ArrayList<String> getParents() {
        return parents;
    }

    public void setParents(ArrayList<String> parents) {
        this.parents=parents;
    }

    public void addParent(String parent) {
        this.parents.add(parent);
    }

    public ArrayList<String> getCpt() {
        return cpt;
    }

    public void setCpt(String cpt) {
        String[] c = cpt.split(" ", 0);
        for(int i=0; i<c.length; i++){
            this.cpt.add(c[i]);
        }
    }

    public void replaceCpt(ArrayList<String> cpt){
        this.cpt=cpt;
    }
}