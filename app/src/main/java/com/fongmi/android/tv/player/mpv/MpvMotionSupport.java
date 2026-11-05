package com.fongmi.android.tv.player.mpv;

import android.app.Activity;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.nativebridge.MpvPlaybackProperties;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.PlayerSetting;

import java.lang.reflect.Field;

public final class MpvMotionSupport {

    private static final Field PLAYBACK_PROPERTIES = findPlaybackProperties();

    private MpvMotionSupport() {
    }

    public static boolean isSupported(@Nullable MpvPlayer player) {
        return player != null && MpvPlayer.isAvailable() && MpvUtil.isVulkanSupported();
    }

    public static int getFps() {
        return PlayerSetting.getMpvMotionFps();
    }

    public static float getEffectiveFps() {
        Activity activity = App.activity();
        if (activity == null) return getFps();
        Display display = activity.getWindowManager().getDefaultDisplay();
        return display == null ? getFps() : display.getRefreshRate();
    }

    public static boolean setFps(@Nullable MpvPlayer player, int fps) {
        if (player == null) return false;
        boolean enabled = fps > 0 && MpvUtil.isVulkanSupported();
        if (!setProperty(player, "interpolation", enabled ? "yes" : "no")) return false;
        if (enabled) {
            setProperty(player, "video-sync", "display-resample");
            setProperty(player, "tscale", "oversample");
        }
        setPreferredRefreshRate(enabled ? fps : 0);
        return true;
    }

    public static void applyDisplayMode() {
        setPreferredRefreshRate(getFps());
    }

    private static boolean setProperty(MpvPlayer player, String name, String value) {
        if (PLAYBACK_PROPERTIES == null) return false;
        try {
            MpvPlaybackProperties properties = (MpvPlaybackProperties) PLAYBACK_PROPERTIES.get(player);
            if (properties == null) return false;
            properties.setStringOptionOrProperty(name, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setPreferredRefreshRate(int fps) {
        Activity activity = App.activity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            Window window = activity.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            params.preferredRefreshRate = fps > 0 ? fps : 0f;
            window.setAttributes(params);
        });
    }

    private static Field findPlaybackProperties() {
        try {
            Field field = MpvPlayer.class.getDeclaredField("playbackProperties");
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            return null;
        }
    }
}
