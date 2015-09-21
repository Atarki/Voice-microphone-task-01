package com.company;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VoiceRecord extends JFrame {
    /**
     * Create main window and buttons.
     * Using old awt technology.
     */
    public volatile boolean running = false;
    public ByteArrayOutputStream out;
    public AudioFormat format;
    public String infoText = "Voice recorder";
    public volatile boolean protectCheck = false;

    public VoiceRecord() {
        setTitle("Voice Record ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container content = getContentPane();
        Dimension d = new Dimension(300, 200);
        content.setPreferredSize(d);
        ImageIcon img1 = new ImageIcon("src/images/record.png");
        ImageIcon img2 = new ImageIcon("src/images/record_protect.png");

        final JButton record = new JButton("Record");
        final JButton stop = new JButton("Stop");
        final JButton play = new JButton("Play");
        final JLabel infoLabel = new JLabel(infoText);
        final JToggleButton protect = new JToggleButton(img1);

        record.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(false);
        infoLabel.setEnabled(true);
        protect.setEnabled(true);
        protect.setSelectedIcon(img2);

        content.add(infoLabel, BorderLayout.PAGE_END);

        //buttons actions
        ItemListener protectListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                protectCheck = protect.isSelected();
            }
        };
        protect.addItemListener(protectListener);
        content.add(protect, BorderLayout.PAGE_START);


        ActionListener captureListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                record.setEnabled(false);
                stop.setEnabled(true);
                play.setEnabled(false);
                infoLabel.setText("Recording...");
                recordAudio();
            }
        };
        record.addActionListener(captureListener);
        content.add(record, BorderLayout.LINE_START);


        ActionListener stopListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(true);
                infoLabel.setText("Stopped...");
                running = false;
            }
        };
        stop.addActionListener(stopListener);
        content.add(stop, BorderLayout.CENTER);


        ActionListener playListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                record.setEnabled(false);
                stop.setEnabled(true);
                play.setEnabled(false);
                infoLabel.setText("Playing...");
                playAudio(record,stop,play,infoLabel);
            }
        };
        play.addActionListener(playListener);
        content.add(play, BorderLayout.LINE_END);
    }

    public static void main(String args[]) {
        JFrame frame = new VoiceRecord();
        frame.pack();
        frame.setVisible(true);
    }

    public void playAudio(final JButton record, final JButton stop,final JButton play, final JLabel infoLabel) {
        try {
            byte audio[] = out.toByteArray();
            InputStream input = new ByteArrayInputStream(audio);
            format = getFormat();
            AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            //Read data in a separated thread.
            Runnable runner = new Runnable() {
                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte buffer[] = new byte[bufferSize];

                //Open thread channel for reading data from SourceDataLine stream
                public void run() {
                    running = true;
                    try {
                        int count;
                        while ((count = ais.read(buffer, 0, buffer.length)) != -1) {
                            //Encrypt secure part
                            if (protectCheck) {
                                for (int i = 0; i < bufferSize; i++) {
                                    int b = (int)buffer[i];
                                    b = b >> 31;// moving byte on 31 circle
                                    buffer[i] = (byte)b;
                                }
                            }
                            if (count > 0) {
                                line.write(buffer, 0, count);
                            }
                            if (!running) {
                                line.close();
                            }
                        }
                        if (line.isOpen()) {
                            line.close();
                            record.setEnabled(true);
                            stop.setEnabled(false);
                            play.setEnabled(true);
                            infoLabel.setText("Playing completed...");
                        }
                    } catch (IOException e) {
                        System.err.println("I/O problems: " + e);
                        System.exit(-3);
                    }
                }
            };
            Thread playThread = new Thread(runner);
            playThread.start();

        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-4);
        }
    }

    public void recordAudio() {
        try {
            format = getFormat();
            //Open channel for data stream
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            //Read data in a separated thread
            Runnable runner = new Runnable() {
                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte buffer[] = new byte[bufferSize];

                //Open thread channel for reading data from TargetDataLine stream
                public void run() {
                    out = new ByteArrayOutputStream();
                    running = true;
                    try {
                        while (running) {
                            int count = line.read(buffer, 0, buffer.length);
                            if (count > 0) {
                                out.write(buffer, 0, count);
                            }
                        }
                        out.close();
                        line.close();
                    } catch (IOException e) {
                        System.err.println("I/O problems: " + e);
                        System.exit(-1);
                    }
                }
            };
            Thread captureThread = new Thread(runner);
            captureThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-2);
        }
    }

    private AudioFormat getFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 2;//2- stereo 1-mono
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
}