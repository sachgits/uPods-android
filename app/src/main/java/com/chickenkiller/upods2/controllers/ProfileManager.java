package com.chickenkiller.upods2.controllers;

import android.util.Log;

import com.chickenkiller.upods2.models.Episod;
import com.chickenkiller.upods2.models.Podcast;
import com.pixplicity.easyprefs.library.Prefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by alonzilberman on 9/28/15.
 */
public class ProfileManager {

    public enum ProfileItem {DOWNLOADED_PODCASTS, SUBSCRIBDED_PODCASTS, SUBSCRIBED_STATIONS}

    private static String PROFILE_PREF = "profile_pref";
    private static String PROFILE = "PROFILE";
    public static ProfileManager profileManager;
    private ArrayList<Podcast> downloadedPodcasts;
    private ArrayList<Podcast> subscribedPodcasts;

    private ProfileManager() {
        this.downloadedPodcasts = new ArrayList<>();
        this.subscribedPodcasts = new ArrayList<>();
        String profileJsonStr = Prefs.getString(PROFILE_PREF, null);
        if (profileJsonStr == null) {
            JSONObject rootProfile = new JSONObject();
            JSONArray downloadedPodcasts = new JSONArray();
            JSONArray subscribedPodcasts = new JSONArray();
            JSONArray subscribedRadioStations = new JSONArray();
            try {
                rootProfile.put("downloadedPodcasts", downloadedPodcasts);
                rootProfile.put("subscribedPodcasts", subscribedPodcasts);
                rootProfile.put("subscribedStations", subscribedRadioStations);
            } catch (JSONException e) {
                Log.e(PROFILE, "Can't create empty profile object");
                e.printStackTrace();
            }
            profileJsonStr = rootProfile.toString();
            Prefs.putString(PROFILE_PREF, profileJsonStr);
        }
        try {
            JSONObject rootProfile = new JSONObject(profileJsonStr);
            initProfilePodcastItem(rootProfile.getJSONArray("downloadedPodcasts"), ProfileItem.DOWNLOADED_PODCASTS);
            initProfilePodcastItem(rootProfile.getJSONArray("subscribedPodcasts"), ProfileItem.SUBSCRIBDED_PODCASTS);
        } catch (JSONException e) {
            Log.e(PROFILE, "Can't parse profile string to json: " + profileJsonStr);
            e.printStackTrace();
        }
    }

    public static ProfileManager getInstance() {
        if (profileManager == null) {
            profileManager = new ProfileManager();
        }
        return profileManager;
    }

    private void initProfilePodcastItem(JSONArray podcasts, ProfileItem profileItem) {
        for (int i = 0; i < podcasts.length(); i++) {
            try {
                Podcast podcast = new Podcast(podcasts.getJSONObject(i));
                if (profileItem == ProfileItem.DOWNLOADED_PODCASTS) {
                    downloadedPodcasts.add(podcast);
                } else if (profileItem == ProfileItem.SUBSCRIBDED_PODCASTS) {
                    subscribedPodcasts.add(podcast);
                }
            } catch (JSONException e) {
                Log.e(PROFILE, "Can't fetch podcast from json object at index" + String.valueOf(i));
                e.printStackTrace();
            }
        }
        String podcastType = "";
        if (profileItem == ProfileItem.DOWNLOADED_PODCASTS) {
            podcastType=" downloaded ";
        } else if (profileItem == ProfileItem.SUBSCRIBDED_PODCASTS) {
            podcastType=" subscribded ";
        }
        Log.i(PROFILE, "Fetcheed " + String.valueOf(podcasts.length()) + podcastType + "podcasts from json profile");
    }

    public void addSubscribedPodcast(Podcast podcast) {
        if (!Podcast.hasPodcastWithName(subscribedPodcasts, podcast)) {
            subscribedPodcasts.add(podcast);
            saveChanges(ProfileItem.SUBSCRIBDED_PODCASTS);
        }

    }

    public void addDownloadedEpisod(Podcast tempPodcast, Episod episod) {
        if (!Podcast.hasPodcastWithName(downloadedPodcasts, tempPodcast)) {
            Podcast podcast = new Podcast(tempPodcast);
            podcast.getEpisods().clear();
            podcast.getEpisods().add(episod);
            downloadedPodcasts.add(podcast);
        } else {
            Podcast podcast = Podcast.getPodcastByName(downloadedPodcasts, tempPodcast);
            podcast.getEpisods().add(episod);
        }
        saveChanges(ProfileItem.DOWNLOADED_PODCASTS);
    }

    public void removeDownloadedEpisod(Podcast tempPodcast, Episod episod) {
        if (Podcast.hasPodcastWithName(downloadedPodcasts, tempPodcast)) {
            Podcast podcast = Podcast.getPodcastByName(downloadedPodcasts, tempPodcast);
            if (podcast.getEpisods().size() == 1) {
                downloadedPodcasts.remove(podcast);
            } else {
                podcast.getEpisods().remove(Episod.getEpisodByTitle(podcast.getEpisods(), episod));
            }
            saveChanges(ProfileItem.DOWNLOADED_PODCASTS);
        }
    }

    public boolean isDownloaded(Podcast tempPodcast, Episod episod) {
        if (Podcast.hasPodcastWithName(downloadedPodcasts, tempPodcast)) {
            Podcast podcast = Podcast.getPodcastByName(downloadedPodcasts, tempPodcast);
            return Episod.hasEpisodWithTitle(podcast.getEpisods(), episod);
        }
        return false;
    }

    public boolean isSubscribedToPodcast(Podcast podcast) {
        return Podcast.hasPodcastWithName(subscribedPodcasts, podcast);
    }

    public void removeSubscribedPodcasts(Podcast podcast) {
        if (Podcast.hasPodcastWithName(subscribedPodcasts, podcast)) {
            subscribedPodcasts.remove(podcast);
            saveChanges(ProfileItem.SUBSCRIBDED_PODCASTS);
        }
    }

    private void saveChanges(ProfileItem profileItem) {
        String profileJsonStr = Prefs.getString(PROFILE_PREF, null);
        if (profileJsonStr != null) {
            try {
                JSONObject rootProfile = new JSONObject(profileJsonStr);
                if (profileItem == ProfileItem.DOWNLOADED_PODCASTS) {
                    rootProfile.put("downloadedPodcasts", Podcast.toJsonArray(downloadedPodcasts, true));
                } else if (profileItem == ProfileItem.SUBSCRIBDED_PODCASTS) {
                    rootProfile.put("subscribedPodcasts", Podcast.toJsonArray(subscribedPodcasts, false));
                }
                Prefs.putString(PROFILE_PREF, rootProfile.toString());
                Log.i(PROFILE_PREF, rootProfile.toString());
            } catch (JSONException e) {
                Log.e(PROFILE, "Can't parse profile string to json: " + profileJsonStr);
                e.printStackTrace();
            }
        }
    }
}
