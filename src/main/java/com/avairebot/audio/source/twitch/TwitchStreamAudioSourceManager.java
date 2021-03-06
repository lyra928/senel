package com.avairebot.audio.source.twitch;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * This is a copy of {@link com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager}, the API endpoint used for
 * looking up channels has been changed as a temporary fix until a permanent fix gets added to the official LavaPlayer package.
 */
public class TwitchStreamAudioSourceManager implements AudioSourceManager, HttpConfigurable {

    private static final String clientId = "jzkbprff40iqj646a697cyrvl0zt2m6";
    private static final String streamNameRegex = "^https://(?:www\\.|go\\.)?twitch.tv/([^/]+)$";
    private static final Pattern streamNamePattern = Pattern.compile(streamNameRegex);
    private final HttpInterfaceManager httpInterfaceManager;

    /**
     * Create an instance.
     */
    public TwitchStreamAudioSourceManager() {
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    /**
     * Extract channel identifier from a channel URL.
     *
     * @param url Channel URL
     * @return Channel identifier (for API requests)
     */
    public static String getChannelIdentifierFromUrl(String url) {
        Matcher matcher = streamNamePattern.matcher(url);
        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(1);
    }

    /**
     * @param url Request URL
     * @return Request with necessary headers attached.
     */
    public static HttpUriRequest createGetRequest(String url) {
        return addClientHeaders(new HttpGet(url));
    }

    /**
     * @param url Request URL
     * @return Request with necessary headers attached.
     */
    public static HttpUriRequest createGetRequest(URI url) {
        return addClientHeaders(new HttpGet(url));
    }

    private static HttpUriRequest addClientHeaders(HttpUriRequest request) {
        request.setHeader("Client-ID", clientId);
        return request;
    }

    @Override
    public String getSourceName() {
        return "twitch";
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        String streamName = getChannelIdentifierFromUrl(reference.identifier);
        if (streamName == null) {
            return null;
        }

        JsonBrowser channelInfo = fetchStreamChannelInfo(streamName);

        if (channelInfo == null) {
            return AudioReference.NO_TRACK;
        } else {
            final String displayName = channelInfo.get("display_name").text();
            final String status = channelInfo.get("status").text();

            return new TwitchStreamAudioTrack(new AudioTrackInfo(
                status,
                displayName,
                Long.MAX_VALUE,
                reference.identifier,
                true,
                reference.identifier
            ), this);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // Nothing special to do, URL (identifier) is enough
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new TwitchStreamAudioTrack(trackInfo, this);
    }

    /**
     * @return Get an HTTP interface for a playing track.
     */
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    private JsonBrowser fetchStreamChannelInfo(String name) {
        try (HttpInterface httpInterface = getHttpInterface()) {
            HttpUriRequest request = createGetRequest("https://api.twitch.tv/helix/streams?user_login=" + name);

            return HttpClientTools.fetchResponseAsJson(httpInterface, request);
        } catch (IOException e) {
            throw new FriendlyException("Loading Twitch channel information failed.", SUSPICIOUS, e);
        }
    }

    @Override
    public void shutdown() {
        // Nothing to shut down
    }
}
