package com.chickenkiller.upods2.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.chickenkiller.upods2.R;
import com.chickenkiller.upods2.controllers.app.ProfileManager;
import com.chickenkiller.upods2.controllers.player.UniversalPlayer;
import com.chickenkiller.upods2.dialogs.DialogFragmentTrackInfo;
import com.chickenkiller.upods2.fragments.FragmentMainFeatured;
import com.chickenkiller.upods2.fragments.FragmentPlayer;
import com.chickenkiller.upods2.fragments.FragmentSearch;
import com.chickenkiller.upods2.fragments.FragmentVideoPlayer;
import com.chickenkiller.upods2.interfaces.IToolbarHolder;
import com.chickenkiller.upods2.models.MediaItem;
import com.chickenkiller.upods2.models.Podcast;
import com.chickenkiller.upods2.models.RadioItem;
import com.chickenkiller.upods2.utils.Analytics;
import com.chickenkiller.upods2.utils.ContextMenuHelper;
import com.chickenkiller.upods2.utils.MediaUtils;
import com.chickenkiller.upods2.utils.enums.ContextMenuType;
import com.chickenkiller.upods2.utils.enums.MediaItemType;
import com.yandex.metrica.YandexMetrica;

public class ActivityPlayer extends BasicActivity implements IToolbarHolder, Toolbar.OnMenuItemClickListener {

    public static final String ACTIVITY_STARTED_FROM = "startedFrom";
    public static final String ACTIVITY_STARTED_FROM_IN_DEPTH = "startedFromDepth"; //used if activity was started from depth fragment i.e search

    private MediaItem currentMediaItem;
    private Toolbar toolbar;
    private ActionMenuItemView itemFavorites;
    private FragmentPlayer fragmentPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        toolbar = (Toolbar) findViewById(R.id.toolbar_player);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.inflateMenu(R.menu.menu_activity_player);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setVisibility(View.VISIBLE);
        itemFavorites = (ActionMenuItemView) toolbar.findViewById(R.id.action_favorites_player);

        currentMediaItem = UniversalPlayer.getInstance().getPlayingMediaItem();
        if (currentMediaItem == null) {
            Toast.makeText(this, getString(R.string.error_cant_play), Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        if (getFragmentManager().getBackStackEntryCount() == 0) {
            if (currentMediaItem instanceof Podcast && MediaUtils.isVideoUrl(currentMediaItem.getAudeoLink())) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                FragmentVideoPlayer fragmentVideoPlayer = new FragmentVideoPlayer();
                fragmentVideoPlayer.setOnPlayingFailedCallback(MediaUtils.getPlayerFailCallback(this, currentMediaItem));
                showFragment(R.id.fl_window, fragmentVideoPlayer, FragmentVideoPlayer.TAG);
            } else {
                fragmentPlayer = new FragmentPlayer();
                fragmentPlayer.setPlayableItem(currentMediaItem);
                showFragment(R.id.fl_window, fragmentPlayer, FragmentMainFeatured.TAG);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1 || getLatestFragmentTag().equals(FragmentPlayer.TAG)) {
            getFragmentManager().popBackStack();
        } else {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Intent myIntent = new Intent(this, ActivityMain.class);
            if (getIntent().hasExtra(ACTIVITY_STARTED_FROM)) {
                myIntent.putExtra(ACTIVITY_STARTED_FROM, getIntent().getIntExtra(ACTIVITY_STARTED_FROM, -1));
                myIntent.putExtra(ACTIVITY_STARTED_FROM_IN_DEPTH, getIntent().getIntExtra(ACTIVITY_STARTED_FROM_IN_DEPTH, -1));
            }
            startActivity(myIntent);
            overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_top);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        if (contextMenuType == ContextMenuType.PLAYER_SETTINGS) {
            inflater.inflate(R.menu.menu_basic_sceleton, menu);
            if (currentMediaItem instanceof RadioItem) {
                menu.add(getString(R.string.select_stream_quality));
                menu.add(getString(R.string.stream_info));
            } else {
                menu.add(getString(R.string.show_notes));
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        MediaItem mediaItem = UniversalPlayer.getInstance().getPlayingMediaItem();
        if (item.getItemId() == R.id.action_favorites_player) {
            if (mediaItem.isSubscribed) {
                itemFavorites.setIcon(getResources().getDrawable(R.drawable.ic_heart_white_24dp));
                ProfileManager.getInstance().removeSubscribedMediaItem(mediaItem);
                Toast.makeText(this, getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show();
                YandexMetrica.reportEvent(Analytics.PLAYER_UNSUBSCRIBE);
            } else {
                itemFavorites.setIcon(getResources().getDrawable(R.drawable.ic_heart_black_24dp));
                ProfileManager.getInstance().addSubscribedMediaItem(mediaItem);
                Toast.makeText(this, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show();
                YandexMetrica.reportEvent(Analytics.PLAYER_SUBSCRIBE);
            }
        } else if (item.getItemId() == R.id.action_player_settings) {
            openContextMenu(findViewById(R.id.action_player_settings), ContextMenuType.PLAYER_SETTINGS, null, null);
        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(getString(R.string.select_stream_quality))) {
            YandexMetrica.reportEvent(Analytics.PLAYER_STREAM_QUALITY);
            ContextMenuHelper.selectRadioStreamQuality(this, fragmentPlayer, (RadioItem) currentMediaItem);
        } else if (item.getTitle().equals("Show notes")) {
            DialogFragmentTrackInfo dialogFragmentTrackInfo = new DialogFragmentTrackInfo();
            dialogFragmentTrackInfo.setTrack(((Podcast) UniversalPlayer.getInstance().getPlayingMediaItem()).getSelectedTrack());
            dialogFragmentTrackInfo.enableStream = false;
            showDialogFragment(dialogFragmentTrackInfo);
        } else if (item.getTitle().equals(getString(R.string.stream_info))) {
            ContextMenuHelper.showStreamInfoDialog(this);
            YandexMetrica.reportEvent(Analytics.PLAYER_STREAM_INFO);
        }
        return super.onContextItemSelected(item);
    }

    public static void openWithIntent(Activity activity) {
        //Detect if starting from search -> if yes from which type
        if (FragmentSearch.isActive) {
            if (ActivityMain.lastFragmentType == MediaItemType.PODCAST.ordinal()) {
                ActivityMain.lastChildFragmentType = MediaItemType.PODCAST_SEARCH.ordinal();
            } else if (ActivityMain.lastFragmentType == MediaItemType.RADIO.ordinal()) {
                ActivityMain.lastChildFragmentType = MediaItemType.RADIO_SEARCH.ordinal();
            }
        }

        Intent myIntent = new Intent(activity, ActivityPlayer.class);
        myIntent.putExtra(ActivityPlayer.ACTIVITY_STARTED_FROM, ActivityMain.lastFragmentType);
        myIntent.putExtra(ActivityPlayer.ACTIVITY_STARTED_FROM_IN_DEPTH, ActivityMain.lastChildFragmentType);
        activity.startActivity(myIntent);
        activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
        activity.finish();
        ActivityMain.lastChildFragmentType = -1;
        ActivityMain.lastChildFragmentType = -1;
    }
}
