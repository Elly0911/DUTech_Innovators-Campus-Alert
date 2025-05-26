package com.example.pbdv_project;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class SafetyTipsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_tips);

        RecyclerView recyclerView = findViewById(R.id.safety_tips_recycler);

        int spanCount = 1;
        if (getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 2;
        }

        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        List<SafetyTip> safetyTips = getSafetyTips();
        SafetyTipsAdapter adapter = new SafetyTipsAdapter(safetyTips);
        recyclerView.setAdapter(adapter);
    }

    private List<SafetyTip> getSafetyTips() {
        List<SafetyTip> tips = new ArrayList<>();

        tips.add(new SafetyTip(
                "🔥 Fire",
                "1. Stay calm and do not use the elevator.\n" +
                        "2. Exit the building using the nearest stairway.\n" +
                        "3. Alert others around you while exiting.\n" +
                        "4. Proceed to the designated assembly point.\n" +
                        "5. Do not re-enter until officials say it's safe."
        ));

        tips.add(new SafetyTip(
                "🚨 Lockdown (e.g. intruder)",
                "1. Find the nearest room and lock/barricade the door.\n" +
                        "2. Turn off lights and silence your phone.\n" +
                        "3. Stay out of sight and quiet.\n" +
                        "4. Do not open the door for anyone.\n" +
                        "5. Wait for official \"All Clear\" message."
        ));

        tips.add(new SafetyTip(
                "💣 Bomb Threat",
                "1. Do not use cellphones or radios near the area.\n" +
                        "2. If you received the threat, write down exactly what was said.\n" +
                        "3. Evacuate calmly via staircases.\n" +
                        "4. Leave bags and personal items behind.\n" +
                        "5. Go to the furthest open assembly point."
        ));

        tips.add(new SafetyTip(
                "🚑 Medical Emergency",
                "1. Call campus emergency line immediately.\n" +
                        "2. Don't move the injured person unless necessary.\n" +
                        "3. If trained, begin CPR or first aid.\n" +
                        "4. Stay with the person until help arrives.\n" +
                        "5. Protection Services: 031 3732181/2182"
        ));

        tips.add(new SafetyTip(
                "⚡ Electrical Fault",
                "1. If safe, unplug devices to prevent damage.\n" +
                        "2. Use emergency lights or phone flashlight.\n" +
                        "3. Avoid wet areas if there's sparking.\n" +
                        "4. Do not use elevators."
        ));

        tips.add(new SafetyTip(
                "🌩️ Severe Weather",
                "1. Seek shelter indoors immediately.\n" +
                        "2. Stay away from windows and doors.\n" +
                        "3. Avoid flooded areas or fallen wires.\n" +
                        "4. Follow official instructions."
        ));

        tips.add(new SafetyTip(
                "🕵️ Suspicious Activity",
                "1. Do not approach or touch the item/person.\n" +
                        "2. Move away and alert campus security.\n" +
                        "3. Provide details (location, appearance).\n" +
                        "4. Be prepared to evacuate or lock down."
        ));

        tips.add(new SafetyTip(
                "📞 Emergency Contacts",
                "Protection Services 24-hour Emergency Numbers:\n" +
                        "031 3732181\n" +
                        "031 3732182\n\n" +
                        "Save these numbers to your contacts for quick access in emergencies."
        ));

        return tips;
    }
}