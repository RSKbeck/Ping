package edu.calpoly.xyanrkeh.ping;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

/**
 * Created by xiang on 11/16/2016.
 */

public class ListFragment extends Fragment {

    private static final String TAG = "LISTLOG";

    private DatabaseReference mDatabase;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_list_fragment_view, container, false);

        final View recyclerView = view.findViewById(R.id.recycler_view);
        assert recyclerView != null;
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(container.getContext());
        ((RecyclerView) recyclerView).setLayoutManager(mLayoutManager);
        setupRecyclerView((RecyclerView) recyclerView);

        mDatabase = FirebaseDatabase.getInstance().getReference("Events");

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                EventList.update(dataSnapshot);
                ((RecyclerView) recyclerView).getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        return view;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter());
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        public SimpleItemRecyclerViewAdapter() {
            super();
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.bind(EventList.events.get(EventList.events.keyAt(position)));

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /**
                     if (mTwoPane) {
                     Bundle arguments = new Bundle();
                     arguments.putString(EventDetailFragment.ARG_ITEM_ID, holder.mEvent.id.toString());
                     EventDetailFragment fragment = new EventDetailFragment();
                     fragment.setArguments(arguments);
                     getSupportFragmentManager().beginTransaction()
                     .replace(R.id.Event_detail_container, fragment)
                     .commit();
                     } else {**/
                    Context context = v.getContext();
                    Intent intent = new Intent(context, DetailActivity.class);
                    intent.putExtra(EventDetail.ARG_ITEM_ID, holder.mEvent.getID());

                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return EventList.events.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mEventView;
            public final TextView mDescView;
            public final TextView mTimeView;
            public Event mEvent;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mDescView = (TextView) view.findViewById(R.id.evt_desc);
                mEventView = (TextView) view.findViewById(R.id.evt_title);
                mTimeView = (TextView) view.findViewById(R.id.evt_time);
            }

            public void bind(Event evt) {
                mEvent = evt;
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(evt.getTime());
                mTimeView.setText(cal.getTime().toString());
                mEventView.setText(evt.getTitle());
                mDescView.setText(evt.getDetails());
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mEventView.getText() + "'";
            }
        }
    }
}