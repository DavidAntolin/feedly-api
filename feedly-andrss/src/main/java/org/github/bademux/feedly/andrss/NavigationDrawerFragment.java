/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Contributors:
 *                Bademus
 */

package org.github.bademux.feedly.andrss;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorTreeAdapter;

import org.github.bademux.feedly.api.util.db.QueryHandler;

import static org.github.bademux.feedly.api.provider.FeedlyContract.Categories;
import static org.github.bademux.feedly.api.provider.FeedlyContract.Feeds;
import static org.github.bademux.feedly.api.provider.FeedlyContract.FeedsByCategory;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment implements
                                                       QueryHandler.AsyncQueryListener {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Read in the flag indicating whether or not the user has demonstrated awareness of the
    // drawer. See PREF_USER_LEARNED_DRAWER for details.
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

    if (savedInstanceState != null) {
      mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
      mFromSavedInstanceState = true;
    }

    // Select either the default item (0) or the last selected item.
    selectItem(mCurrentSelectedPosition);

    mAdapter = createAdapter();

    mQueryHandler = new QueryHandler(getActivity().getContentResolver(), this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // Null out the group cursor.
    // This will cause the group cursor and all of the child cursors to be closed.
    mAdapter.changeCursor(null);
    mAdapter = null;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // Indicate that this fragment would like to influence the set of actions in the action bar.
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    LinearLayout linearLayout = (LinearLayout) inflater.inflate(
        R.layout.fragment_navigation_drawer, container, false);

    Button refreshButton = (Button) linearLayout.findViewById(R.id.button_refresh);
    refreshButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) { mListener.onRefresh(); }
    });

    mDrawerListView = (ExpandableListView) linearLayout.findViewById(R.id.list_menu);

    mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectItem(position);
      }
    });
    mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
    mDrawerListView.setAdapter(mAdapter);

    mQueryHandler.startQuery(TOKEN_GROUP, null, Categories.CONTENT_URI,
                             new String[]{Categories.LABEL, Categories.ID}, null, null, null);

    return linearLayout;
  }

  public boolean isDrawerOpen() {
    return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
  }

  /** ...grandeur and poverty of Java... */
  private CursorTreeAdapter createAdapter() {
    // The constructor does not take a Cursor - avoiding querying the db on the main thread.
    return new SimpleCursorTreeAdapter(getActivity(), null,
                                       android.R.layout.simple_expandable_list_item_1,
                                       new String[]{Categories.LABEL, Categories.ID},
                                       new int[]{android.R.id.text1},
                                       android.R.layout.simple_expandable_list_item_1,
                                       new String[]{Feeds.TITLE},
                                       new int[]{android.R.id.text1}) {
      @Override
      protected Cursor getChildrenCursor(final Cursor groupCursor) {
        String id = groupCursor.getString(groupCursor.getColumnIndex(Categories.ID));
        Uri.Builder builder = FeedsByCategory.CONTENT_URI.buildUpon().appendPath(id);
        mQueryHandler.startQuery(TOKEN_CHILD, groupCursor.getPosition(), builder.build(),
                                 new String[]{Feeds.TITLE}, null, null, null);
        return null;
      }
    };
  }

  /**
   * Users of this fragment must call this method to set up the navigation drawer interactions.
   *
   * @param fragmentId   The android:id of this fragment in its activity's layout.
   * @param drawerLayout The DrawerLayout containing this fragment's UI.
   */
  public void setUp(int fragmentId, DrawerLayout drawerLayout) {
    mFragmentContainerView = getActivity().findViewById(fragmentId);
    mDrawerLayout = drawerLayout;

    // set a custom shadow that overlays the main content when the drawer opens
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    // set up the drawer's list view with items and click listener

    ActionBar actionBar = getActivity().getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the navigation drawer and the action bar app icon.
    mDrawerToggle = new ActionBarDrawerToggle(
        getActivity(),                    /* host Activity */
        mDrawerLayout,                    /* DrawerLayout object */
        R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
        R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
        R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
    ) {
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        if (!isAdded()) {
          return;
        }

        getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        if (!isAdded()) {
          return;
        }

        if (!mUserLearnedDrawer) {
          // The user manually opened the drawer; store this flag to prevent auto-showing
          // the navigation drawer automatically in the future.
          mUserLearnedDrawer = true;
          SharedPreferences sp = PreferenceManager
              .getDefaultSharedPreferences(getActivity());
          sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
        }

        getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
      }
    };

    // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
    // per the navigation drawer design guidelines.
    if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
      mDrawerLayout.openDrawer(mFragmentContainerView);
    }

    // Defer code dependent on restoration of previous instance state.
    mDrawerLayout.post(new Runnable() {
      @Override
      public void run() {
        mDrawerToggle.syncState();
      }
    });

    mDrawerLayout.setDrawerListener(mDrawerToggle);
  }

  private void selectItem(int position) {
    mCurrentSelectedPosition = position;
    if (mDrawerListView != null) {
      mDrawerListView.setItemChecked(position, true);
    }
    if (mDrawerLayout != null) {
      mDrawerLayout.closeDrawer(mFragmentContainerView);
    }
    if (mListener != null) {
      mListener.onNavigationDrawerItemSelected(position);
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      mListener = (OnFragmentInteractionListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException("Activity must implement OnFragmentInteractionListener.");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Forward the new configuration the drawer toggle component.
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // If the drawer is open, show the global app actions in the action bar. See also
    // showGlobalContextActionBar, which controls the top-left area of the action bar.
    if (mDrawerLayout != null && isDrawerOpen()) {
      inflater.inflate(R.menu.global, menu);
      showGlobalContextActionBar();
    }
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
    switch (token) {
      case TOKEN_GROUP:
        mAdapter.setGroupCursor(cursor);
        break;
      case TOKEN_CHILD:
        mAdapter.setChildrenCursor((Integer) cookie, cursor);
        break;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    switch (item.getItemId()) {
      case R.id.action_example:
        mListener.onRefresh();
        return true;
      case R.id.action_auth:
        mListener.onLogin();
        return true;
      case R.id.action_deauth:
        mListener.onLogout();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * Per the navigation drawer design guidelines, updates the action bar to show the global app
   * 'context', rather than just what's in the current screen.
   */
  private void showGlobalContextActionBar() {
    ActionBar actionBar = getActivity().getActionBar();
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setTitle(R.string.app_name);
  }

  public NavigationDrawerFragment() {}

  public static final int TOKEN_GROUP = 0, TOKEN_CHILD = 1;

  /**
   * Remember the position of the selected item.
   */
  private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

  /**
   * Per the design guidelines, you should show the drawer on launch until the user manually
   * expands it. This shared preference tracks this.
   */
  private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

  /**
   * A pointer to the current callbacks instance (the Activity).
   */
  private OnFragmentInteractionListener mListener;

  /**
   * Helper component that ties the action bar to the navigation drawer.
   */
  private ActionBarDrawerToggle mDrawerToggle;

  private DrawerLayout mDrawerLayout;
  private ExpandableListView mDrawerListView;
  private View mFragmentContainerView;

  private int mCurrentSelectedPosition = 0;
  private boolean mFromSavedInstanceState;
  private boolean mUserLearnedDrawer;

  private CursorTreeAdapter mAdapter;
  private QueryHandler mQueryHandler;


  /**
   * Callbacks interface that all activities using this fragment must implement.
   */
  public static interface OnFragmentInteractionListener {

    /**
     * Called when an item in the navigation drawer is selected.
     */
    void onNavigationDrawerItemSelected(int position);

    boolean isAuthenticated();

    void onLogin();

    void onLogout();

    void onRefresh();
  }
}
