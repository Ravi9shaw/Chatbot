import java.io.*;
import java.util.*;

public class ChatEngine {

    public static class PhraseData {
        List<String> replies;
        String persona;
        int usage;
        String topic;

        PhraseData(String reply, String p, int u, String t) {
            replies = new ArrayList<>();
            replies.add(reply);
            persona = p;
            usage = u;
            topic = t;
        }
    }

    private static final Map<String, PhraseData> responses = new HashMap<>();
    private static final String MEMORY_FILE = "memory.txt";
    private static final String TRAINING_FILE = "training.txt";
    private static final String LEARNING_FILE = "learning.txt";
    private static String mode = "Krishna";
    private static final LinkedList<String> context = new LinkedList<>();
    private static final int CONTEXT_SIZE = 22;
    private static final Set<String> shortForms = new HashSet<>(Arrays.asList("ik", "wsp", "brb", "lol", "np"));
    private static final int SIMILARITY_THRESHOLD = 0;

    static {
        // Krishna base
        addPhrase("hello", "Greetings, young coder! Let’s tackle some challenges.", "Krishna","greeting");
        addPhrase("hi", "Yo! Let’s learn something new today.", "Krishna","greeting");
        addPhrase("motivate me", "Strength lies in practice and patience.", "Krishna","motivation");
        addPhrase("keep learning", "Knowledge grows step by step.", "Krishna","motivation");

        // Gojo base
        addPhrase("hello", "Yo! Gojo’s in the house!", "Gojo","greeting");
        addPhrase("hi", "Heh~ What's up bro?", "Gojo","greeting");
        addPhrase("motivate me", "Master your mind, conquer the game.", "Gojo","motivation");

        // White Ichigo base
        addPhrase("hello", "The beast stirs... Hello.", "White Ichigo","greeting");
        addPhrase("hi", "Prepare yourself, bro.", "White Ichigo","greeting");
        addPhrase("motivate me", "Face your fears. Become unstoppable.", "White Ichigo","motivation");

        // Load files
        loadTraining(TRAINING_FILE);
        loadMemory();
        loadLearning();
    }

    public static void setMode(String m){ mode = m; }
    public static String getMode(){ return mode; }

    private static void addPhrase(String key, String reply, String persona, String topic){
        key = key.toLowerCase().trim();
        if(responses.containsKey(key)){
            responses.get(key).replies.add(reply);
        } else {
            responses.put(key,new PhraseData(reply, persona, 0, topic));
        }
    }

    public static void learn(String input, String reply){
        input = normalizeInput(input);
        addPhrase(input,reply,mode,"custom");
        saveToLearning(input, reply, mode,1,"custom");
    }

    public static void deletePhrase(String input){
        input = normalizeInput(input);
        responses.remove(input);
        reloadMemoryFile();
    }

    public static Map<String,String> getLearnedPhrases(){
        Map<String,String> map = new HashMap<>();
        for(String key: responses.keySet()){
            PhraseData pd = responses.get(key);
            map.put(key,String.join("|",pd.replies) + "===" + pd.persona + "===" + pd.usage + "===" + pd.topic);
        }
        return map;
    }

    public static String getTechResponse(String input){
        return getResponse(input);
    }

    private static String getResponse(String input){
        if(input==null || input.trim().isEmpty()) return "Say something, bro!";
        input = normalizeInput(input);

        // Persona switch commands
        if(input.equals("switch to gojo")) { mode="Gojo"; return "Heh~ Gojo’s here."; }
        if(input.equals("switch to ichigo")) { mode="White Ichigo"; return "The beast has awakened."; }
        if(input.equals("switch to krishna")) { mode="Krishna"; return "Peace restored."; }

        context.add(input);
        if(context.size() > CONTEXT_SIZE) context.removeFirst();

        if(responses.containsKey(input)){
            return wrapPersonality(responses.get(input));
        }

        PhraseData best = null;
        double maxScore = 0;
        for(String key: responses.keySet()){
            double score = similarity(input,key);
            if(score>maxScore){
                maxScore = score;
                best = responses.get(key);
            }
        }

        if(best==null || maxScore<SIMILARITY_THRESHOLD){
            return null;
        }

        return wrapPersonality(best);
    }

    private static String wrapPersonality(PhraseData pd){
        Random r = new Random();
        String reply = pd.replies.get(r.nextInt(pd.replies.size()));
        pd.usage++;
        saveMemory(reply,pd.persona,pd.usage);
        switch(pd.persona){
            case "Gojo": return reply + " 💎 (Gojo Mode)";
            case "White Ichigo": return reply + " ⚔️ (Ichigo Mode)";
            case "Krishna": return reply + " 🕉️ (Krishna Mode)";
            default: return reply;
        }
    }

    private static void saveMemory(String reply,String persona,int usage){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(MEMORY_FILE,true))){
            writer.write(reply + "===" + persona + "===" + usage);
            writer.newLine();
        } catch(Exception e){ System.err.println("Error saving memory: "+e.getMessage()); }
    }

    private static void saveToLearning(String key, String reply, String persona,int usage,String topic){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(LEARNING_FILE,true))){
            writer.write(key + "===" + reply + "===" + persona + "===" + usage + "===" + topic);
            writer.newLine();
        } catch(Exception e){ System.err.println("Error saving learning: "+e.getMessage()); }
    }

    private static void loadMemory(){
        File f = new File(MEMORY_FILE); if(!f.exists()) return;
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            while((line=br.readLine())!=null){
                String[] parts = line.split("===");
                if(parts.length>=3){
                    String reply = parts[0].trim();
                    String persona = parts[1].trim();
                    addPhrase(reply,reply,persona,"custom");
                }
            }
        } catch(Exception e){ System.err.println("Error loading memory: "+e.getMessage()); }
    }

    private static void loadLearning(){
        File f = new File(LEARNING_FILE); if(!f.exists()) return;
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            while((line=br.readLine())!=null){
                String[] parts = line.split("===");
                if(parts.length>=4){
                    String key = parts[0].trim();
                    String reply = parts[1].trim();
                    String persona = parts[2].trim();
                    addPhrase(key,reply,persona,"custom");
                }
            }
        } catch(Exception e){ System.err.println("Error loading learning: "+e.getMessage()); }
    }

    private static void reloadMemoryFile(){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(MEMORY_FILE))){
            for(String key: responses.keySet()){
                PhraseData pd = responses.get(key);
                for(String r: pd.replies){
                    writer.write(r + "===" + pd.persona + "===" + pd.usage);
                    writer.newLine();
                }
            }
        } catch(Exception e){ System.err.println("Error reloading memory: "+e.getMessage()); }
    }

    private static void loadTraining(String fileName){
        File f = new File(fileName); if(!f.exists()) return;
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            while((line=br.readLine())!=null){
                String[] parts = line.split("===");
                if(parts.length>=2){
                    String key = parts[0].trim();
                    String reply = parts[1].trim();
                    String persona = parts.length>=3 ? parts[2].trim() : "Krishna";
                    String topic = parts.length>=4 ? parts[3].trim() : "general";
                    addPhrase(key,reply,persona,topic);
                }
            }
        } catch(Exception e){ System.err.println("Error loading training: "+e.getMessage()); }
    }

    private static double similarity(String s1,String s2){
        Set<String> w1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> w2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        int intersect = 0;
        for(String w:w1) if(w2.contains(w)) intersect++;
        return (double)intersect/Math.min(w1.size(),w2.size());
    }

    private static String normalizeInput(String input){
        input = input.toLowerCase().trim();
        for(String sf: shortForms){
            input = input.replace(sf,"");
        }
        return input.trim();
    }
}
