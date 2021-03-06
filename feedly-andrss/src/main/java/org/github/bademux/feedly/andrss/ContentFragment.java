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

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import org.github.bademux.feedly.andrss.helpers.FeedlyContentAdapter;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link ContentFragment.OnFragmentInteractionListener}
 * interface.
 */
public class ContentFragment extends ListFragment implements OnRefreshListener {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MainActivity activity = (MainActivity) getActivity();
    mAdapter = new FeedlyContentAdapter(activity, activity.getAsynchQueryHandler());
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    getListView().addFooterView(createLoadMoreButton(view.getContext()));
    
    setListAdapter(mAdapter);

    // We need to create a PullToRefreshLayout manually
    mPullToRefreshLayout = new PullToRefreshLayout(view.getContext());

    ActionBarPullToRefresh.from(getActivity()).insertLayoutInto((ViewGroup) view)
                          .theseChildrenArePullable(getListView(), getListView().getEmptyView())
                          .listener(this).setup(mPullToRefreshLayout);
  }

  public final Button createLoadMoreButton(final Context context) {
    Button btnLoadMore = new Button(context);
    btnLoadMore.setText(R.string.button_loadmore_text);
    btnLoadMore.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) { mListener.onLoadMore(); }
    });
    return btnLoadMore;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mListener = (OnFragmentInteractionListener) activity;
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);

    if (null != mListener) {
      // Notify the active callbacks interface (the activity, if the
      // fragment is attached to one) that an item has been selected.
      //mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
    }
  }

  @Override
  public void onRefreshStarted(final View view) {
    mListener.onRefreshList();
    mPullToRefreshLayout.setRefreshComplete();
  }

  public void startQueryOnCategory(String groupUrl) { mAdapter.startQueryOnCategory(groupUrl); }

  public void startQueryOnFeed(String childUrl) { mAdapter.startQueryOnFeed(childUrl); }

  public ContentFragment() {}

  private OnFragmentInteractionListener mListener;

  private PullToRefreshLayout mPullToRefreshLayout;

  private FeedlyContentAdapter mAdapter;

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {

    // TODO: Update argument type and TBL_NAME
    public void onFragmentInteraction(String id);

    public void onRefreshList();

    public void onLoadMore();
  }
}