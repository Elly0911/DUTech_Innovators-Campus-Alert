package com.example.pbdv_project;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ResolvedReportsActivity extends AppCompatActivity {

    private static final String TAG = "ResolvedReportsActivity";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9\\s-]{10,20}$");
    private RecyclerView reportsRecyclerView;
    private ResolvedReportAdapter adapter;
    private FirebaseFirestore fStore;
    private TextView noReportsText;
    private ProgressBar progressBar;
    private String currentSearchQuery = "";
    private TextInputEditText searchInput;
    private TextInputLayout searchContainer;
    private String currentFilterField = null;
    private String currentFilterValue = null;
    private String currentFilterEndValue = null;
    private String currentFilterStartTime = null;
    private String currentFilterEndTime = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private SimpleDateFormat datePickerFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private SimpleDateFormat timePickerFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String currentFilterLabel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolved_reports);

        fStore = FirebaseFirestore.getInstance();
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        noReportsText = findViewById(R.id.noReportsText);
        progressBar = findViewById(R.id.progressBar);
        searchInput = findViewById(R.id.searchInput);
        searchContainer = findViewById(R.id.searchContainer);

        // Setup search functionality
        setupSearch();

        View filterIcon = findViewById(R.id.filterIcon);
        filterIcon.setOnClickListener(v -> showFilterDialog());

        setupRecyclerView();
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        dialog.setContentView(view);

        // Initialize all views
        TextInputLayout fieldInputContainer = view.findViewById(R.id.fieldInputContainer);
        TextInputLayout regularValueInputContainer = view.findViewById(R.id.regularValueInputContainer);
        TextInputLayout dateValueInputContainer = view.findViewById(R.id.dateValueInputContainer);
        TextInputLayout startTimeInputContainer = view.findViewById(R.id.startTimeInputContainer);
        TextInputLayout endDateInputContainer = view.findViewById(R.id.endDateInputContainer);
        TextInputLayout endTimeInputContainer = view.findViewById(R.id.endTimeInputContainer);

        AutoCompleteTextView fieldSpinner = view.findViewById(R.id.fieldSpinner);
        TextInputEditText regularValueInput = view.findViewById(R.id.regularValueInput);
        TextInputEditText dateValueInput = view.findViewById(R.id.dateValueInput);
        TextInputEditText startTimeInput = view.findViewById(R.id.startTimeInput);
        TextInputEditText endDateInput = view.findViewById(R.id.endDateInput);
        TextInputEditText endTimeInput = view.findViewById(R.id.endTimeInput);

        MaterialButton clearButton = view.findViewById(R.id.clearButton);
        MaterialButton applyButton = view.findViewById(R.id.applyButton);
        TextView activeFilterInfo = view.findViewById(R.id.activeFilterInfo);

        if (currentFilterLabel != null) {
            activeFilterInfo.setText(currentFilterLabel);
            activeFilterInfo.setVisibility(View.VISIBLE);
        } else {
            activeFilterInfo.setVisibility(View.GONE);
        }

        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_menu_item,
                new ArrayList<>(Arrays.asList(
                        "Report Type",
                        "Description",
                        "Location",
                        "Reporter Name",
                        "Reporter Email",
                        "Reporter Phone",
                        "Reported Date",
                        "Resolved Date",
                        "Resolved By"
                ))
        );
        fieldSpinner.setAdapter(fieldAdapter);

        fieldSpinner.setOnItemClickListener((parent, view1, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            fieldInputContainer.setHint("Filter by " + selected.toLowerCase());

            regularValueInputContainer.setVisibility(View.GONE);
            dateValueInputContainer.setVisibility(View.GONE);
            startTimeInputContainer.setVisibility(View.GONE);
            endDateInputContainer.setVisibility(View.GONE);
            endTimeInputContainer.setVisibility(View.GONE);

            if (selected.contains("Date")) {
                dateValueInputContainer.setVisibility(View.VISIBLE);
                startTimeInputContainer.setVisibility(View.VISIBLE);
                endDateInputContainer.setVisibility(View.VISIBLE);
                endTimeInputContainer.setVisibility(View.VISIBLE);

                dateValueInput.setFocusable(false);
                dateValueInput.setCursorVisible(false);
                dateValueInput.setClickable(true);
                dateValueInput.setOnClickListener(v -> showDatePicker(dateValueInput));

                startTimeInput.setFocusable(false);
                startTimeInput.setCursorVisible(false);
                startTimeInput.setClickable(true);
                startTimeInput.setOnClickListener(v -> showTimePicker(startTimeInput));

                endDateInput.setFocusable(false);
                endDateInput.setCursorVisible(false);
                endDateInput.setClickable(true);
                endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));

                endTimeInput.setFocusable(false);
                endTimeInput.setCursorVisible(false);
                endTimeInput.setClickable(true);
                endTimeInput.setOnClickListener(v -> showTimePicker(endTimeInput));

                dateValueInput.setText("");
                startTimeInput.setText("");
                endDateInput.setText("");
                endTimeInput.setText("");
            } else {
                // Show regular text input
                regularValueInputContainer.setVisibility(View.VISIBLE);

                if (selected.equals("Reporter Phone")) {
                    regularValueInputContainer.setHint("Enter phone number");
                    regularValueInput.setInputType(InputType.TYPE_CLASS_PHONE);
                } else if (selected.equals("Reporter Email")) {
                    regularValueInputContainer.setHint("Enter email");
                    regularValueInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                } else {
                    regularValueInputContainer.setHint("Enter " + selected.toLowerCase());
                    regularValueInput.setInputType(InputType.TYPE_CLASS_TEXT);
                }

                regularValueInput.setText("");
            }
        });

        // Show dropdown when clicked
        fieldSpinner.setOnClickListener(v -> fieldSpinner.showDropDown());

        if (currentFilterField != null) {
            String displayField = "";
            switch (currentFilterField) {
                case "type": displayField = "Report Type"; break;
                case "description": displayField = "Description"; break;
                case "locationText": displayField = "Location"; break;
                case "userName": displayField = "Reporter Name"; break;
                case "userEmail": displayField = "Reporter Email"; break;
                case "userPhone": displayField = "Reporter Phone"; break;
                case "timestamp": displayField = "Reported Date"; break;
                case "resolvedTimestamp": displayField = "Resolved Date"; break;
            }

            fieldSpinner.setText(displayField, false);
            fieldInputContainer.setHint("Filter by " + displayField.toLowerCase());

            if (displayField.contains("Date")) {
                // Show date fields and set values
                dateValueInputContainer.setVisibility(View.VISIBLE);
                startTimeInputContainer.setVisibility(View.VISIBLE);
                endDateInputContainer.setVisibility(View.VISIBLE);
                endTimeInputContainer.setVisibility(View.VISIBLE);

                dateValueInput.setFocusable(false);
                dateValueInput.setCursorVisible(false);
                dateValueInput.setClickable(true);
                dateValueInput.setOnClickListener(v -> showDatePicker(dateValueInput));

                startTimeInput.setFocusable(false);
                startTimeInput.setCursorVisible(false);
                startTimeInput.setClickable(true);
                startTimeInput.setOnClickListener(v -> showTimePicker(startTimeInput));

                endDateInput.setFocusable(false);
                endDateInput.setCursorVisible(false);
                endDateInput.setClickable(true);
                endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));

                endTimeInput.setFocusable(false);
                endTimeInput.setCursorVisible(false);
                endTimeInput.setClickable(true);
                endTimeInput.setOnClickListener(v -> showTimePicker(endTimeInput));

                dateValueInput.setText(currentFilterValue);
                if (currentFilterStartTime != null) {
                    startTimeInput.setText(currentFilterStartTime);
                }
                if (currentFilterEndValue != null) {
                    endDateInput.setText(currentFilterEndValue);
                }
                if (currentFilterEndTime != null) {
                    endTimeInput.setText(currentFilterEndTime);
                }
            } else {
                regularValueInputContainer.setVisibility(View.VISIBLE);
                regularValueInput.setText(currentFilterValue);
            }
        }

        clearButton.setOnClickListener(v -> {
            currentFilterField = null;
            currentFilterValue = null;
            currentFilterEndValue = null;
            currentFilterStartTime = null;
            currentFilterEndTime = null;
            currentFilterLabel = null;
            dialog.dismiss();
            setupRecyclerView();
            Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show();
        });

        applyButton.setOnClickListener(v -> {
            String selectedField = fieldSpinner.getText().toString();
            String value;
            String endValue = null;
            String startTime = null;
            String endTime = null;

            if (selectedField.contains("Date")) {
                value = dateValueInput.getText().toString().trim();
                startTime = startTimeInput.getText().toString().trim();
                endValue = endDateInput.getText().toString().trim();
                endTime = endTimeInput.getText().toString().trim();
            } else {
                value = regularValueInput.getText().toString().trim();
            }

            if (selectedField.equals("Select field") || value.isEmpty()) {
                Snackbar.make(view, "Please select both field and value", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (selectedField.equals("Reporter Phone") && !isValidPhoneNumber(value)) {
                Snackbar.make(view, "Please enter a valid phone number", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> fieldMap = new HashMap<>();
            fieldMap.put("Report Type", "type");
            fieldMap.put("Description", "description");
            fieldMap.put("Location", "locationText");
            fieldMap.put("Reporter Name", "userName");
            fieldMap.put("Reporter Email", "userEmail");
            fieldMap.put("Reporter Phone", "userPhone");
            fieldMap.put("Reported Date", "timestamp");
            fieldMap.put("Resolved Date", "resolvedTimestamp");
            fieldMap.put("Resolved By", "resolvedBy");

            currentFilterField = fieldMap.get(selectedField);
            currentFilterValue = value;
            currentFilterEndValue = TextUtils.isEmpty(endValue) ? null : endValue;
            currentFilterStartTime = TextUtils.isEmpty(startTime) ? null : startTime;
            currentFilterEndTime = TextUtils.isEmpty(endTime) ? null : endTime;

            if (selectedField.contains("Date")) {
                currentFilterLabel = selectedField + ": " + value;
                if (currentFilterStartTime != null) {
                    currentFilterLabel += " " + currentFilterStartTime;
                }
                if (currentFilterEndValue != null) {
                    currentFilterLabel += " to " + currentFilterEndValue;
                    if (currentFilterEndTime != null) {
                        currentFilterLabel += " " + currentFilterEndTime;
                    }
                } else if (currentFilterEndTime != null) {
                    currentFilterLabel += " to " + timePickerFormat.format(new Date()) + " " + currentFilterEndTime;
                }
            } else {
                currentFilterLabel = selectedField + ": " + value;
            }

            dialog.dismiss();
            setupRecyclerView();
            Snackbar.make(reportsRecyclerView, "Filter applied: " + currentFilterLabel, Snackbar.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private boolean isValidPhoneNumber(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return false;
        }
        String cleaned = phone.replaceAll("[^+0-9]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    private void showDatePicker(TextInputEditText input) {
        Calendar calendar = Calendar.getInstance();

        if (!TextUtils.isEmpty(input.getText())) {
            try {
                Date date = datePickerFormat.parse(input.getText().toString());
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing existing date", e);
            }
        }

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    // Format date as MM/DD/YYYY
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", month+1, day, year);
                    input.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.show();
    }

    private void showTimePicker(TextInputEditText input) {
        Calendar calendar = Calendar.getInstance();

        if (!TextUtils.isEmpty(input.getText())) {
            try {
                Date time = timePickerFormat.parse(input.getText().toString());
                if (time != null) {
                    calendar.setTime(time);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing existing time", e);
            }
        }

        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    // Format time as HH:MM AM/PM
                    String time = String.format(Locale.getDefault(), "%02d:%02d %s",
                            hourOfDay > 12 ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay),
                            minute,
                            hourOfDay >= 12 ? "PM" : "AM");
                    input.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePicker.setTitle("Select Time");
        timePicker.show();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                if (adapter != null) {
                    adapter.setSearchQuery(currentSearchQuery);
                    checkAndUpdateUIState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @SuppressLint("SetTextI18n")
    private void checkAndUpdateUIState() {
        if (adapter != null) {
            final String filterField = currentFilterField;
            final String searchQuery = currentSearchQuery;

            if (adapter.getItemCount() == 0) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (filterField != null || !TextUtils.isEmpty(searchQuery)) {
                        if (filterField != null && (filterField.equals("timestamp") || filterField.equals("resolvedTimestamp"))) {
                            noReportsText.setText("No reports found for the selected date range");
                        } else {
                            noReportsText.setText("No matching reports found");
                        }
                    } else {
                        noReportsText.setText("No resolved reports");
                    }
                    noReportsText.setVisibility(View.VISIBLE);
                    reportsRecyclerView.setVisibility(View.GONE);
                });
                return;
            }

            boolean hasVisibleItems = false;
            for (int i = 0; i < adapter.getItemCount(); i++) {
                try {
                    Incident item = adapter.getItem(i);
                    if (adapter.shouldShowItem(item)) {
                        hasVisibleItems = true;
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking item visibility", e);
                }
            }

            final boolean finalHasVisibleItems = hasVisibleItems;
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (!finalHasVisibleItems) {
                    if (filterField != null || !TextUtils.isEmpty(searchQuery)) {
                        if (filterField != null && (filterField.equals("timestamp") || filterField.equals("resolvedTimestamp"))) {
                            noReportsText.setText("No reports found for the selected date range");
                        } else {
                            noReportsText.setText("No matching reports found");
                        }
                    } else {
                        noReportsText.setText("No resolved reports");
                    }
                    noReportsText.setVisibility(View.VISIBLE);
                    reportsRecyclerView.setVisibility(View.GONE);
                } else {
                    noReportsText.setVisibility(View.GONE);
                    reportsRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void setupRecyclerView() {
        showLoadingState();

        Query query = fStore.collection("resolved_incidents");

        if (currentFilterField != null && currentFilterValue != null && !currentFilterValue.isEmpty()) {
            switch (currentFilterField) {
                case "type":
                case "description":
                case "locationText":
                case "userName":
                case "userEmail":
                    query = query.whereGreaterThanOrEqualTo(currentFilterField, currentFilterValue)
                            .whereLessThanOrEqualTo(currentFilterField, currentFilterValue + "\uf8ff");
                    break;
                case "userPhone":
                    query = query.whereEqualTo(currentFilterField, currentFilterValue);
                    break;
                case "timestamp":
                case "resolvedTimestamp":
                    try {
                        // Parse start date and time
                        Date startDate = datePickerFormat.parse(currentFilterValue);
                        if (startDate == null) {
                            throw new ParseException("Failed to parse start date", 0);
                        }

                        Calendar startCal = Calendar.getInstance();
                        startCal.setTime(startDate);

                        // Apply start time if provided
                        if (!TextUtils.isEmpty(currentFilterStartTime)) {
                            try {
                                Date startTime = timePickerFormat.parse(currentFilterStartTime);
                                if (startTime != null) {
                                    Calendar timeCal = Calendar.getInstance();
                                    timeCal.setTime(startTime);
                                    startCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                                    startCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing start time", e);
                                // Fall back to beginning of day
                                startCal.set(Calendar.HOUR_OF_DAY, 0);
                                startCal.set(Calendar.MINUTE, 0);
                            }
                        } else {
                            // Default to beginning of day
                            startCal.set(Calendar.HOUR_OF_DAY, 0);
                            startCal.set(Calendar.MINUTE, 0);
                        }

                        startCal.set(Calendar.SECOND, 0);
                        startCal.set(Calendar.MILLISECOND, 0);
                        long startMillis = startCal.getTimeInMillis();

                        Log.d(TAG, "Start date/time: " + new Date(startMillis));
                        query = query.whereGreaterThanOrEqualTo(currentFilterField, startMillis);

                        Calendar endCal = Calendar.getInstance();
                        if (!TextUtils.isEmpty(currentFilterEndValue)) {
                            try {
                                Date endDate = datePickerFormat.parse(currentFilterEndValue);
                                if (endDate != null) {
                                    endCal.setTime(endDate);
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing end date", e);
                                endCal.setTime(startDate); // Fallback to start date
                            }
                        } else {
                            endCal.setTime(startDate); // Default to start date
                        }

                        // Apply end time if provided
                        if (!TextUtils.isEmpty(currentFilterEndTime)) {
                            try {
                                Date endTime = timePickerFormat.parse(currentFilterEndTime);
                                if (endTime != null) {
                                    Calendar timeCal = Calendar.getInstance();
                                    timeCal.setTime(endTime);
                                    endCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                                    endCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing end time", e);
                                endCal.set(Calendar.HOUR_OF_DAY, 23);
                                endCal.set(Calendar.MINUTE, 59);
                            }
                        } else {
                            endCal.set(Calendar.HOUR_OF_DAY, 23);
                            endCal.set(Calendar.MINUTE, 59);
                        }

                        endCal.set(Calendar.SECOND, 59);
                        endCal.set(Calendar.MILLISECOND, 999);
                        long endMillis = endCal.getTimeInMillis();

                        Log.d(TAG, "End date/time: " + new Date(endMillis));
                        query = query.whereLessThanOrEqualTo(currentFilterField, endMillis);

                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date/time filter", e);
                        Toast.makeText(this, "Invalid date/time format", Toast.LENGTH_SHORT).show();
                        return; // Exit early if parsing fails
                    }
                    break;
                case "resolvedBy":
                    query = query.whereGreaterThanOrEqualTo("resolvedBy", currentFilterValue)
                            .whereLessThanOrEqualTo("resolvedBy", currentFilterValue + "\uf8ff");
                    break;
            }
        }

        final String filterField = currentFilterField;
        final String searchQuery = currentSearchQuery;

        // First check if the query returns any documents
        query.limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && querySnapshot.isEmpty()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (filterField != null) {
                            if (filterField.equals("timestamp") || filterField.equals("resolvedTimestamp")) {
                                noReportsText.setText("No reports found for the selected date range");
                            } else {
                                noReportsText.setText("No matching reports found");
                            }
                        } else if (!TextUtils.isEmpty(searchQuery)) {
                            noReportsText.setText("No matching reports found");
                        } else {
                            noReportsText.setText("No resolved reports");
                        }
                        noReportsText.setVisibility(View.VISIBLE);
                        reportsRecyclerView.setVisibility(View.GONE);
                    });
                } else {
                    // Documents exist, proceed with setting up the adapter
                    setupAdapterWithQuery();
                }
            } else {
                Log.e(TAG, "Error checking collection", task.getException());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    noReportsText.setText("Error loading reports");
                    noReportsText.setVisibility(View.VISIBLE);
                    reportsRecyclerView.setVisibility(View.GONE);
                });
            }
        });
    }

    private void setupAdapterWithQuery() {
        Query query = fStore.collection("resolved_incidents");

        // Apply field filters
        if (currentFilterField != null && currentFilterValue != null && !currentFilterValue.isEmpty()) {
            switch (currentFilterField) {
                case "type":
                case "description":
                case "locationText":
                case "userName":
                case "userEmail":
                    query = query.whereGreaterThanOrEqualTo(currentFilterField, currentFilterValue)
                            .whereLessThanOrEqualTo(currentFilterField, currentFilterValue + "\uf8ff");
                    break;
                case "userPhone":
                    query = query.whereEqualTo(currentFilterField, currentFilterValue);
                    break;
                case "timestamp":
                case "resolvedTimestamp":
                    try {
                        // Parse start date and time
                        Date startDate = datePickerFormat.parse(currentFilterValue);
                        if (startDate == null) {
                            throw new ParseException("Failed to parse start date", 0);
                        }

                        Calendar startCal = Calendar.getInstance();
                        startCal.setTime(startDate);

                        // Apply start time if provided
                        if (!TextUtils.isEmpty(currentFilterStartTime)) {
                            try {
                                Date startTime = timePickerFormat.parse(currentFilterStartTime);
                                if (startTime != null) {
                                    Calendar timeCal = Calendar.getInstance();
                                    timeCal.setTime(startTime);
                                    startCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                                    startCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing start time", e);
                                // Fall back to beginning of day
                                startCal.set(Calendar.HOUR_OF_DAY, 0);
                                startCal.set(Calendar.MINUTE, 0);
                            }
                        } else {
                            // Default to beginning of day
                            startCal.set(Calendar.HOUR_OF_DAY, 0);
                            startCal.set(Calendar.MINUTE, 0);
                        }

                        startCal.set(Calendar.SECOND, 0);
                        startCal.set(Calendar.MILLISECOND, 0);
                        long startMillis = startCal.getTimeInMillis();

                        Log.d(TAG, "Start date/time: " + new Date(startMillis));
                        query = query.whereGreaterThanOrEqualTo(currentFilterField, startMillis);

                        // Parse end date and time if provided
                        if (!TextUtils.isEmpty(currentFilterEndValue)) {
                            Date endDate;
                            try {
                                endDate = datePickerFormat.parse(currentFilterEndValue);
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing end date, using start date instead", e);
                                endDate = startDate; // Fallback to start date
                            }

                            if (endDate != null) {
                                Calendar endCal = Calendar.getInstance();
                                endCal.setTime(endDate);

                                // Apply end time if provided
                                if (!TextUtils.isEmpty(currentFilterEndTime)) {
                                    try {
                                        Date endTime = timePickerFormat.parse(currentFilterEndTime);
                                        if (endTime != null) {
                                            Calendar timeCal = Calendar.getInstance();
                                            timeCal.setTime(endTime);
                                            endCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                                            endCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                                            Log.d(TAG, "Applied end time: " + endTime);
                                        }
                                    } catch (ParseException e) {
                                        Log.e(TAG, "Error parsing end time", e);
                                        // Fall back to end of day
                                        endCal.set(Calendar.HOUR_OF_DAY, 23);
                                        endCal.set(Calendar.MINUTE, 59);
                                    }
                                } else {
                                    // Default to end of day
                                    endCal.set(Calendar.HOUR_OF_DAY, 23);
                                    endCal.set(Calendar.MINUTE, 59);
                                }

                                endCal.set(Calendar.SECOND, 59);
                                endCal.set(Calendar.MILLISECOND, 999);
                                long endMillis = endCal.getTimeInMillis();

                                Log.d(TAG, "End date/time: " + new Date(endMillis));
                                query = query.whereLessThanOrEqualTo(currentFilterField, endMillis);
                            }
                        } else if (!TextUtils.isEmpty(currentFilterEndTime)) {
                            // If only end time is provided without end date, use start date with end time
                            Calendar endCal = Calendar.getInstance();
                            endCal.setTime(startDate);

                            try {
                                Date endTime = timePickerFormat.parse(currentFilterEndTime);
                                if (endTime != null) {
                                    Calendar timeCal = Calendar.getInstance();
                                    timeCal.setTime(endTime);
                                    endCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                                    endCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing end time", e);
                                endCal.set(Calendar.HOUR_OF_DAY, 23);
                                endCal.set(Calendar.MINUTE, 59);
                            }

                            endCal.set(Calendar.SECOND, 59);
                            endCal.set(Calendar.MILLISECOND, 999);
                            long endMillis = endCal.getTimeInMillis();

                            Log.d(TAG, "End time only: " + new Date(endMillis));
                            query = query.whereLessThanOrEqualTo(currentFilterField, endMillis);
                        } else {
                            // If no end date specified, use the end of the start date
                            Calendar endCal = Calendar.getInstance();
                            endCal.setTime(startDate);
                            endCal.set(Calendar.HOUR_OF_DAY, 23);
                            endCal.set(Calendar.MINUTE, 59);
                            endCal.set(Calendar.SECOND, 59);
                            endCal.set(Calendar.MILLISECOND, 999);
                            long endMillis = endCal.getTimeInMillis();

                            Log.d(TAG, "Default end date/time: " + new Date(endMillis));
                            query = query.whereLessThanOrEqualTo(currentFilterField, endMillis);
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date/time filter", e);
                        Toast.makeText(this, "Invalid date/time format", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }

        query = query.orderBy("resolvedTimestamp", Query.Direction.DESCENDING).limit(50);

        FirestoreRecyclerOptions<Incident> options = new FirestoreRecyclerOptions.Builder<Incident>()
                .setQuery(query, snapshot -> {
                    Incident incident = snapshot.toObject(Incident.class);
                    if (incident != null) {
                        incident.setDocumentIdFromSnapshot(snapshot);
                    }
                    return incident;
                })
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new ResolvedReportAdapter(options);
        adapter.setOnDataChangedListener(() -> {
            Log.d(TAG, "Data changed, item count: " + adapter.getItemCount());
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                checkAndUpdateUIState();
            });
        });

        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(adapter);

        adapter.startListening();
    }

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        reportsRecyclerView.setVisibility(View.GONE);
        noReportsText.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}