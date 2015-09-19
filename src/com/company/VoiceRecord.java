package com.company;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    public volatile boolean audioFinished = false;
    public ByteArrayOutputStream out;
    public AudioFormat format;

    public VoiceRecord() {
        setTitle("Voice Record ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container content = getContentPane();
        Dimension d = new Dimension(300, 150);
        content.setPreferredSize(d);

        final JButton record = new JButton("Record");
        final JButton stop = new JButton("Stop");
        final JButton play = new JButton("Play");
        final JButton image = new JButton(new ImageIcon("src/record.png"));


        record.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(false);
        image.setEnabled(false);

        content.add(image, BorderLayout.PAGE_START);

        //buttons actions
        ActionListener captureListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                record.setEnabled(false);
                stop.setEnabled(true);
                play.setEnabled(false);
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
                playAudio();
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

    public synchronized boolean runningFalse() {
        return running = false;
    }

    public synchronized void playAudio() {
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
                            if (count > 0) {
                                line.write(buffer, 0, count);
                            }
                            if (!running) {
                                line.close();
                                audioFinished = true;
                                runningFalse();
                                System.out.println("Play Interrupted");
                            }
                        }
                        if (line.isOpen()) {
                            System.out.println("Play Finished");
                            line.close();
                            runningFalse();
                            audioFinished = true;
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
//            if (line != null) line.close();
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