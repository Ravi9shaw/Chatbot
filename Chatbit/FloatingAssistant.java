import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;

public class FloatingAssistant {

    private static final Map<String, LinkedList<String>> recentReplies = new HashMap<>();
    private static final int MAX_RECENT = 5;

    public static void main(String[] args) {
        JFrame chatFrame = new JFrame("AI Chat");
        chatFrame.setSize(500, 550);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setLayout(new BorderLayout());

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));

        JTextField inputField = new JTextField();

        JPanel topPanel = new JPanel(new BorderLayout());
        JButton memoryButton = new JButton("Memory Panel");

        String[] personas = {"Krishna", "Gojo", "White Ichigo"};
        JComboBox<String> personaSelect = new JComboBox<>(personas);
        personaSelect.setSelectedItem(ChatEngine.getMode());
        personaSelect.addActionListener(e -> {
            ChatEngine.setMode((String) personaSelect.getSelectedItem());
            chatArea.append("Switched to " + ChatEngine.getMode() + "\n\n");
        });

        topPanel.add(memoryButton, BorderLayout.EAST);
        topPanel.add(personaSelect, BorderLayout.WEST);

        chatFrame.add(topPanel, BorderLayout.NORTH);
        chatFrame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatFrame.add(inputField, BorderLayout.SOUTH);

        // Floating bubble
        JFrame bubbleFrame = new JFrame();
        bubbleFrame.setUndecorated(true);
        bubbleFrame.setSize(80, 80);
        bubbleFrame.setAlwaysOnTop(true);
        bubbleFrame.setBackground(new Color(0,0,0,0));

        JPanel bubblePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                switch (ChatEngine.getMode()) {
                    case "Gojo": g.setColor(new Color(120,200,255,220)); break;
                    case "White Ichigo": g.setColor(new Color(255,60,60,220)); break;
                    case "Krishna": g.setColor(new Color(80,0,255,220)); break;
                    default: g.setColor(new Color(150,150,150,220)); break;
                }
                g.fillOval(0,0,getWidth(),getHeight());
            }
        };
        bubblePanel.setOpaque(false);
        bubblePanel.setLayout(new BorderLayout());
        JLabel logo = new JLabel("⚡", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        bubblePanel.add(logo, BorderLayout.CENTER);
        bubbleFrame.add(bubblePanel);
        bubbleFrame.setLocation(100,100);
        bubbleFrame.setVisible(true);

        final Point[] mouseDownCompCoords = {null};
        bubblePanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { mouseDownCompCoords[0] = e.getPoint(); }
            public void mouseClicked(MouseEvent e) {
                if (chatFrame.isVisible()) chatFrame.setVisible(false);
                else {
                    Point loc = bubbleFrame.getLocationOnScreen();
                    chatFrame.setLocation(loc.x + bubbleFrame.getWidth() + 10, loc.y);
                    chatFrame.setVisible(true);
                }
            }
        });
        bubblePanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getLocationOnScreen();
                bubbleFrame.setLocation(curr.x - mouseDownCompCoords[0].x, curr.y - mouseDownCompCoords[0].y);
            }
        });

        inputField.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            chatArea.append("You: " + text + "\n");

            if(text.equalsIgnoreCase("close")){
                chatFrame.dispose();
                bubbleFrame.dispose();
                System.exit(0);
                return;
            }

            if(text.toLowerCase().startsWith("run ")){
                executeMiniPrograms(text, chatArea);
                inputField.setText("");
                return;
            }

            String aiResponse = ChatEngine.getTechResponse(text);

            if(aiResponse != null){
                LinkedList<String> prev = recentReplies.getOrDefault(text, new LinkedList<>());
                if(prev.contains(aiResponse)){
                    List<String> allReplies = Arrays.asList(aiResponse.split("\\|"));
                    aiResponse = allReplies.get(new Random().nextInt(allReplies.size()));
                }
                prev.add(aiResponse);
                if(prev.size() > MAX_RECENT) prev.removeFirst();
                recentReplies.put(text, prev);
            }

            if(aiResponse == null){
                String newReply = JOptionPane.showInputDialog(chatFrame,
                        "I don’t know how to reply to:\n\"" + text + "\"\nTeach me what to say:",
                        "Teach AI", JOptionPane.PLAIN_MESSAGE);
                if(newReply != null && !newReply.trim().isEmpty()){
                    ChatEngine.learn(text,newReply);
                    chatArea.append(ChatEngine.getMode() + ": Learned your new response.\n\n");
                } else {
                    chatArea.append(ChatEngine.getMode() + ": Alright, maybe next time.\n\n");
                }
            } else {
                chatArea.append(ChatEngine.getMode() + ": " + aiResponse + "\n\n");
            }

            inputField.setText("");
            bubblePanel.repaint();
        });

        memoryButton.addActionListener(e -> openMemoryPanel(chatFrame));
    }

    private static void executeMiniPrograms(String text, JTextArea chatArea){
        try {
            String[] parts = text.split(" ");
            if(parts.length < 2){ chatArea.append("AI: Specify program name after 'run'\n\n"); return;}
            String program = parts[1].toLowerCase();

            switch(program){
                case "swap":
                    if(parts.length<4){ chatArea.append("AI: Provide two numbers to swap\n\n"); break;}
                    int a = Integer.parseInt(parts[2]);
                    int b = Integer.parseInt(parts[3]);
                    int temp = a; a = b; b = temp;
                    chatArea.append("AI: Swapped numbers -> a=" + a + " b=" + b + "\n\n");
                    break;
                case "factorial":
                    if(parts.length<3){ chatArea.append("AI: Provide number for factorial\n\n"); break;}
                    int n = Integer.parseInt(parts[2]);
                    long fact = 1; for(int i=1;i<=n;i++) fact*=i;
                    chatArea.append("AI: Factorial of " + n + " is " + fact + "\n\n");
                    break;
                case "fibonacci":
                    if(parts.length<3){ chatArea.append("AI: Provide count for Fibonacci\n\n"); break;}
                    int f = Integer.parseInt(parts[2]);
                    chatArea.append("AI: Fibonacci series up to "+f+": ");
                    int x=0,y1=1; for(int i=0;i<f;i++){ chatArea.append(x+" "); int z=x+y1;x=y1;y1=z;}
                    chatArea.append("\n\n");
                    break;
                case "prime":
                    if(parts.length<3){ chatArea.append("AI: Provide number to check prime\n\n"); break;}
                    int p = Integer.parseInt(parts[2]);
                    boolean isPrime=true; for(int i=2;i<=Math.sqrt(p);i++){ if(p%i==0){ isPrime=false; break;}}
                    chatArea.append("AI: "+p+(isPrime?" is prime":" is not prime")+"\n\n");
                    break;
                case "arraysum":
                    if(parts.length<3){ chatArea.append("AI: Provide numbers for array sum\n\n"); break;}
                    chatArea.append("AI: Sum of array: ");
                    int sum=0; for(int i=2;i<parts.length;i++){ sum+=Integer.parseInt(parts[i]); }
                    chatArea.append(sum+"\n\n");
                    break;
                default: chatArea.append("AI: Unknown program.\n\n"); break;
            }
        } catch(Exception ex){
            chatArea.append("AI: Error in program execution, check input.\n\n");
        }
    }

    private static void openMemoryPanel(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Memory Control Panel", true);
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());

        String[] columns = {"Phrase","Reply","Persona"};
        DefaultTableModel model = new DefaultTableModel(columns,0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);

        loadMemoryTable(model);

        JPanel buttons = new JPanel();
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");
        buttons.add(editBtn); buttons.add(deleteBtn); buttons.add(refreshBtn);
        dialog.add(buttons, BorderLayout.SOUTH);

        // Fixed edit button listener
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String phrase = (String) table.getValueAt(row,0);
                String reply = (String) table.getValueAt(row,1);
                String newReply = JOptionPane.showInputDialog(dialog,"Edit reply for: \""+phrase+"\"",reply);
                if (newReply!=null && !newReply.trim().isEmpty()){
                    ChatEngine.learn(phrase,newReply);
                    table.setValueAt(newReply,row,1);
                }
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if(row != -1){
                String phrase = (String)table.getValueAt(row,0);
                ChatEngine.deletePhrase(phrase);
                model.removeRow(row);
            }
        });

        refreshBtn.addActionListener(e -> {
            model.setRowCount(0);
            loadMemoryTable(model);
        });

        dialog.setVisible(true);
    }

    private static void loadMemoryTable(DefaultTableModel model){
        Map<String,String> mem = ChatEngine.getLearnedPhrases();
        for(String key: mem.keySet()){
            String val = mem.get(key);
            String reply = val, persona = "";
            String[] parts = val.split("===");
            if(parts.length >= 2) { reply = parts[0]; persona = parts[1]; }
            model.addRow(new Object[]{key, reply, persona});
        }
    }
}
