package com.redes;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnviaDados extends Thread {

    private final int portaDestino = 2001;
    private final int portaLocalRecebimento = 2003;
    private Semaphore sem;
    private final String funcao;
    private static List<Pacote> pacotes;
    private static int totalPacotes;
    private static final int TAMANHO_JANELA = 5;
    private static int base;
    private int proximoNumeroSequencia;
    private DatagramSocket datagramSocket;

    public EnviaDados(Semaphore sem, String funcao) {
        super(funcao);
        this.sem = sem;
        this.funcao = funcao;
        pacotes = new ArrayList<>();
        totalPacotes = 0;
        base = 0;
        proximoNumeroSequencia = 0;
    }

    public String getFuncao() {
        return funcao;
    }

    private void enviaProximaJanela() {
        while (proximoNumeroSequencia < totalPacotes && proximoNumeroSequencia < base + TAMANHO_JANELA) {
            Pacote pacote = pacotes.get(proximoNumeroSequencia);

            enviaPct(pacote, false);

            proximoNumeroSequencia++;
        }
    }

    private void enviaPct(Pacote pacote, boolean reenvio) {
        ByteBuffer byteBuffer = ByteBuffer.allocate((pacote.getDados().length + 1) * 4);
        byteBuffer.putInt(pacote.getNumeroSequencia());
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(pacote.getDados());

        byte[] buffer = byteBuffer.array();

        try {
            if (!reenvio) {
                sem.acquire();
            }

            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, portaDestino);
            datagramSocket.send(packet);
            System.out.println("[->] Dado enviado: " + pacote.getNumeroSequencia());

            pacote.startTimer(1, new TimerTask() {
                @Override
                public void run() {
                    enviaPct(pacote, true);
                }
            }, sem);
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void divideArquivo(String nomeArquivo) {
        try (FileInputStream fileInput = new FileInputStream(nomeArquivo)) {
            int[] dados = new int[350];
            int cont = 1;

            int numeroSequencia = 0;

            int lido;
            while ((lido = fileInput.read()) != -1) {
                dados[cont-1] = lido;
                cont++;

                if (cont == 350) {
                    Pacote pacote = new Pacote(numeroSequencia, dados.clone());
                    pacotes.add(pacote);
                    totalPacotes++;
                    numeroSequencia++;
                    dados = new int[350];
                    cont = 1;
                }
            }

            if (cont > 1) {
                for (int i = cont-1; i < 350; i++) {
                    dados[i] = -1;
                }
                Pacote pacoteFinal = new Pacote(numeroSequencia, dados.clone());
                pacotes.add(pacoteFinal);
                totalPacotes++;
            }

            System.out.println("TOTAL PACOTES: " + totalPacotes);

        } catch (IOException e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket();

            switch (this.getFuncao()) {
                case "envia":
                    divideArquivo("C:\\Users\\ongio_1lak36v\\Downloads\\github\\redes-trabalho-2\\ProtocoloParalelo\\teste-29mb.zip");

                    enviaProximaJanela();

                    while (base < totalPacotes) {
                        try {
                            Thread.sleep(100);

                            while (base < totalPacotes && pacotes.get(base).isConfirmado()) {
                                base++;
                                if (proximoNumeroSequencia < totalPacotes) {
                                    enviaPct(pacotes.get(proximoNumeroSequencia), false);
                                    proximoNumeroSequencia++;
                                }
                            }

                        } catch (InterruptedException ex) {
                            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    break;
                case "ack":
                    try {
                        DatagramSocket serverSocket = new DatagramSocket(portaLocalRecebimento);
                        byte[] receiveData = new byte[4];
                        while (true) {
                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            serverSocket.receive(receivePacket);

                            int numeroSequencia = ByteBuffer.wrap(receivePacket.getData()).getInt();

                            for (Pacote pacote : pacotes) {
                                if (pacote.getNumeroSequencia() == numeroSequencia) {
                                    System.out.println("[ok] confirmando pacote: " + pacote.getNumeroSequencia());
                                    pacote.setConfirmado(true);
                                    pacote.cancelaTimer();
                                    break;
                                }
                            }

                            if (numeroSequencia == totalPacotes-1) {
                                System.out.println("[fim] Todos os pacotes foram confirmados. Parando o envio.");
                                serverSocket.close();
                                System.exit(0);
                                return;
                            }
                            sem.release();
                        }
                    } catch (IOException e) {
                        System.out.println("Exceção: " + e.getMessage());
                    }
                    break;
                default:
                    break;
            }
        } catch (SocketException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
