package de.ubuntix.android.birthdayreminder;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.ListActivity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import de.ubuntix.android.birthdayreminder.database.Database;
import de.ubuntix.android.birthdayreminder.database.Preferences;
import de.ubuntix.android.birthdayreminder.model.Contact;
import de.ubuntix.android.birthdayreminder.model.DateOfBirth;
import de.ubuntix.android.birthdayreminder.service.BirthdayBroadcastReceiver;
import de.ubuntix.android.birthdayreminder.view.adapter.BirthContactAdapter;
import de.ubuntix.android.birthdayreminder.view.adapter.CategoryAdapter;
import de.ubuntix.android.birthdayreminder.view.adapter.MultiListAdapter;
import de.ubuntix.android.birthdayreminder.view.comparator.BirthContactBirthdayComparator;
import de.ubuntix.android.birthdayreminder.view.comparator.BirthContactNameComparator;
import de.ubuntix.android.birthdayreminder.view.helper.BirthContact;
import de.ubuntix.android.birthdayreminder.view.helper.BirthContactHelper;

public class BirthdayReminder extends ListActivity {
	// TODO: call/write message on birthday
	// TODO: hideNotificationPref

	private final DateFormatSymbols dateSymbols = new DateFormatSymbols();

	private Database db;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getListView().setFastScrollEnabled(true);

		this.db = new Database(getContentResolver());

		// debug code
		// Debug.logDatabase(this);

		// start BirthdayBroadcastReceiver if it is activated
		Preferences prefs = Preferences.getInstance(getApplicationContext());
		if (prefs.getActivateService()) {
			BirthdayBroadcastReceiver.restart(getApplicationContext());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		updateView();
	}

	private void updateView() {
		// create new list adapter
		MultiListAdapter listAdapter = new MultiListAdapter();
		List<ListAdapter> adapterList = listAdapter.getListAdapters();

		// load birthday and contact information
		List<Contact> contacts = this.db.getAllContacts();
		List<BirthContact> birthContacts = BirthContactHelper.createBirthContactList(contacts);

		// group all contacts by known and unknown birthday
		SortedSet<BirthContact> knownBirthdays = new TreeSet<BirthContact>(new BirthContactBirthdayComparator());
		SortedSet<BirthContact> unknownBirthdays = new TreeSet<BirthContact>(new BirthContactNameComparator());

		for (BirthContact birthContact : birthContacts) {
			DateOfBirth dateOfBirth = birthContact.getDateOfBirth();

			if (dateOfBirth != null) {
				knownBirthdays.add(birthContact);
			} else {
				unknownBirthdays.add(birthContact);
			}
		}

		Integer currentMonth = null;
		BirthContactAdapter currentBirthContactAdapter = null;
		String[] monthStrs = this.dateSymbols.getMonths();
		for (BirthContact birthContact : knownBirthdays) {
			int month = birthContact.getDateOfBirth().getDate().get(Calendar.MONTH);

			if (currentMonth == null || currentMonth != month) {
				currentMonth = month;

				currentBirthContactAdapter = new BirthContactAdapter(this);
				adapterList.add(new CategoryAdapter(this, monthStrs[currentMonth]));
				adapterList.add(currentBirthContactAdapter);
			}

			currentBirthContactAdapter.add(birthContact);
		}

		adapterList.add(new CategoryAdapter(this, getResources().getString(R.string.unknownBirthdays)));
		adapterList.add(new BirthContactAdapter(this, unknownBirthdays));

		setListAdapter(listAdapter);
	}

	public static boolean insertContact(ContentResolver contactAdder,
										String firstName, String mobileNumber) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.RawContacts.CONTENT_URI)
				.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
				.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
				.build());
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				.withValue(
						ContactsContract.Data.MIMETYPE,
						ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
				.withValue(
						ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
						firstName).build());
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				.withValue(
						ContactsContract.Data.MIMETYPE,
						ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,
						mobileNumber)
				.withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
						ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build());

		try {
			contactAdder.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		BirthContact birthContact = (BirthContact) l.getAdapter().getItem(position);

		Intent editorIntent = new Intent(this, BirthdayEditor.class);
		editorIntent.putExtra(BirthdayEditor.CONTACT_ID, birthContact.getContact().getId());
		startActivity(editorIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.preferences:
			startActivity(new Intent(this, PreferenceWindow.class));
			return true;
			case R.id.butinsert:
				startActivity(new Intent(this, AddContact.class));
				return true;
			case R.id.delinsert:
				startActivity(new Intent(this, DeleteContacts.class));
				return true;
			case R.id.addcont:
				startActivity(new Intent(this, MainActivity.class));
				return true;
		case R.id.quit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
