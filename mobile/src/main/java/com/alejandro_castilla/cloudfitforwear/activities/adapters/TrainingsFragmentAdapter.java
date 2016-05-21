package com.alejandro_castilla.cloudfitforwear.activities.adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alejandro_castilla.cloudfitforwear.R;
import com.alejandro_castilla.cloudfitforwear.activities.TrainingDetailsActivity;
import com.alejandro_castilla.cloudfitforwear.cloudfit.models.CalendarEvent;
import com.alejandro_castilla.cloudfitforwear.utilities.StaticVariables;
import com.blunderer.materialdesignlibrary.views.CardView;

import java.util.ArrayList;

/**
 * Created by alejandrocq on 19/05/16.
 */
public class TrainingsFragmentAdapter extends
        RecyclerView.Adapter<TrainingsFragmentAdapter.ViewHolder> {

    private final String TAG = TrainingsFragmentAdapter.class.getSimpleName();

    private Activity context;
    private ArrayList<CalendarEvent> calendarEvents;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;

        public ViewHolder (View view) {
            super(view);
            this.cardView = (CardView) view.findViewById(R.id.cardView);
        }
    }

    public TrainingsFragmentAdapter(Activity context, ArrayList<CalendarEvent> calendarEvents) {
        this.context = context;
        this.calendarEvents = calendarEvents;
    }

    @Override
    public TrainingsFragmentAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.trainings_cardview, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (calendarEvents != null && calendarEvents.size()>0) {
            holder.cardView.setTitle(calendarEvents.get(position).getText());
            String trainingDescription = "Fecha: " + calendarEvents.get(position).getDate() + "\n"
                    + "Tipo de entrenamiento: " + calendarEvents.get(position).getType();
            holder.cardView.setDescription(trainingDescription);
            holder.cardView.setOnNormalButtonClickListener(new
                    ButtonClickListener(StaticVariables.NORMAL_BUTTON,position));
            holder.cardView.setOnHighlightButtonClickListener(new
                    ButtonClickListener(StaticVariables.HIGHLIGHT_BUTTON, position));
        }
    }

    @Override
    public int getItemCount() {
//        if (calendarEvents != null) {
//            return calendarEvents.size();
//        } else {
//            return 0;
//        }
        return calendarEvents.size();
    }

    public void setCalendarEvents(ArrayList<CalendarEvent> calendarEvents) {
        Log.d(TAG, "Data updated.");
        this.calendarEvents = calendarEvents;
    }

    /**
     * Listener for CardView's buttons.
     */

    private class ButtonClickListener implements View.OnClickListener {

        private int position;
        private short buttonType;

        public ButtonClickListener(short buttonType ,int position) {
            this.buttonType = buttonType;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
//            Toast.makeText(v.getContext(), "Botón detalles pulsado para: "
//                    + calendarEvents.get(position).getText(), Toast.LENGTH_LONG).show();
            switch (buttonType) {
                case StaticVariables.NORMAL_BUTTON:
                    Intent startTrainingDetailsIntent = new Intent(context,
                            TrainingDetailsActivity.class);
                    startTrainingDetailsIntent.putExtra("trainingid",
                            calendarEvents.get(position).getId());
                    context.startActivity(startTrainingDetailsIntent);
                    break;
                case StaticVariables.HIGHLIGHT_BUTTON:

                    break;
            }
        }
    }

}