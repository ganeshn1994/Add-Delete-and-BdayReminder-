package de.ubuntix.android.birthdayreminder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import de.ubuntix.android.birthdayreminder.R;

public class AddContact extends Activity implements OnClickListener {

	public static final String add = "add";


	private EditText edtContactName, edtContactNumber;
	private Button btnAddContact, btnCancel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.addcontact);

		edtContactName = (EditText) findViewById(R.id.edtContactName);
		edtContactNumber = (EditText) findViewById(R.id.edtContactNumber);

		btnAddContact = (Button) findViewById(R.id.btnAddContact);
		btnCancel = (Button) findViewById(R.id.btnCancel);

		btnAddContact.setOnClickListener(this);
		btnCancel.setOnClickListener(this);

	}

	public void onClick(View v) {

		Intent intent = new Intent(this, BirthdayReminder.class);
		if (v.getId() == R.id.btnAddContact) {
			if (edtContactName.getText().toString().equals("")
					&& edtContactNumber.getText().toString().equals("")) {
				Toast.makeText(this, "Please fill both fields...",
						Toast.LENGTH_SHORT).show();
			} else {
				BirthdayReminder.insertContact(getContentResolver(),
						edtContactName.getText().toString(), edtContactNumber
								.getText().toString());
				edtContactName.setText("");
				edtContactNumber.setText("");

				startActivity(intent);
			}

		} else if (v.getId() == R.id.btnCancel) {
			startActivity(intent);
		}

	}
}
