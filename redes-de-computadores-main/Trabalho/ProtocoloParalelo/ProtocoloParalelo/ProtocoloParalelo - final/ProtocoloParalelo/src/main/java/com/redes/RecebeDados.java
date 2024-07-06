package com.redes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecebeDados extends Thread {

    private final int portaLocalReceber = 2001;
    private final int portaLocalEnviar = 2002;
    private final int portaDestino = 2003;
    private final double PROBABILIDADE_PERDA = 0;
    private Random random = new Random();

    private int proximoNumeroSequenciaEsperado = 0;

    private void enviaAck(int numeroSequencia) {
        try {
            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnviar)) {
                byte[] sendData = ByteBuffer.allocate(4).putInt(numeroSequencia).array();

                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length, address, portaDestino);

                datagramSocket.send(packet);
            }
        } catch (SocketException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(portaLocalReceber);
            byte[] receiveData = new byte[1400];
            try (FileOutputStream fileOutput = new FileOutputStream("saida.zip")) {
                boolean fim = false;
                while (!fim) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);


                    byte[] receivedBytes = receivePacket.getData();
                    int numeroSequencia = ByteBuffer.wrap(receivePacket.getData()).getInt();

                    System.out.println("[<-] Dado recebido: " + numeroSequencia);
                    if (random.nextDouble() < PROBABILIDADE_PERDA) {
                        System.out.println("[x ] Pacote perdido: " + numeroSequencia);
                        continue;
                    }

                    if (numeroSequencia == proximoNumeroSequenciaEsperado) {
                        for (int i = 4; i < receivedBytes.length; i = i + 4) {
                            int dados = ((receivedBytes[i] & 0xff) << 24) + ((receivedBytes[i + 1] & 0xff) << 16) + ((receivedBytes[i + 2] & 0xff) << 8) + (receivedBytes[i + 3] & 0xff);

                            if (dados == -1) {
                                fim = true;
                                break;
                            }
                            fileOutput.write(dados);
                        }

                        System.out.println("[->] ack: " + numeroSequencia);
                        enviaAck(numeroSequencia);

                        proximoNumeroSequenciaEsperado++;
                    } else {
                        System.out.println("[x ] Pacote fora de ordem. Esperando pelo pacote: " + proximoNumeroSequenciaEsperado);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Excecao: " + e.getMessage());
        }
    }
}
