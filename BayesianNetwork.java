import java.io.*;
import java.text.DecimalFormat;
import java.util.*;


public class BayesianNetwork {
    /**
     * The function read the text file with the queries and calculate them.
     * Finally, put it in text output file and return it.++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * @throws FileNotFoundException throws this exception.
     */
    public static void calc() throws FileNotFoundException {
        File inputFile = new File("input.txt");//input file
        File outputFile = new File("output.txt");//output file+++++++++++++++++++++++++++++++++++
        FileWriter fw;
        try {
            fw = new FileWriter(outputFile.getAbsoluteFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BufferedWriter bw = new BufferedWriter(fw);
        Scanner sc = new Scanner(inputFile);
        String xmlFile = sc.nextLine();
        HashMap<String, Variable> originalData= XMLParser.Parser(xmlFile);
        while (sc.hasNextLine()) {
            HashMap<String, Variable> d = copy(originalData);
            String line = sc.nextLine();
//            System.out.println(line);
            String query = line.substring(2,line.length()-3);
            //check if answer for query already exist in one of the cpt tables and return it if it does.
            //first, we will create a list of Strings which will hold all evidence variables
            // and String object which will hold the query variable.
            //then, we will check if the evidence variables are the all the query variable parents exactly.
            // in this situation we can take out the query answer from its cpt table. otherwise
            //we will calculate the answer using the algorithms.
            ArrayList<String> evidencesVars = new ArrayList<>();
            ArrayList<String> evidencesVals = new ArrayList<>();
            String[] questionArray = query.split("\\|", -1);
            String[] queryArray = questionArray[0].split("=", -1);
            String queryVar = queryArray[0];
            String queryVal = queryArray[1];
            String[] evidenceArray = questionArray[1].split(",", -1);
            for (String evidenceData : evidenceArray) {
                String[] variableAndValue = evidenceData.split("=");
                evidencesVars.add(variableAndValue[0]);
                evidencesVals.add(variableAndValue[1]);
            }
            // check if the query answer already exist in query variable cpt. if yes, take the answer
            //immediately. else, send query to relevant algorithm.
            ArrayList<String> queryVarPar = d.get(queryVar).getParents();
            if(queryVarPar.size()>0 && evidencesVars.containsAll(queryVarPar)) {
                String answer = String.valueOf(cptval(queryVar, queryVal, evidencesVars, evidencesVals, d));
//                System.out.println(answer+",0,0");
                try {
                    bw.write(answer+",0,0");
                    if(sc.hasNextLine()) bw.write("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            else{
                char alg = line.charAt(line.length()-1);
                ////write to file for each condition
                if(alg=='1') {
                    try {
                        bw.write(one(query, d));
                        if(sc.hasNextLine()) bw.write("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else if(alg=='2') {
                    try {
                        bw.write(two(query, d));
                        if(sc.hasNextLine()) bw.write("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else if(alg=='3') {
                    try {
                        bw.write(three(query, d));
                        if(sc.hasNextLine()) bw.write("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        try {
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * the function return a deep copy of the bayesian network hashmap.
     * @param original the original hashmap.
     * @return a copied hashmap.
     */
    private static HashMap<String, Variable> copy(HashMap<String, Variable> original)
    {
        HashMap<String, Variable> copy = new HashMap<>();
        for (Map.Entry<String, Variable> entry : original.entrySet())
        {
            copy.put(entry.getKey(), new Variable(entry.getValue().getVar_name(),
                    new ArrayList<>(entry.getValue().getOutcomes()),
                    new ArrayList<>(entry.getValue().getParents()),
                    new ArrayList<>(entry.getValue().getCpt())));
        }
        return copy;
    }


    /**
     * The function get a query string such - "B=T|M=T,J=T" and return a hashmap
     * which holds this data- "{B:T,M:T,J:T}".
     * @param query string of query.
     * @return hashmap of query data.
     */
    private static HashMap<String, String> querySplit (String query){
        HashMap<String, String> map = new HashMap<>();
        String[] queryArray = query.split("[|,]", -1);
        for (String data : queryArray) {
            String[] keyValue = data.split("=");
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
    }


    /**
     * The function return all the options of outcomes for a list of variables.
     * @param data hashmap which holds all the variables data.
     * @param keys the variables which we want to get all their options (the hidden variables).
     * @return a set of all optional outcomes combinations.
     */
    private static Set<List<String>> allCombinations (HashMap<String, Variable> data, Object[] keys){
        Set<List<String>> combinations = new HashSet<>();
        Set<List<String>> newCombinations;

        int index = 0;
        String firstVarOut=(String) keys[0];
        for (String i : (data.get(firstVarOut)).getOutcomes()) {
            List<String> newList = new ArrayList<>();
            newList.add(i);
            combinations.add(newList);
        }
        index++;
        while (index < keys.length) {
            String nextVarOut=(String) keys[index];
            List<String> nextList = (data.get(nextVarOut)).getOutcomes();
            newCombinations = new HashSet<>();
            for (List<String> first : combinations) {
                for (String second : nextList) {
                    List<String> newList = new ArrayList<>(first);
                    newList.add(second);
                    newCombinations.add(newList);
                }
            }
            combinations = newCombinations;
            index++;
        }
        return combinations;
    }


    /**
     * The function returns the value of a specific row in the variable cpt table.
     * @param v a string of the variable.
     * @param outcome The variable wanted outcome.
     * @param vars an arraylist of all the variables.
     * @param outcomes an arraylist of an outcomes combination.
     * @param d hashmap which holds all variables data.
     * @return the value of the relevant row in the cpt table.
     */
    private static double cptval(String v, String outcome, ArrayList<String> vars, ArrayList<String> outcomes, HashMap<String, Variable> d){
        int rowNum = 1;
        Variable var = d.get(v);
        rowNum *= var.getOutcomes().size();
        for(String p:var.getParents()){
            Variable parent = d.get(p);
            rowNum*=parent.getOutcomes().size();
        }

        int myIndex=0;
        if(var.getParents().size()>0){
            int rowCount = rowNum;
            for(String p:var.getParents()){
                Variable parent = d.get(p);
                int pIndex = vars.indexOf(parent.getVar_name());
                String pOutcome = outcomes.get(pIndex);
                int pOutcomeIndex = parent.getOutcomes().indexOf(pOutcome);
                int pOutcomeNum = parent.getOutcomes().size();
                int temp = rowCount / pOutcomeNum;
                rowCount = rowCount / pOutcomeNum * pOutcomeIndex;
                myIndex += rowCount;
                rowCount=temp;
            }
        }
        myIndex += var.getOutcomes().indexOf(outcome);
        return Double.parseDouble(var.getCpt().get(myIndex));
    }


    /**
     * The function makes the calculation of the first algorithm.
     * @param query string of query.
     * @param d hashmap which holds all variables data.
     * @return the answer of query, plus and multiply operations counters.
     */
    private static String one(String query, HashMap<String, Variable> d) {
        //splitting the query string.
        HashMap<String, String> split = querySplit(query);
        Object[] allKeys= d.keySet().toArray();
        ArrayList<String> hiddenArrList = new ArrayList<>();
        for(Object key: allKeys){
            String sKey= (String) key;
            if (split.get(sKey)==null){
                hiddenArrList.add((String) key);
            }
        }
        Object[] keys = hiddenArrList.toArray();
        Set<List<String>> allCombinations = allCombinations(d, keys);
        ArrayList<String> orderOfVars = new ArrayList<>(hiddenArrList);
        orderOfVars.addAll(split.keySet());
        for(List<String> l: allCombinations){
            l.addAll(split.values());
        }

        //casting to arraylists for cptVal func. need to send to it this data for calc.
        ArrayList<ArrayList<String>> combsArr = new ArrayList<>();
        for (List<String> l : allCombinations) {
            ArrayList<String> comb = new ArrayList<>(l);
            combsArr.add(comb);
        }

        double numeratorSum=0;
        for(ArrayList<String> arr:combsArr){
            double innerSum=1;
            for(String var: orderOfVars){
                int varIndex = orderOfVars.indexOf(var);
                String varOutcome = arr.get(varIndex);
                double rowVal = cptval(var, varOutcome, orderOfVars, arr, d);
                innerSum=innerSum*rowVal;
            }
            numeratorSum+=innerSum;
        }

        //  do the same for the denominator.
        //first, find query variable-
        String[] questionArray = query.split("\\|", -1);
        String[] queryArray = questionArray[0].split("=", -1);
        String queryVar = queryArray[0];
        hiddenArrList.add(queryVar);
        Object[] denominatorKeys = hiddenArrList.toArray();
        Set<List<String>> denominatorAllCombinations = allCombinations(d, denominatorKeys);
        split.remove(queryVar);
        ArrayList<String> orderOfVarsDenominator = new ArrayList<>();
        orderOfVarsDenominator.addAll(hiddenArrList);
        orderOfVarsDenominator.addAll(split.keySet());
        for(List<String> l: denominatorAllCombinations) {
            l.addAll(split.values());
        }
        //casting to arraylists for cptCol func. need to send to it this data for calc.
        ArrayList<ArrayList<String>> denominatorCombsArr = new ArrayList<>();
        for (List<String> l : denominatorAllCombinations) {
            ArrayList<String> denominatorComb = new ArrayList<>(l);
            denominatorCombsArr.add(denominatorComb);
        }

        double sum=0;
        int plus=0;
        int mult=0;
        for(ArrayList<String> denominatorArr:denominatorCombsArr){
            double innerSum=1;
            for(String var: orderOfVarsDenominator){
                int varIndex = orderOfVarsDenominator.indexOf(var);
                String varOutcome = denominatorArr.get(varIndex);
                double rowVal = cptval(var, varOutcome, orderOfVarsDenominator, denominatorArr, d);
                innerSum =innerSum*rowVal;
                mult+=1;
            }

            mult-=1;
            sum+=innerSum;
            plus+=1;
        }
        plus-=1;
        DecimalFormat df = new DecimalFormat("#.#####");
        String answer = String.valueOf(df.format(numeratorSum/sum));

        return (answer+","+plus+","+mult);
    }


    /**
     * returns the ascii value of factor variables.
     * @param factor factor.
     * @return ascii values of factor variables.
     */
    private static int ascii(Factor factor) {
        ArrayList<String> arr = factor.getVariables();
        int sum=0;
        for (String str:arr){
            for(int i=0; i<str.length(); i++) {
                int asciiValue = str.charAt(i);
                sum = sum+ asciiValue;
            }
        }
       return sum;
    }


    /**
     * the function gets 2 factors and multiply them by relevant rows.
     * @param a first factor.
     * @param b second factor.
     * @param outcomes list of all optional outcomes.
     * @param multSum counter of multiply operations.
     * @return new factor that created from multiplying.
     */
    private static Factor multiplyFactors(Factor a, Factor b, HashMap<String, ArrayList<String>> outcomes, int[] multSum){
        ArrayList<String> varsA = a.getVariables();
        ArrayList<String> varsB = b.getVariables();
        Set<String> set = new HashSet<>();
        set.addAll(varsA);
        set.addAll(varsB);
        ArrayList<String> newFactorVars= new ArrayList<>(set);
        ArrayList<String> newFactorCpt = new ArrayList<>();

        ArrayList<Integer> lengths = new ArrayList<>();
        for(String var:newFactorVars){
            lengths.add(outcomes.get(var).size());
        }
        //transform variables to numbers
        HashMap<String, Integer> dict = new HashMap<>();
        for(int i=0;i<newFactorVars.size();i++){
            dict.put(newFactorVars.get(i), i);
        }

        int[] values = new int[newFactorVars.size()];
        Arrays.fill(values, -1);

        int k = 0;
        while (k >= 0) {
            if (k == lengths.size()) {

                int indexInA = 0;
                int indexInB = 0;

                int jumpA = 1;
                for (int i = varsA.size() - 1; i >= 0; i--) {
                    indexInA += values[dict.get(varsA.get(i))] * jumpA;
                    jumpA *= lengths.get(dict.get(varsA.get(i)));
                }

                int jumpB = 1;
                for (int i = varsB.size() - 1; i >= 0; i--) {
                    indexInB += values[dict.get(varsB.get(i))] * jumpB;
                    jumpB *= lengths.get(dict.get(varsB.get(i)));
                }

                //
                double aVal = Double.parseDouble(a.getCpt().get(indexInA));
                double bVal = Double.parseDouble(b.getCpt().get(indexInB));
                newFactorCpt.add(String.valueOf(aVal * bVal));
                multSum[0]+=1;

                //
                k--;
            } else if (values[k] == lengths.get(k) - 1) {
                values[k] = -1;
                k--;
            } else {
                values[k]++;
                k++;
            }
        }
        return new Factor(newFactorVars, newFactorCpt);
    }


    /**
     * eliminate variable from factor.
     * @param factor a factor.
     * @param outcomes list of all optional outcomes.
     * @param hidden the variable to eliminate.
     * @param plusSum counter of plus operations.
     * @return new eliminated factor.
     */
    private static Factor eliminate(Factor factor, HashMap<String, ArrayList<String>> outcomes, String hidden, int[] plusSum) {
        ArrayList<String> newFactorVars = new ArrayList<>(factor.getVariables());
        newFactorVars.remove(hidden);
        ArrayList<String> newFactorCpt = new ArrayList<>();

        int thisJump=outcomes.get(hidden).size();
        int beforeJump=1;
        for(int i=factor.getVariables().size()-1;i>factor.getVariables().indexOf(hidden);i--){
            beforeJump*=outcomes.get(factor.getVariables().get(i)).size();
        }
        for(int i=0; i<factor.getCpt().size(); i+=beforeJump*thisJump){
            for(int j=0; j<beforeJump; j++){
                double cptValue=0;
                for(int k=0; k<thisJump;k++){
                    int currIndex=i+j+(k*(beforeJump));
                    cptValue += Double.parseDouble(factor.getCpt().get(currIndex));
                    plusSum[0]+=1;
                }
                // because in the beginning cptValue was 0 and I count plus operation for adding the first value-
                //I will decrease it by this one plus operation.
                plusSum[0]-=1;
                newFactorCpt.add(String.valueOf(cptValue));
            }
        }
        return new Factor(newFactorVars, newFactorCpt);
    }


    /**
     * variable elimination algorithm.
     * @param query the query.
     * @param d a hashmap which holds the bayesian network.
     * @return the answer of query, plus and multiply operations counters.
     */
    public static String two(String query, HashMap<String, Variable> d) {
        HashMap<String, String> evidences = new HashMap<>();
        //splitting query string.
        String[] questionArray = query.split("\\|", -1);
        String[] queryArray = questionArray[0].split("=", -1);
        String queryVar = queryArray[0];
        String queryVal = queryArray[1];
        String[] evidenceArray = questionArray[1].split(",", -1);
        for (String data : evidenceArray) {
            String[] keyValue = data.split("=");
            evidences.put(keyValue[0], keyValue[1]);
        }
        //delete from factors every variable that is not an ancestor of a query variable or evidence
        // variable because it irrelevant to the query.

        //first, we will make a set of all relevant variables:
        ArrayList<String> relevantVars = new ArrayList<>();
        relevantVars.add(queryVar);
        relevantVars.addAll(evidences.keySet());
        Queue<String> queue = new LinkedList<>(relevantVars);
        while(!queue.isEmpty()) {
            for (String parent : d.get(queue.poll()).getParents()) {
                queue.add(parent);
                relevantVars.add(parent);
            }
        }

        HashMap<String, Variable> factors = new HashMap<>();
        for(String var:d.keySet()){
            if(relevantVars.contains(var)) factors.put(var, new Variable(d.get(var).getVar_name(),
                    new ArrayList<>(d.get(var).getOutcomes()),
                    new ArrayList<>(d.get(var).getParents()),
                    new ArrayList<>(d.get(var).getCpt())));
        }

        ArrayList<String> hidden = new ArrayList<>();
        for(String key: d.keySet()){
            if(evidences.get(key)==null && !key.equals(queryVar) && relevantVars.contains(key)){
                hidden.add(key);
            }
        }
        Collections.sort(hidden); ///sort by abc.

        //create an hashmap of all optional outcomes:
        HashMap<String,ArrayList<String>> allOutcomes=new HashMap<>();
        for(Variable factor: factors.values()){
            allOutcomes.put(factor.getVar_name(), d.get(factor.getVar_name()).getOutcomes());
            //delete rows from each factor by evidence information and push the new data to factors.
            boolean erased=false; // if var name was erased or not
            for (Map.Entry<String, String> entry : evidences.entrySet()) {
                String evidenceVarName = entry.getKey();
                String evidenceOutcome = entry.getValue();
                int cptLen=factor.getCpt().size();
                int evidenceOutIndex=d.get(evidenceVarName).getOutcomes().indexOf(evidenceOutcome);
                int outcomesNum=factor.getOutcomes().size();
                if(factor.getVar_name().equals(evidenceVarName)){
                    ArrayList<String> newCpt=new ArrayList<>();
                    int pointer=evidenceOutIndex;
                    while(pointer<cptLen){
                        newCpt.add(factor.getCpt().get(pointer));
                        pointer+=outcomesNum;
                    }

                    factors.get(factor.getVar_name()).replaceCpt(newCpt);
                    erased=true;
                }
                else{
                    for(String p:factor.getParents()){
                        Variable parent=d.get(p);
                        if(parent.getVar_name().equals(evidenceVarName)){
                            int outChange=factor.getOutcomes().size();
                            if(erased) outChange=1;
                            int parentOutNum = parent.getOutcomes().size();
                            List<String> factParents = factor.getParents();
                            for (int i=factParents.size()-1; i>=0; i--) {
                                if(factParents.get(i).equals(p)) break;
                                int inParentOutNum=factors.get(factParents.get(i)).getOutcomes().size();
                                outChange*=inParentOutNum;
                            }
                            ArrayList<String> newCpt=new ArrayList<>();
                            int pointer=evidenceOutIndex*outChange;
                            while(pointer<cptLen){
                                for(int i=0; i<outChange;i++) {
                                    newCpt.add(factor.getCpt().get(pointer+i));
                                }
                                pointer+=outChange*parentOutNum;
                            }
                            ArrayList<String> parents = factors.get(factor.getVar_name()).getParents();
                            ArrayList<String> newParents = new ArrayList<>();
                            for(String par:parents){
                                if(!par.equals(p)) newParents.add(par);
                            }
                            factors.get(factor.getVar_name()).setParents(newParents);
                            factors.get(factor.getVar_name()).replaceCpt(newCpt);
                        }
                    }
                }
                ArrayList<String> newOutcomes=new ArrayList<>();
                newOutcomes.add(evidenceOutcome);
                allOutcomes.put(evidenceVarName, newOutcomes);
            }
        }

        //transfer factors to arraylist of Factor class objects which holds the factor variables and its cpt table.
        //from now I will use myFactors ArrayList and allOutcomes HashMap.
        ArrayList<Factor> myFactors = new ArrayList<>();
        for(Variable factor:factors.values()){
            boolean flag=true;
            for (String evidence : evidences.keySet()) {
                if (factor.getVar_name().equals(evidence)) {
                    flag = false;
                    break;
                }
            }
            //check that the factor has more than one row:
            if(factor.getCpt().size()>1) {
                ArrayList<String> parentsPlusName = new ArrayList<>(factor.getParents());
                if(flag) parentsPlusName.add(factor.getVar_name());
                Factor f=new Factor(parentsPlusName, factor.getCpt());
                myFactors.add(f);
                factor.setVar_name(null);
            }
        }

        int[] plusSum=new int[1];
        int[] multSum=new int[1];
        for(String variable:hidden){
            ArrayList<Factor> relevantFactors = new ArrayList<>();
            for(Factor factor:myFactors){
                if(factor.getVariables().contains(variable)){
                    relevantFactors.add(factor);
                }
            }
            for(Factor factor:relevantFactors){
                myFactors.remove(factor);
            }
            //sort the factors to multiply by size order. if size is equal sort by sum of ascii values.
            // used lambda instead of compare.
            relevantFactors.sort((x, y) -> {
                if (x.getCpt().size() == y.getCpt().size()) return ascii(x) - ascii(y);
                else return x.getCpt().size() - y.getCpt().size();
            });

            Factor product = relevantFactors.get(0);
            for (int i = 1; i < relevantFactors.size(); i++) {
                product = multiplyFactors(product, relevantFactors.get(i), allOutcomes, multSum);
            }

            if(product.getVariables().size()>1){
                product = eliminate(product, allOutcomes, variable, plusSum);
                myFactors.add(product);
            }
        }


        Factor finalProduct;
        if(myFactors.size()>1){
            finalProduct = myFactors.get(0);
            for (int i = 1; i < myFactors.size(); i++) {
                finalProduct = multiplyFactors(finalProduct, myFactors.get(i), allOutcomes, multSum);
            }
        }
        else {
            finalProduct = myFactors.get(0);
        }

        //find sum of all cpt for normalization.
        double sumOfCpt = 0;
        for(String value:finalProduct.getCpt()){
            sumOfCpt+=Double.parseDouble(value);
            plusSum[0]+=1;
        }
        //I added 1 one time more than needed because sumOfCpt started from 0.
        plusSum[0]-=1;
        ArrayList<String> newCpt=new ArrayList<>();
        for(String value:finalProduct.getCpt()){
            newCpt.add(String.valueOf((Double.parseDouble(value)/sumOfCpt)));
        }
        String returnVal = newCpt.get(allOutcomes.get(queryVar).indexOf(queryVal));
        DecimalFormat df = new DecimalFormat("#.#####");
        String answer = String.valueOf(df.format(Double.parseDouble(returnVal)));

        return(answer+","+plusSum[0]+","+multSum[0]);
    }


    /**
     * the function calculate the number of variable neighbors.
     * @param hidden the variable we want to count its neighbors.
     * @param d a hashmap which holds the bayesian network.
     * @return number of variable neighbors.
     */
    private static int neighborsCounter(String hidden, HashMap<String, Variable> d){
        int counter = 0;
        for(Variable v:d.values()){
            if(v.getParents().contains(hidden)) counter++;
        }
        counter += d.get(hidden).getParents().size();
        return counter;
    }


    /**
     * variable elimination algorithm with heuristic way to order the variables elimination.
     * @param query the query.
     * @param d a hashmap which holds the bayesian network.
     * @return the answer of query, plus and multiply operations counters.
     */
    private static String three(String query, HashMap<String, Variable> d) {
        //splitting the query string.
        HashMap<String, String> evidences = new HashMap<>();
        String[] questionArray = query.split("\\|", -1);
        String[] queryArray = questionArray[0].split("=", -1);
        String queryVar = queryArray[0];
        String queryVal = queryArray[1];
        String[] evidenceArray = questionArray[1].split(",", -1);
        for (String data : evidenceArray) {
            String[] keyValue = data.split("=");
            evidences.put(keyValue[0], keyValue[1]);
        }
        //delete from factors every variable that is not an ancestor of a query variable or evidence
        // variable because it irrelevant to the query.

        //first, we will made a set of all relevant variables:
        ArrayList<String> relevantVars = new ArrayList<>();
        relevantVars.add(queryVar);
        relevantVars.addAll(evidences.keySet());
        Queue<String> queue = new LinkedList<>(relevantVars);
        while(!queue.isEmpty()) {
            for (String parent : d.get(queue.poll()).getParents()) {
                queue.add(parent);
                relevantVars.add(parent);
            }
        }

        HashMap<String, Variable> factors = new HashMap<>();
        for(String var:d.keySet()){
            if(relevantVars.contains(var)) factors.put(var, new Variable(d.get(var).getVar_name(),
                    new ArrayList<>(d.get(var).getOutcomes()),
                    new ArrayList<>(d.get(var).getParents()),
                    new ArrayList<>(d.get(var).getCpt())));
        }

        ArrayList<String> hidden = new ArrayList<>();
        for(String key: d.keySet()){
            if(evidences.get(key)==null && !key.equals(queryVar) && relevantVars.contains(key)){
                hidden.add(key);
            }
        }
        // the heuristic order for elimination- first sort by number of neighbors, ascending order. then, if
        //it equals,sort by cpt table size, descending order.
        hidden.sort((x, y) -> {
            if (neighborsCounter(x, d) == neighborsCounter(y, d)){
                return d.get(y).getCpt().size() - d.get(x).getCpt().size();
            }
            else return neighborsCounter(x, d) - neighborsCounter(y, d);
        });

        //create an hashmap of all optional outcomes:
        HashMap<String,ArrayList<String>> allOutcomes=new HashMap<>();
        for(Variable factor: factors.values()){
            allOutcomes.put(factor.getVar_name(), d.get(factor.getVar_name()).getOutcomes());
            //delete rows from each factor by evidence information and push the new data to factors.
            boolean erased=false; // if var name was erased or not
            for (Map.Entry<String, String> entry : evidences.entrySet()) {
                String evidenceVarName = entry.getKey();
                String evidenceOutcome = entry.getValue();
                int cptLen=factor.getCpt().size();
                int evidenceOutIndex=d.get(evidenceVarName).getOutcomes().indexOf(evidenceOutcome);
                int outcomesNum=factor.getOutcomes().size();
                if(factor.getVar_name().equals(evidenceVarName)){
                    ArrayList<String> newCpt=new ArrayList<>();
                    int pointer=evidenceOutIndex;
                    while(pointer<cptLen){
                        newCpt.add(factor.getCpt().get(pointer));
                        pointer+=outcomesNum;
                    }

                    factors.get(factor.getVar_name()).replaceCpt(newCpt);
                    erased=true;
                }
                else{
                    for(String p:factor.getParents()){
                        Variable parent=d.get(p);
                        if(parent.getVar_name().equals(evidenceVarName)){
                            int outChange=factor.getOutcomes().size();
                            if(erased) outChange=1;
                            int parentOutNum = parent.getOutcomes().size();
                            List<String> factParents = factor.getParents();
                            for (int i=factParents.size()-1; i>=0; i--) {
                                if(factParents.get(i).equals(p)) break;
                                int inParentOutNum=factors.get(factParents.get(i)).getOutcomes().size();
                                outChange*=inParentOutNum;
                            }
                            ArrayList<String> newCpt=new ArrayList<>();
                            int pointer=evidenceOutIndex*outChange;
                            while(pointer<cptLen){
                                for(int i=0; i<outChange;i++) {
                                    newCpt.add(factor.getCpt().get(pointer+i));
                                }
                                pointer+=outChange*parentOutNum;
                            }
                            ArrayList<String> parents = factors.get(factor.getVar_name()).getParents();
                            ArrayList<String> newParents = new ArrayList<>();
                            for(String par:parents){
                                if(!par.equals(p)) newParents.add(par);
                            }
                            factors.get(factor.getVar_name()).setParents(newParents);
                            factors.get(factor.getVar_name()).replaceCpt(newCpt);
                        }
                    }
                }
                ArrayList<String> newOutcomes=new ArrayList<>();
                newOutcomes.add(evidenceOutcome);
                allOutcomes.put(evidenceVarName, newOutcomes);
            }
        }

        //transfer factors to arraylist of Factor class objects which holds the factor variables and its cpt table.
        //from now i will use myFactors ArrayList and allOutcomes HashMap.
        ArrayList<Factor> myFactors = new ArrayList<>();
        for(Variable factor:factors.values()){
            boolean flag=true;
            for (String evidence : evidences.keySet()) {
                if (factor.getVar_name().equals(evidence)) {
                    flag = false;
                    break;
                }
            }
            //check that the factor has more than one row:
            if(factor.getCpt().size()>1) {
                ArrayList<String> parentsPlusName = new ArrayList<>(factor.getParents());
                if(flag) parentsPlusName.add(factor.getVar_name());
                Factor f=new Factor(parentsPlusName, factor.getCpt());
                myFactors.add(f);
                factor.setVar_name(null);
            }
        }
        int[] plusSum=new int[1];
        int[] multSum=new int[1];
        for(String variable:hidden){
            ArrayList<Factor> relevantFactors = new ArrayList<>();
            for(Factor factor:myFactors){
                if(factor.getVariables().contains(variable)){
                    relevantFactors.add(factor);
                }
            }
            for(Factor factor:relevantFactors){
                myFactors.remove(factor);
            }
            //sort the factors to multiply by size order. if size is equal sort by sum of ascii values.
            // used lambda instead of compare.
            relevantFactors.sort((x, y) -> {
                if (x.getCpt().size() == y.getCpt().size()) return ascii(x) - ascii(y);
                else return x.getCpt().size() - y.getCpt().size();
            });

            Factor product = relevantFactors.get(0);
            for (int i = 1; i < relevantFactors.size(); i++) {
                product = multiplyFactors(product, relevantFactors.get(i), allOutcomes, multSum);
            }

            if(product.getVariables().size()>1){
                product = eliminate(product, allOutcomes, variable, plusSum);
                myFactors.add(product);
            }
        }

        Factor finalProduct;
        if(myFactors.size()>1){
            finalProduct = myFactors.get(0);
            for (int i = 1; i < myFactors.size(); i++) {
                finalProduct = multiplyFactors(finalProduct, myFactors.get(i), allOutcomes, multSum);
            }
        }
        else {
            finalProduct = myFactors.get(0);
        }

        //find sum of all cpt for normalization.
        double sumOfCpt = 0;
        for(String value:finalProduct.getCpt()){
            sumOfCpt+=Double.parseDouble(value);
            plusSum[0]+=1;
        }
        //i added 1 one time more than needed because sumOfCpt started from 0.
        plusSum[0]-=1;
        ArrayList<String> newCpt=new ArrayList<>();
        for(String value:finalProduct.getCpt()){
            newCpt.add(String.valueOf((Double.parseDouble(value)/sumOfCpt)));
        }
        String returnVal = newCpt.get(allOutcomes.get(queryVar).indexOf(queryVal));
        DecimalFormat df = new DecimalFormat("#.#####");
        String answer = String.valueOf(df.format(Double.parseDouble(returnVal)));

        return(answer+","+plusSum[0]+","+multSum[0]);
    }
}
