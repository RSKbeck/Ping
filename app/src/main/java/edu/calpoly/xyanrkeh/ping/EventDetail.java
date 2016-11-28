package edu.calpoly.xyanrkeh.ping;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;

public class EventDetail extends Fragment {

    public static final String ARG_ITEM_ID = "eventid";

    private Event mEvent;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mEvent = EventList.events.get(getArguments().getString(ARG_ITEM_ID));
            mAuth = FirebaseAuth.getInstance();
            Activity activity = this.getActivity();

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_event_detail, container, false);
        final EditText title = (EditText) rootView.findViewById(R.id.evt_det_title);
        final EditText descrip = (EditText) rootView.findViewById(R.id.evt_det_desc);
        final EditText latt = (EditText) rootView.findViewById(R.id.evt_det_lat);
        final EditText longg = (EditText) rootView.findViewById(R.id.evt_det_long);
        // Show the dummy content as text in a TextView.
        if (mEvent != null) {
            title.setText(mEvent.getTitle());
            ((EditText) rootView.findViewById(R.id.evt_det_crtr)).setText(mEvent.getCreator());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(mEvent.getTime());
            ((EditText) rootView.findViewById(R.id.evt_det_time)).setText(cal.getTime().toString());
            latt.setText(mEvent.getLatitude() + "");
            longg.setText(mEvent.getLongitude() + "");
            descrip.setText(mEvent.getDetails());


            if (mEvent.getCreator().equals(mAuth.getCurrentUser().getEmail())) {
                Button save = (Button) rootView.findViewById(R.id.save_button);

                save.setVisibility(View.VISIBLE);
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EventList.delete(mEvent.getID());
                        mEvent.setTitle(title.getText().toString());
                        mEvent.setDetails(descrip.getText().toString());
                        mEvent.setLocation(Double.valueOf(latt.getText().toString()), Double.valueOf(longg.getText().toString()));
                        EventList.events.put(mEvent.getID(), mEvent);
                        EventList.push(mEvent.getID());
                    }
                });
                title.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                latt.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                longg.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                descrip.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
            } else {
                title.setFocusable(false);
                descrip.setFocusable(false);
                latt.setFocusable(false);
                longg.setFocusable(false);
            }
        } else {
            title.setFocusable(false);
            descrip.setFocusable(false);
            latt.setFocusable(false);
            longg.setFocusable(false);
        }
        ((EditText) rootView.findViewById(R.id.evt_det_time)).setFocusable(false);
        ((EditText) rootView.findViewById(R.id.evt_det_crtr)).setFocusable(false);


        return rootView;
    }


}
