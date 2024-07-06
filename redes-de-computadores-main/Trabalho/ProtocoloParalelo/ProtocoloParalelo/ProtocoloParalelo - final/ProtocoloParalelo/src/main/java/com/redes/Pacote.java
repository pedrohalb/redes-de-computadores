package com.redes;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class Pacote {
    private int numeroSequencia;
    private int[] dados;
    private boolean confirmado;
    private Timer timer;
    private TimerTask timerTask;
    private static List<Pacote> todosPacotes = new ArrayList<>();
    private boolean cancelado;

    public Pacote(int numeroSequencia, int[] dados) {
        this.numeroSequencia = numeroSequencia;
        this.dados = dados;
        todosPacotes.add(this);
        this.cancelado = false;
    }

    public int getNumeroSequencia() {
        return numeroSequencia;
    }

    public void setNumeroSequencia(int numeroSequencia) {
        this.numeroSequencia = numeroSequencia;
    }

    public int[] getDados() {
        return dados;
    }

    public void setDados(int[] dados) {
        this.dados = dados;
    }

    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
        if (confirmado && timerTask != null) {
            timerTask.cancel();
        }
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public void startTimer(long delay, TimerTask task, Semaphore sem) {

        if (!todosPacotes.get(numeroSequencia).isConfirmado()) {
            this.timer = new Timer();
            System.out.println("[ti] timer comecado no: " + numeroSequencia);

            if (!todosPacotes.get(numeroSequencia).isCancelado()) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!todosPacotes.get(numeroSequencia).isCancelado()) {
                            System.out.println("[ti]Timer acionado para o pacote: " + numeroSequencia);
                            task.run();
                            sem.release();
                        }
                    }
                }, delay);
            }
        }
    }

    public boolean isCancelado() {
        return cancelado;
    }

    public void cancelaTimer() {

        if (!this.isCancelado()) {
            if (this.timer != null) {
                this.timer.cancel();
                this.timer.purge();
                this.timer = null;
            }
            this.cancelado = true;
        }
    }
}
