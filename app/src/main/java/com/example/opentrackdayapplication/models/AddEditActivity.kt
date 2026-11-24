package com.example.opentrackdayapplication.models

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.opentrackdayapplication.R
import timber.log.Timber
import android.net.Uri
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


// Activity used to create a new placemark or edit an existing one.
class AddEditActivity : AppCompatActivity() {

    // Input field for the placemark title.
    private lateinit var titleInput: EditText

    // Input field for the placemark description.
    private lateinit var descriptionInput: EditText

    // Input field for the X-coordinate.
    private lateinit var xInput: EditText

    // Input field for the Y-coordinate.
    private lateinit var yInput: EditText

    // Dropdown input used to select the placemark category.
    private lateinit var categoryInput: AutoCompleteTextView

    private lateinit var imagePreview: ImageView
    private var selectedImageUri: Uri? = null

    // Stores the ID of the placemark being edited.
    // Remains null when creating a new placemark.
    private var editingId: Long? = null

    // Called when the activity is first created.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Loads the layout for the add/edit screen.
        setContentView(R.layout.activity_add_edit)

        // Enables edge-to-edge display so the app can draw behind system bars.
        enableEdgeToEdge()
        // Applies padding to the main view to account for system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            // Gets the sizes of the system bars, such as the status bar and navigation bar.
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Adds padding so the content does not overlap the system bars.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            // Returns the insets so they can continue through the view system.
            insets
        }

        // Finds the title input field in the layout.
        titleInput =
            findViewById(R.id.titleInput)

        // Finds the description input field in the layout.
        descriptionInput =
            findViewById(R.id.descriptionInput)

        // Finds the X-coordinate input field.
        xInput =
            findViewById(R.id.xInput)

        // Finds the Y-coordinate input field.
        yInput =
            findViewById(R.id.yInput)

        // Retrieves the available placemark categories from the string resources.
        val category = resources.getStringArray(R.array.categoryArray)

        // Finds the category dropdown input field.
        categoryInput =
            findViewById(R.id.autoCompleteTextView)


        // Creates an adapter to supply the category options to the dropdown.
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            category
        )

        // Connects the category options to the dropdown input.
        categoryInput.setAdapter(arrayAdapter)

        // Opens the category dropdown when the user clicks the input field.
        categoryInput.setOnClickListener {
            categoryInput.showDropDown()
        }

        // Finds the button used to save a new or edited placemark.
        val saveButton =
            findViewById<Button>(R.id.saveButton)

        // Finds the button used to cancel the operation.
        val cancelButton =
            findViewById<Button>(R.id.cancelButton)

        // Retrieves the placemark ID passed from the previous activity.
        // If no valid ID is supplied, editingId remains null.
        editingId =
            intent.getLongExtra("id", -1L)
                .takeIf { it != -1L }

        // Checks whether an existing placemark is being edited.
        if (editingId != null) {

            // Changes the form heading to indicate edit mode.
            findViewById<TextView>(R.id.formTitle)
                .text = "Edit Mark"

            // Loads the existing placemark details into the input fields.
            loadExistingMark(editingId!!)
        }

        // Saves the placemark when the save button is clicked.
        saveButton.setOnClickListener {
            saveMark()
        }

        // Closes the activity when the cancel button is clicked.
        cancelButton.setOnClickListener {
            finish()
        }

        var selectedLatitude = 0.0
        var selectedLongitude = 0.0

        val mapPicker =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data ?: return@registerForActivityResult
                    selectedLatitude = data.getDoubleExtra("latitude", 0.0)
                    selectedLongitude = data.getDoubleExtra("longitude", 0.0)

                    selectedLatitude?.let { xInput.setText(it.toString()) }

                    selectedLongitude?.let { yInput.setText(it.toString()) }
                }
            }

        findViewById<Button>(R.id.mapButton).setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPicker.launch(intent)
        }


        imagePreview = findViewById(R.id.imageView)
        val imagePicker =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri != null) {
                    // Keep access to the document URI for later use, when supported.
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // Some providers may not grant persistable access.
                    }

                    selectedImageUri = uri
                    imagePreview.setImageURI(uri)
                }
            }
        findViewById<Button>(R.id.Imagebutton).setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }
    }

    // Loads an existing placemark using its ID.
    private fun loadExistingMark(id: Long) {

        // Searches the placemark store for the matching ID.
        val mark =
            AppData.placedMarks.findOne(id)

        // Logs the result of the placemark lookup for debugging.
        Timber.d("found mark id=${mark}}")

        // Checks whether the requested placemark exists.
        if (mark == null) {

            // Displays a message if the placemark cannot be found.
            Toast.makeText(
                this,
                "Mark not found",
                Toast.LENGTH_SHORT
            ).show()

            // Closes the activity because there is no placemark to edit.
            finish()
            return
        }

        // Fills the title field with the existing title.
        titleInput.setText(mark.title)

        // Fills the description field with the existing description.
        descriptionInput.setText(mark.desc)

        // Converts the X-coordinate to a string and displays it.
        xInput.setText(mark.x.toString())

        // Converts the Y-coordinate to a string and displays it.
        yInput.setText(mark.y.toString())

        // Displays the existing category in the dropdown input.
        categoryInput.setText(mark.category)


    }

    // Validates the input and creates or updates a placemark.
    private fun saveMark() {

        // Retrieves the title and removes leading and trailing whitespace.
        val title =
            titleInput.text.toString().trim()

        // Retrieves the description and removes leading and trailing whitespace.
        val description =
            descriptionInput.text.toString().trim()

        // Checks that the title is not empty.
        if (title.isEmpty()) {
            // Displays a validation error if no title was entered.
            titleInput.error = "Title is required"
            return
        }

        // Converts the X-coordinate input to a number.
        // Returns null if the input is not a valid number.
        val x =
            xInput.text.toString().toDoubleOrNull()

        // Checks whether the X-coordinate is valid.
        if (x == null) {
            // Displays an error if the X-coordinate is invalid.
            xInput.error = "Enter a valid number"
            return
        }

        // Converts the Y-coordinate input to a number.
        // Returns null if the input is not a valid number.
        val y =
            yInput.text.toString().toDoubleOrNull()

        // Checks whether the Y-coordinate is valid.
        if (y == null) {
            // Displays an error if the Y-coordinate is invalid.
            yInput.error = "Enter a valid number"
            return
        }

        // Retrieves the selected category and removes surrounding whitespace.
        val category = categoryInput.text.toString().trim()

        // Checks that a category has been entered or selected.
        if (category.isEmpty()) {
            // Displays an error if the category is empty.
            categoryInput.error = "Please select a category"
            return
        }


        // Checks whether the user is creating a new placemark.
        if (editingId == null) {

            // Creates a new placemark using the values entered by the user.
            try {
                val mark = PlacemarkModel(
                    title = titleInput.text.toString().trim(),
                    desc = descriptionInput.text.toString(),
                    x = xInput.text.toString().toDouble(),
                    y = yInput.text.toString().toDouble(),
                    category = categoryInput.toString().trim(),
                    imageUri = selectedImageUri?.toString()
                )

                // Logs information about the new placemark.
                Timber.d("Creating mark id=${mark.id}, title=${mark.title}")
                mark.validate()
                // Adds the new placemark to the application's data store.
                AppData.placedMarks.create(mark)

                // Displays a confirmation message after creating the placemark.
                Toast.makeText(
                    this,
                    "Mark created",
                    Toast.LENGTH_SHORT
                ).show()

                // Continue with your existing success/navigation behavior.
            } catch (e: IllegalArgumentException) {
                android.widget.Toast.makeText(
                    this,
                    e.message ?: "Please check the entered values.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

        } else {

            // Creates a placemark object containing the updated details.
            // The existing ID is retained so the correct placemark is updated.
            val mark = PlacemarkModel(
                id = editingId!!,
                title = title,
                desc = description,
                x = x,
                y = y,
                category = category

            )

            // Updates the existing placemark in the data store.
            AppData.placedMarks.update(mark)

            // Displays a confirmation message after updating the placemark.
            Toast.makeText(
                this,
                "Mark updated",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Closes the activity after the placemark has been saved.
        finish()
    }
}