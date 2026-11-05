package com.fongmi.android.tv.player.exo;

import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ExoAudioTracks {

    private ExoAudioTracks() {
    }

    static boolean cycle(Player player, int trackType, boolean reverse, boolean includeNone) {
        return cycle(player, trackType, null, reverse, includeNone);
    }

    static boolean cycle(Player player, int trackType, List<String> trackIds, boolean reverse, boolean includeNone) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return false;
        List<Option> options = collect(player.getCurrentTracks(), trackType, trackIds);
        if (options.isEmpty()) return false;
        int offset = includeNone ? 1 : 0;
        int size = options.size() + offset;
        int current = selectedSlot(options, offset);
        int next = current < 0 ? (includeNone ? 1 : 0) : (current + (reverse ? -1 : 1) + size) % size;
        if (includeNone && next == 0) return disable(player, trackType);
        return apply(player, trackType, options.get(next - offset));
    }

    static boolean select(Player player, int trackType, String trackId) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return false;
        for (Option option : collect(player.getCurrentTracks(), trackType, null)) {
            if (option.id.equals(trackId)) return apply(player, trackType, option);
        }
        return false;
    }

    static boolean disable(Player player, int trackType) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return false;
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
        builder.clearOverridesOfType(trackType);
        builder.setTrackTypeDisabled(trackType, true);
        player.setTrackSelectionParameters(builder.build());
        return true;
    }

    static boolean reset(Player player, int trackType) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return false;
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
        builder.clearOverridesOfType(trackType);
        builder.setTrackTypeDisabled(trackType, false);
        player.setTrackSelectionParameters(builder.build());
        return true;
    }

    private static boolean apply(Player player, int trackType, Option option) {
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
        builder.clearOverridesOfType(trackType);
        builder.setTrackTypeDisabled(trackType, false);
        builder.addOverride(new TrackSelectionOverride(option.group.getMediaTrackGroup(), option.index));
        player.setTrackSelectionParameters(builder.build());
        return true;
    }

    private static int selectedSlot(List<Option> options, int offset) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).group.isTrackSelected(options.get(i).index)) return i + offset;
        }
        return -1;
    }

    private static List<Option> collect(Tracks tracks, int trackType, List<String> trackIds) {
        List<Option> options = new ArrayList<>();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != trackType || !group.isSupported()) continue;
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSupported(i)) continue;
                Format format = group.getTrackFormat(i);
                if (format.id == null) continue;
                if (trackIds != null && !trackIds.contains(format.id)) continue;
                options.add(new Option(group, i, format.id));
            }
        }
        if (trackIds != null) options.sort(Comparator.comparingInt(option -> trackIds.indexOf(option.id)));
        return options;
    }

    private static final class Option {

        private final TrackGroup group;
        private final int index;
        private final String id;

        private Option(Tracks.Group group, int index, String id) {
            this.group = group.getMediaTrackGroup();
            this.index = index;
            this.id = id;
        }
    }
}
