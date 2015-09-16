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
    protected boolean running;
    ByteArrayOutputStream out;
    volatile boolean audioFinished = true;

    /**
     * Create main window and buttons.
     * Using old awt technology.
     */
    public VoiceRecord() {
        super("Voice Record ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container content = getContentPane();
        Dimension d = new Dimension(300, 150);
        content.setPreferredSize(d);

        final JButton record = new JButton("Record");
        final JButton stop = new JButton("Stop");
        final JButton play = new JButton("Play");

        record.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(false);

        //buttons actions
        ActionListener captureListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                record.setEnabled(false);
                stop.setEnabled(true);
                play.setEnabled(false);
                recordAudio();
                if (audioFinished) record.setEnabled(true);
            }
        };
        record.addActionListener(captureListener);
        content.add(record, BorderLayout.NORTH);


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
                playAudio();
                stop.setEnabled(true);
                record.setEnabled(false);
                running = true;
                while (!audioFinished) {
                    if (running) play.setEnabled(false);
                    else play.setEnabled(true);
                }



            }
        };
        play.addActionListener(playListener);
        content.add(play, BorderLayout.SOUTH);
    }

    public static void main(String args[]) {
        JFrame frame = new VoiceRecord();
        frame.pack();
        frame.setVisible(true);
    }

    private void playAudio() {
        try {
            byte audio[] = out.toByteArray();

            InputStream input = new ByteArrayInputStream(audio);
            final AudioFormat format = getFormat();
            final AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
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
                                System.out.println("Play Interapted");
                            }
                        }
                        if (line.isRunning()) {
                            line.close();
                            System.out.println("Play Finished");
                        }
                        audioFinished = true;
                        running = false;
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

    private AudioFormat getFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 2;//2- stereo 1-mono
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    private void recordAudio() {
        try {
            final AudioFormat format = getFormat();
            //Open channel for data stream
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
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
}