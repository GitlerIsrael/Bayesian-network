import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.*;


public class BayesianNetwork {
    /**
     * The function read the text file with the queries and calculate them.
     * Finally, put it in text output file and return it.++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * @param txt text file with needed queries.
     * @throws FileNotFoundException throws this exception.
     */
    public static void calc(String txt) throws FileNotFoundException {
        File file = new File(txt);
        Scanner sc = new Scanner(file);
        String xmlFile = sc.nextLine();
        HashMap<String, Variable> originalData= XMLParser.Parser(xmlFile);
        while (sc.hasNextLine()) {
            HashMap<String, Variable> d = copy(originalData);
            String line = sc.nextLine();
            System.out.println(line);
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
            ArrayList<String> queryVarPar = d.get(queryVar).getParents();
            if(evidencesVars.containsAll(queryVarPar) && queryVarPar.containsAll(evidencesVars)) {
                String answer = String.valueOf(cptval(queryVar, queryVal, evidencesVars, evidencesVals, d));
                ///write to file!++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++=

            }

            char alg = line.charAt(line.length()-1);
            ////write to file for each condition
            if(alg=='1') one(query, d);
            else if(alg=='2') two(query, d);
            else if(alg=='3') three(query, d);
        }

//        PrintWriter writer = new PrintWriter("output.txt", "UTF-8");
//        writer.println("The first line");
//        writer.println("The second line");
//        writer.close();
    }


    private static HashMap<String, Variable> copy(HashMap<String, Variable> original)
    {
        HashMap<String, Variable> copy = new HashMap<String, Variable>();
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
     * The function get a query string such - "B=T|M=T,J=T" and return an hashmap
     * which holds this data- "{B:T,M:T,J:T}".
     * @param query string of query.
     * @return hashmap of query data.
     */
    private static HashMap querySplit (String query){
        HashMap<String, String> map = new HashMap<String, String>();
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

        // extract each of the integers in the first list
        // and add each to ints as a new list

        for (String i : (data.get(keys[0])).getOutcomes()) {
            List<String> newList = new ArrayList<>();
            newList.add(i);
            combinations.add(newList);
        }
        index++;
        while (index < keys.length) {
            List<String> nextList = (data.get(keys[index])).getOutcomes();
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
                if(rowCount==0){
                    rowCount=temp;
                }
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
        HashMap<String, String> splitted = (HashMap<String, String>) querySplit(query);
        Object[] allKeys= d.keySet().toArray();
        ArrayList<String> hiddenArrList = new ArrayList<String>();
        for(Object key: allKeys){
            if (splitted.get(key)==null){
                hiddenArrList.add((String) key);
            }
        }
        Object[] keys = hiddenArrList.toArray();
        Set<List<String>> allCombinations = allCombinations(d, keys);
        ArrayList<String> orderOfVars = new ArrayList<String>();
        for(String key: hiddenArrList){
            orderOfVars.add(key);
        }
        for(String key:splitted.keySet()){
            orderOfVars.add(key);
        }
        for(List l: allCombinations){
            for(String value: splitted.values()){
                l.add(value);
            }
        }

///////casting to arraylists for cptVal func. need to send to it this data for calc.
        ArrayList<ArrayList<String>> combsArr = new ArrayList<>();
        for (List l : allCombinations) {
            ArrayList<String> comb = new ArrayList<>();
            for (Object s : l) {
                comb.add((String) s);
            }
            combsArr.add(comb);
        }

        double numeratorSum=0;
        for(ArrayList<String> arr:combsArr){
            double innerSum=1;
            for(String var: orderOfVars){
                int varIndex = orderOfVars.indexOf(var);
                String varOutcome = arr.get(varIndex);
                double rowVal = cptval(var, varOutcome, orderOfVars, arr, d);
                innerSum =innerSum*rowVal;
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
        splitted.remove(queryVar);
        ArrayList<String> orderOfVarsDenominator = new ArrayList<>();
        orderOfVarsDenominator.addAll(hiddenArrList);
        orderOfVarsDenominator.addAll(splitted.keySet());
        for(List l: denominatorAllCombinations) {
            l.addAll(splitted.values());
        }
///////casting to arraylists for cptCol func. need to send to it this data for calc.
        ArrayList<ArrayList<String>> denominatorCombsArr = new ArrayList<>();
        for (List l : denominatorAllCombinations) {
            ArrayList<String> denominatorComb = new ArrayList<>();
            for (Object s : l) {
                denominatorComb.add((String) s);
            }
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
        System.out.println(answer+","+plus+","+mult);

        return (answer+","+plus+","+mult);
    }


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
                // do func
//                for (int i = 0; i < values.length; i++) {
//                    System.out.print(values[i]);
//                }
//                System.out.println();

                //
                int indexInA = 0;
                int indexInB = 0;
                //

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


    private static Factor eliminate(Factor factor, HashMap<String, ArrayList<String>> outcomes, String hidden, int[] plusSum) {
        ArrayList<String> newFactorVars = new ArrayList<>(factor.getVariables());
        newFactorVars.remove(hidden);
        ArrayList<String> newFactorCpt = new ArrayList<>();


        ArrayList<Integer> lengths = new ArrayList<>();
        for (String var : factor.getVariables()) {
            lengths.add(outcomes.get(var).size());
        }

        int thisJump=outcomes.get(hidden).size();
        int beforeJump=1;
        int afterJump=1;
        for(int i=factor.getVariables().size()-1;i>factor.getVariables().indexOf(hidden);i--){
            beforeJump*=outcomes.get(factor.getVariables().get(i)).size();
        }
        for(int i=0;i<factor.getVariables().indexOf(hidden);i++){
            afterJump*=outcomes.get(factor.getVariables().get(i)).size();
        }
        for(int i=0; i<factor.getCpt().size(); i+=beforeJump*thisJump){
            for(int j=0; j<beforeJump; j++){
                double cptValue=0;
                for(int k=0; k<thisJump;k++){
                    int currIndex=i+j+(k*(beforeJump));
                    cptValue += Double.parseDouble(factor.getCpt().get(currIndex));
                    plusSum[0]+=1;
                }
                // because in the beginning cptValue was 0 and i count plus operation for adding the first value-
                //i will decrease it by this one plus operation.
                plusSum[0]-=1;
                newFactorCpt.add(String.valueOf(cptValue));
            }
        }
        return new Factor(newFactorVars, newFactorCpt);
    }


    public static String two(String query, HashMap<String, Variable> d) {
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
        for(Object key: d.keySet()){
            if(evidences.get(key)==null && !key.equals(queryVar) && relevantVars.contains(key)){
                hidden.add((String) key);
            }
        }
        Collections.sort(hidden); ///sort by abc.

        //create an hashmap of all optional outcomes:
        HashMap<String,ArrayList<String>> allOutcomes=new HashMap<>();
        for(Variable factor: factors.values()){
            allOutcomes.put(factor.getVar_name(), factor.getOutcomes());
            //delete rows from each factor by evidence information and push the new data to factors.
            for (Map.Entry<String, String> entry : evidences.entrySet()) {
                String evidenceVarName = entry.getKey();
                System.out.println("factor name:  "+ factor.getVar_name());
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
                }
                else{
                    for(String p:factor.getParents()){
                        System.out.println(p);
                        Variable parent=d.get(p); ///factors.get
                        System.out.println(parent);
                        if(parent.getVar_name().equals(evidenceVarName)){
                            int outChange=factor.getOutcomes().size();
                            List<String> factParents = factor.getParents();
                            System.out.println("Factor: "+factor.getVar_name()+", parents: "+factParents);
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
                                pointer+=outChange*outcomesNum;
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
                if (factor.getVar_name().equals(evidence)) flag = false;
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
        System.out.println(myFactors);/////////////////////////
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
                if(myFactors.contains(factor)) myFactors.remove(factor);
            }
            //sort the factors to multiply by size order. if size is equal sort by sum of ascii values.
            // used lambda instead of compare.
            relevantFactors.sort((x, y) -> {
                if (x.getCpt().size() == y.getCpt().size()) return ascii(x) - ascii(y);
                else return x.getCpt().size() - y.getCpt().size();
            });
            System.out.println("Relevant");
            System.out.println(relevantFactors);
            System.out.println("\n\n");


            Factor product = relevantFactors.get(0);
            for (int i = 1; i < relevantFactors.size(); i++) {
                product = multiplyFactors(product, relevantFactors.get(i), allOutcomes, multSum);
            }
            System.out.println("after multiply");
            System.out.println(product);
            System.out.println("\n\n");
            ////////////eliminate!!!!!!!!!!!!!!!!!!!!!!!!!!!
            product = eliminate(product, allOutcomes, variable, plusSum);
            myFactors.add(product);
            System.out.println("after elimination");
            System.out.println(product);
            System.out.println("\n\n");
        }

        System.out.println("My Factors:" + myFactors);

        Factor finalProduct;
        if(myFactors.size()>1){
            finalProduct = myFactors.get(0);
            for (int i = 1; i < myFactors.size(); i++) {
                finalProduct = multiplyFactors(finalProduct, myFactors.get(i), allOutcomes, multSum);
            }
            System.out.println("after multiply");
            System.out.println(finalProduct);
            System.out.println("\n\n");
        }
        else {
            finalProduct = myFactors.get(0);
        }
        //find sum of all cpt for normalization.
        double sumOfCpt = 0;
        for(String value:finalProduct.getCpt()){
            sumOfCpt+=Double.valueOf(value);
            plusSum[0]+=1;
        }
        //i added 1 one time more than needed beacause sumOfCpt started from 0.
        plusSum[0]-=1;
        ArrayList<String> newCpt=new ArrayList<>();
        for(String value:finalProduct.getCpt()){
            newCpt.add(String.valueOf((Double.valueOf(value)/sumOfCpt)));
        }
        String returnVal = newCpt.get(allOutcomes.get(queryVar).indexOf(queryVal));
        DecimalFormat df = new DecimalFormat("#.#####");
        String answer = String.valueOf(df.format(Double.parseDouble(returnVal)));
        System.out.println(answer+","+plusSum[0]+","+multSum[0]);

        return(answer+","+plusSum[0]+","+multSum[0]);
    }

    private static void three(String query, HashMap d) {
    }


}
