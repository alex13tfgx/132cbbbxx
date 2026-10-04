package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MusicInfo
extends AbstractDraggable {
    private static final Identifier ARTWORK = Identifier.of((String)"wiosvisual", (String)"dynamic/music_artwork");
    private static final int WIDTH = 112;
    private static final int HEIGHT = 40;
    private static final float PADDING = 4.0f;
    private static final float COVER_SIZE = 32.0f;
    private static final long POLL_INTERVAL_MS = 500L;
    private static final long MEDIA_TIMEOUT_MS = 5000L;

    private final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Wiosvisual-MediaInfo");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean polling = new AtomicBoolean();
    private final FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.SuisseIntlSemiBold);
    private final FontRenderer artistFont = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
    private final FontRenderer timeFont = Fonts.getSize(11, Fonts.Type.SuisseIntlMedium);

    private volatile MediaInfo mediaInfo;
    private volatile IMediaSession session;
    private volatile String sessionOwner = "";
    private volatile long lastPollTime;
    private volatile long lastMediaTime;
    private volatile boolean mediaAvailable = true;
    private volatile boolean artworkReady;
    private volatile int artworkHash;
    private float animatedProgress;

    private static final int SPOTIFY_GREEN = 0x1DB954;
    /** Высота полосы караоке под основной панелью. */
    private static final float KARAOKE_HEIGHT = 24.0f;
    /** Сколько считать последнюю строку текста, если за ней ничего нет. */
    private static final long LAST_LINE_MS = 4000L;
    private final Lyrics lyrics = new Lyrics();
    private final FontRenderer lyricFont = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
    private final FontRenderer nextLyricFont = Fonts.getSize(11, Fonts.Type.SuisseIntlMedium);
    /** Локальные часы трека: плеер отдаёт позицию по секундам и не каждый опрос. */
    private String clockTrack = "";
    private boolean clockPlaying;
    private long clockBase;
    private long clockAt;
    private long clockReported = -1L;
    private long clockPollAt;
    /** Подсветка чуть впереди звука: без этого глаз воспринимает строку как запаздывающую. */
    private static final long KARAOKE_LEAD_MS = 250L;
    /** Опрос плеера при караоке: чем чаще, тем точнее ловится смена секунды. */
    private static final long KARAOKE_POLL_MS = 100L;

    public MusicInfo() {
        super("Music Info", 10, 250, 112, 40, true);
        this.stopAnimation();
    }

    public static MusicInfo getInstance() {
        return Instance.getDraggable(MusicInfo.class);
    }

    @Override
    public boolean visible() {
        Hud.getInstance();
        if (!Hud.interfaceSettings.isSelected("Music Info")) {
            return false;
        }
        if (Hud.getInstance() == null || !Hud.getInstance().state) {
            return false;
        }
        return PlayerInteractionHelper.isChat(MusicInfo.mc.currentScreen) || this.hasRecentMedia() || !this.scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        long now;
        if (MusicInfo.mc.player == null) {
            this.stopAnimation();
            return;
        }
        // Элемент убран с HUD (или HUD выключен): гасим анимацию и не опрашиваем
        // плеер. Иначе tick продолжает дёргать scaleAnimation в FORWARDS, из-за
        // чего элемент мигает после удаления.
        Hud hudModule = Hud.getInstance();
        if (!Hud.interfaceSettings.isSelected("Music Info") || hudModule == null || !hudModule.state) {
            this.stopAnimation();
            return;
        }
        if (this.mediaAvailable && (now = System.currentTimeMillis()) - this.lastPollTime >= (Hud.musicKaraoke.isValue() ? KARAOKE_POLL_MS : 500L) && this.polling.compareAndSet(false, true)) {
            this.lastPollTime = now;
            this.mediaExecutor.execute(this::pollMedia);
        }
        if (this.hasRecentMedia() || PlayerInteractionHelper.isChat(MusicInfo.mc.currentScreen)) {
            this.startAnimation();
        } else {
            this.stopAnimation();
        }
    }

    private void pollMedia() {
        try {
            MediaInfo info;
            List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
            IMediaSession current = this.selectSession(sessions);
            if (current == null) {
                return;
            }
            if ((info = current.getMedia()) == null || info.getTitle().isBlank() && info.getArtist().isBlank()) {
                return;
            }
            this.mediaInfo = info;
            this.session = current;
            this.sessionOwner = current.getOwner() == null ? "" : current.getOwner();
            this.lastMediaTime = System.currentTimeMillis();
            this.syncClock(info, this.lastMediaTime);
            this.updateArtwork(info.getArtworkPng());
        }
        catch (Throwable error) {
            // Без медиасессий Windows караоке и плеер не работают — пишем причину один раз.
            if (this.mediaAvailable) {
                org.apache.logging.log4j.LogManager.getLogger("Wiosvisual").warn("[MusicInfo] media sessions unavailable: {}", error.toString());
            }
            this.mediaAvailable = false;
        }
        finally {
            this.polling.set(false);
        }
    }

    private IMediaSession selectSession(List<IMediaSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }
        // Сначала играющий плеер, при равенстве — Spotify.
        return sessions.stream().filter(candidate -> candidate != null && candidate.getMedia() != null).max(Comparator.comparing((IMediaSession candidate) -> {
            try {
                return candidate.getMedia().getPlaying();
            }
            catch (Throwable ignored) {
                return false;
            }
        }).thenComparing(candidate -> MusicInfo.isSpotify(candidate.getOwner()))).orElse(null);
    }

    private void updateArtwork(byte[] artworkBytes) {
        if (artworkBytes == null || artworkBytes.length == 0) {
            this.artworkReady = false;
            this.artworkHash = 0;
            return;
        }
        int hash = Arrays.hashCode(artworkBytes);
        if (hash == this.artworkHash && this.artworkReady) {
            return;
        }
        byte[] imageData = Arrays.copyOf(artworkBytes, artworkBytes.length);
        MusicInfo.mc.execute(() -> {
            try {
                NativeImage image = NativeImage.read((byte[])imageData);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "wios", image);
                MusicInfo.mc.getTextureManager().destroyTexture(ARTWORK);
                MusicInfo.mc.getTextureManager().registerTexture(ARTWORK, (net.minecraft.client.texture.AbstractTexture)texture);
                this.artworkHash = hash;
                this.artworkReady = true;
            }
            catch (Throwable ignored) {
                this.artworkReady = false;
            }
        });
    }

    private boolean hasRecentMedia() {
        return this.mediaInfo != null && System.currentTimeMillis() - this.lastMediaTime <= 5000L;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = GuiCompat.stack(context);
        float x = this.getX();
        float y = this.getY();
        int alpha = 255;
        float alphaFactor = 1.0f;
        MediaInfo info = this.mediaInfo;
        boolean preview = info == null || !this.hasRecentMedia();
        boolean spotify = !preview && MusicInfo.isSpotify(this.sessionOwner);
        List<Lyrics.Line> lines = preview || !Hud.musicKaraoke.isValue() ? null
                : this.lyrics.get(MusicInfo.safeText(info.getArtist(), ""), MusicInfo.safeText(info.getTitle(), ""), info.getDuration());
        boolean karaoke = lines != null && !lines.isEmpty();
        float height = 40.0f + (karaoke ? KARAOKE_HEIGHT : 0.0f);
        this.setWidth(112);
        this.setHeight((int)height);

        rectangle.render(ShapeProperties.create(matrix, x, y, 112.0, height).round(6.0f).softness(2.0f).color(HudTheme.panelTop(alpha), HudTheme.panel(alpha), HudTheme.panelBottom(alpha), HudTheme.item(alpha)).build());
        Rectangle.flushPending();

        float coverX = x + 4.0f;
        float coverY = y + 4.0f;
        this.drawArtwork(context, matrix, coverX, coverY, alpha, preview);
        int source = preview ? -1 : MusicInfo.sourceColor(this.sessionOwner);
        if (spotify) {
            this.drawSpotifyBadge(matrix, coverX + 32.0f - 9.0f, coverY + 32.0f - 9.0f);
        } else if (source != -1) {
            this.drawSourceBadge(matrix, coverX + 32.0f - 9.0f, coverY + 32.0f - 9.0f, source);
        }

        float contentX = coverX + 32.0f + 5.0f;
        float contentWidth = 112.0f - (contentX - x) - 4.0f;
        String title = preview ? "No media playing" : MusicInfo.safeText(info.getTitle(), "Unknown track");
        String artist = preview ? "Open your music player" : MusicInfo.safeText(info.getArtist(), "Unknown artist");
        this.titleFont.drawStringWithScroll(matrix, title, contentX, y + 4.0f, contentWidth, HudTheme.text(alpha));
        this.artistFont.drawStringWithScroll(matrix, artist, contentX, y + 12.0f, contentWidth, HudTheme.muted(alpha));

        float targetProgress = preview || info.getDuration() <= 0L ? 0.0f : MusicInfo.clamp((float)info.getPosition() / (float)info.getDuration());
        this.animatedProgress += (targetProgress - this.animatedProgress) * 0.18f;

        float progressY = y + 21.5f;
        rectangle.render(ShapeProperties.create(matrix, contentX, progressY, contentWidth, 1.5).round(0.75f).color(HudTheme.track((int)(220.0f * alphaFactor))).build());
        if (this.animatedProgress > 0.001f) {
            rectangle.render(ShapeProperties.create(matrix, contentX, progressY, contentWidth * this.animatedProgress, 1.5).round(0.75f).color(spotify ? 0xFF000000 | SPOTIFY_GREEN : HudTheme.musicIcon(alpha)).build());
        }
        Rectangle.flushPending();

        String elapsed = preview ? "0:00" : MusicInfo.formatTime(info.getPosition());
        String duration = preview ? "0:00" : MusicInfo.formatTime(info.getDuration());
        this.timeFont.drawCenteredString(matrix, elapsed + " / " + duration, contentX + contentWidth / 2.0f, y + 24.5f, HudTheme.text(alpha));

        this.drawControls(matrix, contentX, contentWidth, y + 29.0f, alpha, preview || !this.mediaAvailable, !preview && info.getPlaying());
        if (karaoke) {
            this.drawKaraoke(matrix, lines, this.positionMs(info), x + 4.0f, y + 40.0f, 104.0f, spotify);
        }
    }

    private static boolean isSpotify(String owner) {
        return owner != null && owner.toLowerCase().contains("spotify");
    }

    private static final int YOUTUBE_RED = 0xFF0033;
    private static final int VK_BLUE = 0x0077FF;

    /** Приложения YouTube Music и VK Музыка видны по владельцу сессии; вкладка браузера — нет. */
    private static int sourceColor(String owner) {
        String lower = owner == null ? "" : owner.toLowerCase();
        if (lower.contains("youtube")) {
            return YOUTUBE_RED;
        }
        if (lower.contains("vk") || lower.contains("vkontakte")) {
            return VK_BLUE;
        }
        return -1;
    }

    /** Значок источника: цветной круг с белым «play» (YouTube) или тремя полосками (VK). */
    private void drawSourceBadge(MatrixStack matrix, float x, float y, int color) {
        rectangle.render(ShapeProperties.create(matrix, x - 0.5f, y - 0.5f, 10.0, 10.0).round(5.0f).color(0xFF000000).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, 9.0, 9.0).round(4.5f).color(0xFF000000 | color).build());
        if (color == YOUTUBE_RED) {
            for (int i = 0; i < 3; ++i) {
                float h = 3.6f - (float)i * 1.2f;
                rectangle.render(ShapeProperties.create(matrix, x + 3.4f + (float)i * 1.0f, y + 4.5f - h / 2.0f, 1.0, h).round(0.3f).color(0xFFFFFFFF).build());
            }
        } else {
            rectangle.render(ShapeProperties.create(matrix, x + 2.2f, y + 3.0f, 1.1, 3.0).round(0.4f).color(0xFFFFFFFF).build());
            rectangle.render(ShapeProperties.create(matrix, x + 4.0f, y + 3.0f, 1.1, 3.0).round(0.4f).color(0xFFFFFFFF).build());
            rectangle.render(ShapeProperties.create(matrix, x + 5.8f, y + 3.0f, 1.1, 2.0).round(0.4f).color(0xFFFFFFFF).build());
        }
        Rectangle.flushPending();
    }

    /** Значок Spotify: зелёный круг с тремя дугами-полосками. */
    private void drawSpotifyBadge(MatrixStack matrix, float x, float y) {
        rectangle.render(ShapeProperties.create(matrix, x - 0.5f, y - 0.5f, 10.0, 10.0).round(5.0f).color(0xFF000000).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, 9.0, 9.0).round(4.5f).color(0xFF000000 | SPOTIFY_GREEN).build());
        rectangle.render(ShapeProperties.create(matrix, x + 2.0f, y + 2.6f, 5.2, 1.0).round(0.5f).color(0xFF0B0B0B).build());
        rectangle.render(ShapeProperties.create(matrix, x + 2.4f, y + 4.3f, 4.4, 0.9).round(0.45f).color(0xFF0B0B0B).build());
        rectangle.render(ShapeProperties.create(matrix, x + 2.8f, y + 5.9f, 3.6, 0.8).round(0.4f).color(0xFF0B0B0B).build());
        Rectangle.flushPending();
    }

    /**
     * Подводит локальные часы трека по очередному опросу плеера. Плеер отдаёт позицию
     * целыми секундами (с округлением вниз), поэтому ценен момент, когда секунда
     * сменилась: настоящая позиция тогда ровно N секунд плюс половина интервала опроса.
     */
    private synchronized void syncClock(MediaInfo info, long now) {
        String track = info.getArtist() + "|" + info.getTitle();
        long reportedSec = info.getPosition();
        boolean playing = info.getPlaying();
        long predicted = this.clockBase + (this.clockPlaying ? now - this.clockAt : 0L);
        // Без перехода секунды знаем только, что позиция где-то внутри неё — берём середину.
        long rough = reportedSec * 1000L + (reportedSec > 0L ? 500L : 0L);
        if (!track.equals(this.clockTrack)) {
            this.setClock(track, playing, rough, now);
        } else if (playing != this.clockPlaying) {
            // Пауза или продолжение: накопленное время точнее грубой оценки, если не было перемотки.
            this.setClock(track, playing, Math.abs(predicted - rough) < 1500L ? predicted : rough, now);
        } else if (playing && reportedSec == this.clockReported + 1L && this.clockPollAt > 0L) {
            long exact = reportedSec * 1000L + (now - this.clockPollAt) / 2L;
            if (Math.abs(predicted - exact) > 60L) {
                this.setClock(track, true, exact, now);
            }
        } else if (Math.abs(predicted - rough) > 1500L) {
            // Перемотка.
            this.setClock(track, playing, rough, now);
        }
        this.clockReported = reportedSec;
        this.clockPollAt = now;
    }

    private void setClock(String track, boolean playing, long base, long now) {
        this.clockTrack = track;
        this.clockPlaying = playing;
        this.clockBase = base;
        this.clockAt = now;
    }

    /** Позиция трека в миллисекундах по локальным часам, с небольшим опережением для подсветки. */
    private synchronized long positionMs(MediaInfo info) {
        long now = System.currentTimeMillis();
        long position = this.clockBase + (this.clockPlaying ? now - this.clockAt : 0L) + KARAOKE_LEAD_MS;
        long duration = info.getDuration() * 1000L;
        return duration > 0L ? Math.min(position, duration) : position;
    }

    /** Текущая строка заливается цветом слева направо, под ней бледно — следующая. */
    private void drawKaraoke(MatrixStack matrix, List<Lyrics.Line> lines, long position, float x, float y, float width, boolean spotify) {
        int index = -1;
        for (int i = 0; i < lines.size() && lines.get(i).timeMs() <= position; ++i) {
            index = i;
        }
        String current = index < 0 ? "\u266a" : lines.get(index).text();
        String next = index + 1 < lines.size() ? lines.get(index + 1).text() : "";
        float fill = 0.0f;
        if (index >= 0) {
            long start = lines.get(index).timeMs();
            long end = index + 1 < lines.size() ? lines.get(index + 1).timeMs() : start + LAST_LINE_MS;
            // Строка поётся не до самой следующей: после неё бывает проигрыш. Длительность
            // пения оцениваем по числу букв и не даём заливке растянуться на паузу.
            long sung = Math.max(800L, Math.min(6000L, (long)lines.get(index).text().length() * 70L));
            end = Math.min(end, start + sung);
            fill = end <= start ? 1.0f : MusicInfo.clamp((float)(position - start) / (float)(end - start));
        }
        int accent = spotify ? 0xFF000000 | SPOTIFY_GREEN : HudTheme.musicIconBright(255);
        this.drawFilledLine(matrix, this.lyricFont, current, x, y + 1.5f, width, fill, accent, HudTheme.text(255));
        if (!next.isEmpty()) {
            this.nextLyricFont.drawStringWithScroll(matrix, next, x, y + 12.5f, width, HudTheme.muted(255));
        }
    }

    /**
     * Строка караоке по буквам: пропетая часть цветом акцента, остальное — цветом текста.
     * Длинная строка сдвигается так, чтобы граница заливки оставалась в поле зрения.
     */
    private void drawFilledLine(MatrixStack matrix, FontRenderer font, String text, float x, float y, float width, float fill, int sung, int rest) {
        float total = font.getStringWidth(text);
        float fillX = total * fill;
        float offset = total <= width ? (width - total) / 2.0f : -Math.max(0.0f, Math.min(total - width, fillX - width * 0.6f));
        float cursor = 0.0f;
        for (int i = 0; i < text.length(); ++i) {
            String glyph = String.valueOf(text.charAt(i));
            float glyphWidth = font.getStringWidth(glyph);
            float left = cursor + offset;
            if (left >= 0.0f && left + glyphWidth <= width + 0.5f) {
                float mid = cursor + glyphWidth / 2.0f;
                font.drawString(matrix, glyph, x + left, y, mid <= fillX ? sung : rest);
            }
            cursor += glyphWidth;
        }
    }

    private void drawArtwork(DrawContext context, MatrixStack matrix, float x, float y, int alpha, boolean preview) {
        if (!preview && this.artworkReady) {
            rectangle.render(ShapeProperties.create(matrix, x, y, 32.0, 32.0).round(4.0f).color(HudTheme.item(alpha)).build());
            Rectangle.flushPending();
            Render2D.drawTexturedQuad(matrix, ARTWORK, x, x + 32.0f, y, y + 32.0f, 0.0f, 1.0f, 0.0f, 1.0f, alpha << 24 | 0xFFFFFF);
            return;
        }
        rectangle.render(ShapeProperties.create(matrix, x, y, 32.0, 32.0).round(4.0f).color(HudTheme.itemSoft(alpha), HudTheme.itemSoft(alpha), HudTheme.track(alpha), HudTheme.item(alpha)).build());
        Rectangle.flushPending();
        float noteX = x + 16.0f;
        float noteY = y + 11.0f;
        rectangle.render(ShapeProperties.create(matrix, noteX - 1.0f, noteY, 1.8, 9.0).round(0.9f).color(HudTheme.musicIcon(alpha)).build());
        rectangle.render(ShapeProperties.create(matrix, noteX - 1.0f, noteY, 6.0, 1.8).round(0.9f).color(HudTheme.musicIcon(alpha)).build());
        rectangle.render(ShapeProperties.create(matrix, noteX - 4.5f, noteY + 7.5f, 4.5, 3.5).round(1.75f).color(HudTheme.musicIcon(alpha)).build());
        Rectangle.flushPending();
    }

    private void drawControls(MatrixStack matrix, float contentX, float contentWidth, float y, int alpha, boolean disabled, boolean playing) {
        float buttonSize = 8.0f;
        float gap = 3.0f;
        float rowWidth = buttonSize * 3.0f + gap * 2.0f;
        float startX = contentX + (contentWidth - rowWidth) / 2.0f;
        int iconColor = disabled ? HudTheme.muted((int)(135.0f * ((float)alpha / 255.0f))) : HudTheme.musicIcon(alpha);
        this.drawControlButton(matrix, startX, y, buttonSize, alpha);
        this.drawControlButton(matrix, startX + buttonSize + gap, y, buttonSize, alpha);
        this.drawControlButton(matrix, startX + (buttonSize + gap) * 2.0f, y, buttonSize, alpha);
        Rectangle.flushPending();
        this.drawPrevIcon(matrix, startX, y, buttonSize, iconColor);
        if (playing) {
            this.drawPauseIcon(matrix, startX + buttonSize + gap, y, buttonSize, iconColor);
        } else {
            this.drawPlayIcon(matrix, startX + buttonSize + gap, y, buttonSize, iconColor);
        }
        this.drawNextIcon(matrix, startX + (buttonSize + gap) * 2.0f, y, buttonSize, iconColor);
        Rectangle.flushPending();
    }

    private void drawControlButton(MatrixStack matrix, float x, float y, float size, int alpha) {
        rectangle.render(ShapeProperties.create(matrix, x, y, size, size).round(2.5f).color(HudTheme.itemSoft((int)(220.0f * ((float)alpha / 255.0f)))).build());
    }

    private void drawPlayIcon(MatrixStack matrix, float x, float y, float size, int color) {
        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        for (int i = 0; i < 4; ++i) {
            float h = 3.6f - (float)i * 0.8f;
            rectangle.render(ShapeProperties.create(matrix, cx - 1.6f + (float)i * 0.8f, cy - h / 2.0f, 0.9, h).round(0.35f).color(color).build());
        }
    }

    private void drawPauseIcon(MatrixStack matrix, float x, float y, float size, int color) {
        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        rectangle.render(ShapeProperties.create(matrix, cx - 1.7f, cy - 1.8f, 1.2, 3.6).round(0.4f).color(color).build());
        rectangle.render(ShapeProperties.create(matrix, cx + 0.5f, cy - 1.8f, 1.2, 3.6).round(0.4f).color(color).build());
    }

    private void drawPrevIcon(MatrixStack matrix, float x, float y, float size, int color) {
        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        rectangle.render(ShapeProperties.create(matrix, cx - 2.2f, cy - 1.8f, 0.9, 3.6).round(0.35f).color(color).build());
        for (int i = 0; i < 3; ++i) {
            float h = 1.6f + (float)i * 0.8f;
            rectangle.render(ShapeProperties.create(matrix, cx - 0.9f + (float)i * 0.8f, cy - h / 2.0f, 0.9, h).round(0.35f).color(color).build());
        }
    }

    private void drawNextIcon(MatrixStack matrix, float x, float y, float size, int color) {
        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        for (int i = 0; i < 3; ++i) {
            float h = 3.2f - (float)i * 0.8f;
            rectangle.render(ShapeProperties.create(matrix, cx - 2.2f + (float)i * 0.8f, cy - h / 2.0f, 0.9, h).round(0.35f).color(color).build());
        }
        rectangle.render(ShapeProperties.create(matrix, cx + 1.3f, cy - 1.8f, 0.9, 3.6).round(0.35f).color(color).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || this.mediaInfo == null || !this.hasRecentMedia() || !this.mediaAvailable) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        float contentX = (float)this.getX() + 4.0f + 32.0f + 5.0f;
        float contentWidth = 112.0f - (contentX - (float)this.getX()) - 4.0f;
        float buttonSize = 8.0f;
        float gap = 3.0f;
        float startX = contentX + (contentWidth - buttonSize * 3.0f - gap * 2.0f) / 2.0f;
        float buttonY = (float)this.getY() + 29.0f;
        if (MusicInfo.inside(mouseX, mouseY, startX, buttonY, buttonSize)) {
            this.control(IMediaSession::previous);
            return true;
        }
        if (MusicInfo.inside(mouseX, mouseY, startX + buttonSize + gap, buttonY, buttonSize)) {
            this.control(IMediaSession::playPause);
            return true;
        }
        if (MusicInfo.inside(mouseX, mouseY, startX + (buttonSize + gap) * 2.0f, buttonY, buttonSize)) {
            this.control(IMediaSession::next);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mouseX, double mouseY, float x, float y, float size) {
        return mouseX >= (double)x && mouseX <= (double)(x + size) && mouseY >= (double)y && mouseY <= (double)(y + size);
    }

    private void control(Consumer<IMediaSession> action) {
        this.mediaExecutor.execute(() -> {
            try {
                List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                IMediaSession target = sessions.stream().filter(candidate -> candidate != null && candidate.getMedia() != null).filter(candidate -> this.sessionOwner.isEmpty() || this.sessionOwner.equals(candidate.getOwner())).filter(candidate -> this.mediaInfo == null || this.mediaInfo.getTitle().equals(candidate.getMedia().getTitle())).findFirst().orElse(this.session);
                if (target != null) {
                    action.accept(target);
                    this.lastPollTime = 0L;
                }
            }
            catch (Throwable ignored) {
                this.mediaAvailable = false;
            }
        });
    }

    private static String safeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private static String formatTime(long seconds) {
        long totalSeconds = Math.max(0L, seconds);
        return totalSeconds / 60L + ":" + String.format("%02d", totalSeconds % 60L);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
