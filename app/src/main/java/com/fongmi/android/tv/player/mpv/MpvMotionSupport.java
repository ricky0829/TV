package com.fongmi.android.tv.player.mpv;

import androidx.annotation.Nullable;
import androidx.media3.mpvplayer.MpvPlayer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.lang.reflect.Method;

public final class MpvMotionSupport {

    private static final Method ADD_LISTENER = find("addMotionInterpolationListener", Runnable.class);
    private static final Method REMOVE_LISTENER = find("removeMotionInterpolationListener", Runnable.class);
    private static final Method IS_SUPPORTED = find("isMotionInterpolationSupported");
    private static final Method IS_PENDING = find("isMotionInterpolationUpdatePending");
    private static final Method GET_FPS = find("getMotionInterpolationFps");
    private static final Method GET_EFFECTIVE_FPS = find("getMotionInterpolationEffectiveTargetFps");
    private static final Method SET_FPS = find("setMotionInterpolationFps", int.class);

    private MpvMotionSupport() {
    }

    public static boolean isAvailable() {
        return SET_FPS != null;
    }

    public static void addListener(MpvPlayer player, Runnable listener) {
        invoke(ADD_LISTENER, player, listener);
    }

    public static void removeListener(MpvPlayer player, Runnable listener) {
        invoke(REMOVE_LISTENER, player, listener);
    }

    public static boolean isSupported(MpvPlayer player) {
        Object result = invoke(IS_SUPPORTED, player);
        return result instanceof Boolean value && value;
    }

    public static boolean isUpdatePending(MpvPlayer player) {
        Object result = invoke(IS_PENDING, player);
        return result instanceof Boolean value && value;
    }

    public static int getFps(MpvPlayer player) {
        Object result = invoke(GET_FPS, player);
        return result instanceof Integer value ? value : 0;
    }

    public static float getEffectiveFps(MpvPlayer player) {
        Object result = invoke(GET_EFFECTIVE_FPS, player);
        return result instanceof Number value ? value.floatValue() : 0;
    }

    public static void setFps(MpvPlayer player, int fps, Callback callback) {
        if (SET_FPS == null) {
            callback.onFailure(new UnsupportedOperationException("motion interpolation"));
            return;
        }
        try {
            Object result = SET_FPS.invoke(player, fps);
            if (result instanceof ListenableFuture<?> future) {
                Futures.addCallback(future, new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(@Nullable Object value) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        callback.onFailure(error);
                    }
                }, MoreExecutors.directExecutor());
            } else {
                callback.onSuccess();
            }
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static Method find(String name, Class<?>... parameters) {
        try {
            return MpvPlayer.class.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Object invoke(Method method, MpvPlayer player, Object... arguments) {
        if (method == null) return null;
        try {
            return method.invoke(player, arguments);
        } catch (Exception e) {
            return null;
        }
    }

    public interface Callback {

        void onSuccess();

        void onFailure(Throwable error);
    }
}
