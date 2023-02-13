import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConsoleView extends JFrame{
    
    private class JTextFieldInputStream extends InputStream {
        byte[] contents;
        int pointer = 0;
        Object lock = 0;
        
        public JTextFieldInputStream(final JTextField text) {
            
            text.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if(e.getKeyChar()=='\n'){
                        synchronized (lock){
                            contents = (text.getText()+"\n").getBytes();
                            pointer = 0;
                            System.out.println(text.getText());
                            text.setText("");
                            lock.notify();
                        }
                    }
                    super.keyReleased(e);
                }
            });
        }
        
        @Override
        public int read() throws IOException {
            try {
                synchronized(lock) {
                    while(contents == null) {
                        lock.wait();
                    }
                    if(pointer >= contents.length) {
                        contents = null;
                        return -1;
                    }
                    int b = this.contents[pointer++];
                    //System.out.print(b);
                    return b;
                }
            } catch (InterruptedException e) {
                //handleInterruption();
            }
            return -1;
        }
        
    }
    
    public ConsoleView() {
        Font font = new Font("Arial", Font.PLAIN, 14);
        
        this.setLocation(100, 100);
        this.setSize(1000, 500);
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        JLabel header = new JLabel("Console");
        this.add(header, BorderLayout.NORTH);
        
        final JTextArea console = new JTextArea();
        console.setEditable(false);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        console.setFont(font);
        this.add(new JScrollPane(console), BorderLayout.CENTER);
        
        final JTextField inputField = new JTextField();
        inputField.setFont(font);
        
        JLabel inputLabel = new JLabel("Input: ");
        JPanel inputFieldPanel = new JPanel();
        GroupLayout layout = new GroupLayout(inputFieldPanel);
        inputFieldPanel.setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(inputLabel)
                .addComponent(inputField)
        );
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(inputLabel)
                .addComponent(inputField)
        );
        this.add(inputFieldPanel, BorderLayout.SOUTH);
        
        redirectStandardOut(console);
        
        System.setIn(new JTextFieldInputStream(inputField));
    }
    
    private void redirectStandardOut(JTextArea area) {
        try {
            PipedInputStream in = new PipedInputStream();
            PrintStream out = new PrintStream(new PipedOutputStream(in), true, UTF_8);
            System.setOut(out);
            System.setErr(out);
            
            Thread thread = new Thread(new StreamReader(in, area));
            thread.setDaemon(true);
            thread.start();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private static class StreamReader implements Runnable {
        
        private final StringBuilder buffer = new StringBuilder();
        private boolean notify = true;
        
        private final BufferedReader reader;
        private final JTextArea textArea;
        
        StreamReader(InputStream input, JTextArea textArea) {
            this.reader = new BufferedReader(new InputStreamReader(input, UTF_8));
            this.textArea = textArea;
        }
        
        @Override
        public void run() {
            try (reader) {
                int charAsInt;
                while ((charAsInt = reader.read()) != -1) {
                    synchronized (buffer) {
                        buffer.append((char) charAsInt);
                        if (notify) {
                            notify = false;
                            SwingUtilities.invokeLater(this::appendTextToTextArea);
                        }
                    }
                }
            } catch (IOException ex){}
        }
        
        private void appendTextToTextArea() {
            synchronized (buffer) {
                textArea.append(buffer.toString());
                buffer.delete(0, buffer.length());
                notify = true;
            }
        }
    }
}