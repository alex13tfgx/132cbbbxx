package guardlisten;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Единый кадровый движок лаунчера.
 *
 * javax.swing.Timer в Windows не тикает чаще ~15.7 мс, а у каждой анимации был свой таймер —
 * кадры шли неравномерно (визуально ~30-45 fps). Здесь один поток с точным Thread.sleep
 * задаёт ритм под частоту монитора, а на каждом кадре в EDT все анимации делают шаг по
 * реальному прошедшему времени и сразу отрисовываются одним проходом.
 */
final class Anim {
    /** Шаг анимации; dt — секунды с прошлого кадра. Вернуть false, когда анимация закончилась. */
    interface Ticker { boolean tick(float dt); }

    private static final List<Ticker> TICKERS = new ArrayList<>(); // только из EDT
    private static final Object LOCK = new Object();
    private static final AtomicBoolean PENDING = new AtomicBoolean();
    private static boolean running;
    private static Thread thread;
    private static long last;
    private static int refresh = 60;
    /** Ограничение fps (например, при тяжёлых частицах на мониторах 144+ Гц). */
    static volatile int cap = 144;

    private Anim() { }

    /** Добавляет анимацию (вызывать из EDT). Повторное добавление того же объекта игнорируется. */
    static void add(Ticker t) {
        if (!TICKERS.contains(t)) TICKERS.add(t);
        if (thread == null) startThread();
        synchronized (LOCK) {
            if (!running) {
                running = true;
                LOCK.notifyAll();
            }
        }
    }

    /** Целевая частота: частота монитора, при необходимости делённая пополам до лимита. */
    private static int fps() {
        int f = refresh;
        while (f > cap && f >= 100) f /= 2;
        return Math.max(50, Math.min(f, cap));
    }

    private static void startThread() {
        try {
            int r = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getRefreshRate();
            if (r >= 50 && r <= 360) refresh = r;
        } catch (Throwable ignored) { }
        thread = new Thread(Anim::loop, "gl-frame-clock");
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private static void loop() {
        long next = System.nanoTime();
        while (true) {
            synchronized (LOCK) {
                while (!running) {
                    try { LOCK.wait(); } catch (InterruptedException e) { return; }
                    next = System.nanoTime();
                }
            }
            long period = 1_000_000_000L / fps();
            next += period;
            long now = System.nanoTime();
            if (next < now - period) next = now; // сильно отстали (EDT был занят) — не догоняем пачкой
            long wait = next - now;
            if (wait > 0) {
                try { Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000)); } catch (InterruptedException e) { return; }
            }
            // если EDT ещё не отрисовал прошлый кадр — не копим очередь
            if (PENDING.compareAndSet(false, true)) SwingUtilities.invokeLater(Anim::frame);
        }
    }

    private static void frame() {
        PENDING.set(false);
        long now = System.nanoTime();
        float dt = last == 0 ? 1f / fps() : Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        for (int i = TICKERS.size() - 1; i >= 0; i--) {
            boolean keep;
            try { keep = TICKERS.get(i).tick(dt); } catch (Throwable t) { keep = false; }
            if (!keep) TICKERS.remove(i);
        }
        // рисуем всё накопленное прямо в этом кадре, а не «когда-нибудь потом»
        RepaintManager.currentManager((JComponent) null).paintDirtyRegions();
        if (TICKERS.isEmpty()) {
            last = 0;
            synchronized (LOCK) { running = false; }
        }
    }

    /** Экспоненциальное сглаживание, не зависящее от частоты кадров. */
    static float approach(float v, float target, float speed, float dt) {
        v += (target - v) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(target - v) < 0.003f ? target : v;
    }

    static float easeOut(float x) {
        x = Math.max(0f, Math.min(1f, x));
        float k = 1f - x;
        return 1f - k * k * k;
    }
}
