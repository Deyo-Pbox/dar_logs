package com.example.darlogs

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class AddRecordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.initialize()
        setContentView(R.layout.activity_add_record)

        val municipalitySpinner = findViewById<Spinner>(R.id.spinnerMunicipality)
        val statusSpinner = findViewById<Spinner>(R.id.spinnerStatus)
        val submitButton = findViewById<Button>(R.id.buttonSubmit)

        municipalitySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Bombon", "Calabanga", "Canaman", "Caramoan", "Garchitorena", "Goa", "Lagonoy", "Magarao", "Naga", "Ocampo", "Pili", "Presentacion", "Sagnay", "San Jose", "Siruma", "Tigaon", "Tinambac")
        )

        statusSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Pending", "Finished")
        )

        submitButton.setOnClickListener {
            submitRecord(
                municipalitySpinner.selectedItem.toString(),
                findViewById<EditText>(R.id.inputClaimant).text.toString(),
                findViewById<EditText>(R.id.inputTitleNo).text.toString(),
                findViewById<EditText>(R.id.inputOdtsNo).text.toString(),
                findViewById<EditText>(R.id.inputLotNo).text.toString(),
                findViewById<EditText>(R.id.inputSurveyNo).text.toString(),
                findViewById<EditText>(R.id.inputAreaHas).text.toString(),
                findViewById<EditText>(R.id.inputLocation).text.toString(),
                findViewById<EditText>(R.id.inputTransmittedDocuments).text.toString(),
                findViewById<EditText>(R.id.inputRouteTo).text.toString(),
                findViewById<EditText>(R.id.inputReceivedByControlNo).text.toString(),
                findViewById<EditText>(R.id.inputRemarks).text.toString(),
                statusSpinner.selectedItem.toString().lowercase()
            )
        }
    }

    private fun submitRecord(
        municipality: String,
        claimant: String,
        titleNo: String,
        odtsNo: String,
        lotNo: String,
        surveyNo: String,
        areaHas: String,
        location: String,
        transmittedDocuments: String,
        routeTo: String,
        receivedByControlNo: String,
        remarks: String,
        workStatus: String
    ) {
        if (claimant.isBlank() || titleNo.isBlank()) {
            Toast.makeText(this, "Claimant and Title are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = JSONObject().apply {
            put("municipality", municipality)
            put("lo_claimant", claimant)
            put("title_no", titleNo)
            put("odts_no", odtsNo)
            put("lot_no", lotNo)
            put("survey_no", surveyNo)
            put("area_has", areaHas)
            put("location", location)
            put("transmitted_documents", transmittedDocuments)
            put("route_to", routeTo)
            put("received_by_control_no", receivedByControlNo)
            put("remarks_action_taken", remarks)
            put("work_status", workStatus)
        }.toString()

        Thread {
            val response = ApiClient.postJson(getString(R.string.records_api_url), requestBody)
            runOnUiThread {
                if (!response.success || response.json == null) {
                    Toast.makeText(this, "Unable to submit record. Try again.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                if (response.json.optBoolean("success", false)) {
                    Toast.makeText(this, "Record added successfully.", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val message = response.json.optString("message", "Unable to submit record.")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
