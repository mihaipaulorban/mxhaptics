package dev.mxhaptics.bridge;

import dev.mxhaptics.MxHapticsClient;
import dev.mxhaptics.config.ConfigManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ponte verso il plugin aptico locale di Logi Options+ (es. HapticWeb).
 *
 * Protocollo: frame binario il cui primo byte è l'indice del pattern (0-15).
 * Come estensione forward-compatible inviamo un secondo byte con
 * l'intensità (0-100): i plugin che non la supportano leggono solo il primo
 * byte e la ignorano (vedi README).
 *
 * Tutto è asincrono su un singolo thread daemon dedicato: nessuna chiamata
 * di rete tocca mai il thread di gioco. Se il plugin non è disponibile la
 * mod continua a funzionare e si limita a un log discreto, riprovando a
 * connettersi con backoff esponenziale.
 */
public final class HapticBridge implements WebSocket.Listener {

    private static final long BACKOFF_MIN_SECONDS = 1;
    private static final long BACKOFF_MAX_SECONDS = 60;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread t = new Thread(runnable, "mxhaptics-bridge");
                t.setDaemon(true);
                return t;
            });

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean unavailableLogged = new AtomicBoolean(false);

    private volatile WebSocket socket;
    private volatile long backoffSeconds = BACKOFF_MIN_SECONDS;

    /** Catena di invii: WebSocket richiede che ogni send parta dopo il precedente. */
    private CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);

    /** Avvia il primo tentativo di connessione (non bloccante). */
    public void start() {
        executor.execute(this::attemptConnect);
    }

    /** Chiude la connessione (se presente) e riparte con la config attuale. */
    public void restart() {
        executor.execute(() -> {
            WebSocket ws = socket;
            socket = null;
            if (ws != null) {
                try {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "reconfigure");
                } catch (RuntimeException ignored) {
                }
            }
            backoffSeconds = BACKOFF_MIN_SECONDS;
            unavailableLogged.set(false);
            attemptConnect();
        });
    }

    /** Da chiamare alla chiusura del gioco. */
    public void shutdown() {
        shuttingDown.set(true);
        WebSocket ws = socket;
        socket = null;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (RuntimeException ignored) {
            }
        }
        executor.shutdown();
    }

    /** True se in questo momento c'è una connessione aperta col plugin. */
    public boolean isConnected() {
        return socket != null;
    }

    /**
     * Invia (in modo asincrono) un trigger aptico.
     * Se il bridge non è connesso il trigger viene semplicemente ignorato.
     *
     * @param pattern   indice del pattern 0-15
     * @param intensity intensità 0-100 (secondo byte, opzionale lato plugin)
     */
    public void trigger(int pattern, int intensity) {
        WebSocket ws = socket;
        if (ws == null) {
            return;
        }
        byte[] payload = {
                (byte) (Math.max(0, Math.min(15, pattern))),
                (byte) (Math.max(0, Math.min(100, intensity)))
        };
        synchronized (this) {
            sendChain = sendChain
                    .exceptionally(err -> null)
                    .thenComposeAsync(ignored -> {
                        WebSocket current = socket;
                        if (current == null) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return current.sendBinary(ByteBuffer.wrap(payload), true)
                                .handle((w, err) -> null);
                    }, executor);
        }
    }

    // ------------------------------------------------------------------
    // Connessione
    // ------------------------------------------------------------------

    private void attemptConnect() {
        if (shuttingDown.get()) {
            return;
        }
        String url = ConfigManager.get().bridgeUrl;
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            MxHapticsClient.LOGGER.warn("[mxhaptics] URL del bridge non valido: {}", url);
            return;
        }

        buildHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(uri, this)
                .whenComplete((ws, err) -> {
                    if (err != null || ws == null) {
                        onConnectFailed();
                    } else {
                        socket = ws;
                        backoffSeconds = BACKOFF_MIN_SECONDS;
                        unavailableLogged.set(false);
                        MxHapticsClient.LOGGER.info(
                                "[mxhaptics] haptic bridge connesso a {}", url);
                    }
                });
    }

    private void onConnectFailed() {
        if (shuttingDown.get()) {
            return;
        }
        if (unavailableLogged.compareAndSet(false, true)) {
            // Un solo log, non invasivo: il gioco continua normalmente.
            MxHapticsClient.LOGGER.info(
                    "[mxhaptics] haptic bridge non disponibile "
                            + "(Logi Options+ o il plugin non sono in esecuzione?). "
                            + "Riproverò in background.");
        }
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (shuttingDown.get()) {
            return;
        }
        long delay = backoffSeconds;
        backoffSeconds = Math.min(BACKOFF_MAX_SECONDS, backoffSeconds * 2);
        executor.schedule(this::attemptConnect, delay, TimeUnit.SECONDS);
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(Duration.ofSeconds(5));
        if (ConfigManager.get().trustAllCertificates) {
            SSLContext ctx = insecureSslContext();
            if (ctx != null) {
                builder.sslContext(ctx);
            }
        }
        return builder.build();
    }

    /**
     * SSLContext che accetta qualsiasi certificato. Usato SOLO se l'utente
     * attiva esplicitamente trustAllCertificates per un plugin locale
     * self-signed. Il traffico resta comunque su localhost.
     */
    private static SSLContext insecureSslContext() {
        try {
            TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            MxHapticsClient.LOGGER.warn(
                    "[mxhaptics] impossibile creare l'SSLContext permissivo", e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // WebSocket.Listener
    // ------------------------------------------------------------------

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        // Eventuali messaggi dal plugin (ack, info) vengono ignorati.
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (socket == webSocket) {
            socket = null;
            MxHapticsClient.LOGGER.info(
                    "[mxhaptics] haptic bridge disconnesso ({}), riconnessione in corso", statusCode);
            scheduleReconnect();
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (socket == webSocket) {
            socket = null;
            MxHapticsClient.LOGGER.debug("[mxhaptics] errore sul bridge", error);
            scheduleReconnect();
        }
    }
}
